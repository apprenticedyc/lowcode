package com.hex.ailowcode.ai.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Prompt 安全审查护轨：防止恶意 Prompt 注入攻击
 *
 * 作用：
 * - 拦截提示词注入攻击（Prompt Injection）
 * - 防止越狱尝试（Jailbreak）
 * - 检测并拒绝恶意输入
 *
 * 使用场景：用户输入安全审查，防御 AI 对话劫持
 */
public class PromptSafetyInputGuardrail implements InputGuardrail {

    // 敏感词列表：提示词注入和越狱攻击的常见关键词
    private static final List<String> SENSITIVE_WORDS = Arrays.asList("忽略之前的指令", "ignore previous instructions", "ignore above", "破解", "hack", "绕过", "bypass", "越狱", "jailbreak");

    // 注入攻击模式：使用正则表达式匹配复杂的注入攻击模式
    private static final List<Pattern> INJECTION_PATTERNS = Arrays.asList(Pattern.compile("(?i)ignore\\s+(?:previous|above|all)\\s+(?:instructions?|commands?|prompts?)"), Pattern.compile("(?i)(?:forget|disregard)\\s+(?:everything|all)\\s+(?:above|before)"), Pattern.compile("(?i)(?:pretend|act|behave)\\s+(?:as|like)\\s+(?:if|you\\s+are)"), Pattern.compile("(?i)system\\s*:\\s*you\\s+are"), Pattern.compile("(?i)new\\s+(?:instructions?|commands?|prompts?)\\s*:"));

    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        String input = userMessage.singleText();
        // 1. 检查输入长度（防止超长输入攻击）
        if (input.length() > 1000) {
            return fatal("输入内容过长，不要超过 1000 字");
        }
        // 2. 检查是否为空
        if (input.trim().isEmpty()) {
            return fatal("输入内容不能为空");
        }
        // 3. 检查敏感词（提示词注入关键词）
        String lowerInput = input.toLowerCase();
        for (String sensitiveWord : SENSITIVE_WORDS) {
            if (lowerInput.contains(sensitiveWord.toLowerCase())) {
                return fatal("输入包含不当内容，请修改后重试");
            }
        }
        // 4. 检查注入攻击模式（正则表达式匹配）
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                return fatal("检测到恶意输入，请求被拒绝");
            }
        }
        // 5. 验证通过，返回成功
        return success();
    }
}
