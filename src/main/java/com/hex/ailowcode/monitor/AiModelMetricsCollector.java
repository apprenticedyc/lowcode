package com.hex.ailowcode.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * AI模型指标收集器
 *
 * 基于 Micrometer 框架收集 AI 模型调用的监控指标，包括：
 * - 请求次数：记录成功和失败的总请求数
 * - 错误次数：记录各类错误发生的次数
 * - Token消耗：记录输入和输出Token的使用量
 * - 响应时间：记录模型调用的耗时
 *
 * 使用 ConcurrentHashMap 缓存已创建的指标对象，避免重复注册相同维度的指标
 */
@Component
@Slf4j
public class AiModelMetricsCollector {

    @Resource
    private MeterRegistry meterRegistry;

    // 使用ConcurrentHashMap缓存已创建的指标，避免重复创建
    private final ConcurrentMap<String, Counter> requestCountersCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> errorCountersCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> tokenCountersCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Timer> responseTimersCache = new ConcurrentHashMap<>();

    /**
     * 记录 AI 模型请求次数
     *
     * 使用 Counter 类型的指标记录请求，并通过标签实现多维度统计：
     * - user_id: 按用户维度统计
     * - app_id: 按应用维度统计
     * - model_name: 按模型名称统计
     * - status: 按请求状态统计（如 success、failed）
     *
     */
    public void recordRequest(String userId, String appId, String modelName, String status) {
        String key = String.format("%s_%s_%s_%s", userId, appId, modelName, status);  // 缓存key：拼接所有维度
        // computeIfAbsent：key不存在才执行创建
        Counter counter = requestCountersCache.computeIfAbsent(key, k ->
                Counter.builder("ai_model_requests_total")  // 指标名称：Prometheus格式，使用下划线命名
                        .description("AI模型总请求次数")      // 指标描述
                        .tag("user_id", userId)              // 标签：按用户维度统计
                        .tag("app_id", appId)                // 标签：按应用维度统计
                        .tag("model_name", modelName)        // 标签：按模型名称统计
                        .tag("status", status)               // 标签：按请求状态统计（success/failed）
                        .register(meterRegistry)             // 注册到Micrometer注册表
        );
    }

    /**
     * 记录 AI 模型错误次数
     *
     * 使用 Counter 类型的指标记录错误，并通过标签实现多维度统计：
     * - user_id: 按用户维度统计
     * - app_id: 按应用维度统计
     * - model_name: 按模型名称统计
     * - error_message: 按错误类型统计（如 timeout、rate_limit、api_error）
     */
    public void recordError(String userId, String appId, String modelName, String errorMessage) {
        String key = String.format("%s_%s_%s_%s", userId, appId, modelName, errorMessage);  // 缓存key：拼接所有维度
        Counter counter = errorCountersCache.computeIfAbsent(key, k ->
                Counter.builder("ai_model_errors_total")     // 指标名称
                        .description("AI模型错误次数")         // 指标描述
                        .tag("user_id", userId)               // 标签：按用户维度统计
                        .tag("app_id", appId)                 // 标签：按应用维度统计
                        .tag("model_name", modelName)         // 标签：按模型名称统计
                        .tag("error_message", errorMessage)   // 标签：按错误类型统计
                        .register(meterRegistry)              // 注册到Micrometer注册表
        );
        counter.increment();  // 计数器+1
    }

    /**
     * 记录 AI 模型 Token 消耗量
     *
     * 使用 Counter 类型的指标记录 Token 使用量，并通过标签实现多维度统计：
     * - user_id: 按用户维度统计
     * - app_id: 按应用维度统计
     * - model_name: 按模型名称统计
     * - token_type: 按 Token 类型统计（input 输入Token / output 输出Token）
     */
    public void recordTokenUsage(String userId, String appId, String modelName,
                                 String tokenType, long tokenCount) {
        String key = String.format("%s_%s_%s_%s", userId, appId, modelName, tokenType);  // 缓存key：拼接所有维度
        Counter counter = tokenCountersCache.computeIfAbsent(key, k ->
                Counter.builder("ai_model_tokens_total")      // 指标名称
                        .description("AI模型Token消耗总数")     // 指标描述
                        .tag("user_id", userId)                // 标签：按用户维度统计
                        .tag("app_id", appId)                  // 标签：按应用维度统计
                        .tag("model_name", modelName)          // 标签：按模型名称统计
                        .tag("token_type", tokenType)          // 标签：按Token类型统计（input/output）
                        .register(meterRegistry)               // 注册到Micrometer注册表
        );
        counter.increment(tokenCount);  // 计数器增加指定数量
    }

    /**
     * 记录 AI 模型响应时间
     *
     * 使用 Timer 类型的指标记录响应耗时，并通过标签实现多维度统计：
     * - user_id: 按用户维度统计
     * - app_id: 按应用维度统计
     * - model_name: 按模型名称统计
     *
     * Timer 会自动计算百分位数（如 p50、p95、p99），用于评估性能
     */
    public void recordResponseTime(String userId, String appId, String modelName, Duration duration) {
        String key = String.format("%s_%s_%s", userId, appId, modelName);  // 缓存key：拼接所有维度
        Timer timer = responseTimersCache.computeIfAbsent(key, k ->
                Timer.builder("ai_model_response_duration_seconds")  // 指标名称（单位：秒）
                        .description("AI模型响应时间")                  // 指标描述
                        .tag("user_id", userId)                        // 标签：按用户维度统计
                        .tag("app_id", appId)                          // 标签：按应用维度统计
                        .tag("model_name", modelName)                  // 标签：按模型名称统计
                        .register(meterRegistry)                       // 注册到Micrometer注册表
        );
        timer.record(duration);  // 记录耗时
    }
}
