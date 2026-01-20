package com.hex.ailowcode.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hex.ailowcode.ai.guardrail.PromptSafetyInputGuardrail;
import com.hex.ailowcode.ai.tools.FileWriteTool;
import com.hex.ailowcode.exception.BusinessException;
import com.hex.ailowcode.exception.ErrorCode;
import com.hex.ailowcode.model.enums.CodeGenTypeEnum;
import com.hex.ailowcode.service.ChatHistoryService;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 初始化AIService将其注册为Bean
 */
@Slf4j
@Configuration
public class AiCodeGeneratorServiceFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    private StreamingChatModel openAiStreamingChatModel;
    @Resource
    private StreamingChatModel reasoningStreamingChatModel;
    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;
    @Resource
    private ChatHistoryService chatHistoryService;

    /**
     * 创建新的 AI 服务实例
     * APP之间隔离 - 每个app一个AppID对应一个服务实例
     *
     * @param appId 应用ID
     * @param codeGenType 代码生成类型
     * @return AI 服务实例
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        // 1. 根据 appId 构建独立的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder().id(appId)
                .chatMemoryStore(redisChatMemoryStore).maxMessages(20).build();
        // 2. 从数据库加载历史对话到记忆中
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);

        // 3. 根据代码生成类型选择不同的模型配置
        return switch (codeGenType) {
            // Vue 项目生成，使用推理模型+工具调用
            case VUE_PROJECT -> AiServices.builder(AiCodeGeneratorService.class).chatModel(chatModel)
                    .streamingChatModel(reasoningStreamingChatModel).chatMemoryProvider(memoryId -> chatMemory)
                    .tools(new FileWriteTool()).inputGuardrails(new PromptSafetyInputGuardrail()) // 添加输入护轨
                    // 处理工具调用幻觉问题
                    .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(toolExecutionRequest, "Error: there is no tool called " + toolExecutionRequest.name()))
                    .build();
            // HTML 和 多文件生成，使用对话模型
            case HTML, MULTI_FILE -> AiServices.builder(AiCodeGeneratorService.class).chatModel(chatModel)
                    .inputGuardrails(new PromptSafetyInputGuardrail()) // 添加输入护轨
                    .streamingChatModel(openAiStreamingChatModel).chatMemory(chatMemory).build();
            default ->
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型: " + codeGenType.getValue());
        };
    }

    /**
     * Caffeine 缓存已创建的AI Service实例
     * 缓存策略：
     * - 最大缓存 1000 个实例
     * - 写入后 30 分钟过期
     * - 访问后 10 分钟过期
     */
    private final Cache<Long, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder().maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30)).expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI 服务实例被移除，appId: {}, 原因: {}", key, cause);
            }).build();

    /**
     * 从缓存中根据 appId 获取对应的Ai-Service实例
     *
     * 采用旁路缓存策略：如果缓存中不存在该 appId 对应的实例，Caffeine 会自动：
     * 1. 调用 createAiCodeGeneratorService(appId, codeGenType) 创建新实例
     * 2. 将创建的实例自动存入缓存并返回
     *
     * @param appId 应用ID
     * @param codeGenType 代码生成类型
     * @return AI 服务实例
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        return serviceCache.get(appId, id -> createAiCodeGeneratorService(id, codeGenType));
    }
}
