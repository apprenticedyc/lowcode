package com.hex.ailowcode.monitor;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 基于 ThreadLocal 实现的监控上下文管理器，用于在同一线程中传递监控相关的业务数据（如用户ID、应用ID）。
 * 主要用于在拦截器、AOP切面、Service层之间共享，避免层层传参。
 * </p>
 * <p>典型使用场景：</p>
 * <ul>
 *   <li>请求入口（拦截器）设置上下文</li>
 *   <li>业务处理过程中获取上下文信息</li>
 *   <li>请求结束或异常时清除上下文，防止内存泄漏</li>
 * </ul>
 *
 * @see MonitorContext
 */
@Slf4j
public class MonitorContextHolder {

    private static final ThreadLocal<MonitorContext> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 设置监控上下文
     */
    public static void setContext(MonitorContext context) {
        CONTEXT_HOLDER.set(context);
    }

    /**
     * 获取当前监控上下文
     */
    public static MonitorContext getContext() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * 清除监控上下文
     */
    public static void clearContext() {
        CONTEXT_HOLDER.remove();
    }
}