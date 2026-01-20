package com.hex.ailowcode.ai.guardrail;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;

/**
 * 重试输出护轨：验证 AI 输出质量，不合格则重新生成
 *
 * 作用：
 * - 拦截空响应或过短响应
 * - 过滤含敏感信息的响应
 * - 触发 LLM 重新生成（reprompt）
 *
 * 使用场景：AI 代码生成输出质量控制
 */
public class RetryOutputGuardrail implements OutputGuardrail {

    @Override
    public OutputGuardrailResult validate(AiMessage responseFromLLM) {
        String response = responseFromLLM.text();
        // 1. 检查响应是否为空
        if (response == null || response.trim().isEmpty()) {
            return reprompt("响应内容为空", "请重新生成完整的内容");
        }
        // 2. 检查响应是否过短（小于 10 字符）
        if (response.trim().length() < 10) {
            return reprompt("响应内容过短", "请提供更详细的内容");
        }
        // 3. 检查是否包含敏感信息（密码、token、证书等）
        if (containsSensitiveContent(response)) {
            return reprompt("包含敏感信息", "请重新生成内容，避免包含敏感信息");
        }
        // 4. 验证通过，返回成功
        return success();
    }

    /**
     * 检查是否包含敏感内容
     *
     * 敏感词：密码、password、secret、token、私钥、证书等
     */
    private boolean containsSensitiveContent(String response) {
        String lowerResponse = response.toLowerCase();
        String[] sensitiveWords = {
                "密码", "password", "secret", "token",
                "api key", "私钥", "证书", "credential"
        };
        for (String word : sensitiveWords) {
            if (lowerResponse.contains(word)) {
                return true;
            }
        }
        return false;
    }
}