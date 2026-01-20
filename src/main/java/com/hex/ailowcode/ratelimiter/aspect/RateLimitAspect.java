package com.hex.ailowcode.ratelimiter.aspect;

import com.hex.ailowcode.exception.BusinessException;
import com.hex.ailowcode.exception.ErrorCode;
import com.hex.ailowcode.model.entity.User;
import com.hex.ailowcode.ratelimiter.annotation.RateLimit;
import com.hex.ailowcode.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Duration;

@Aspect
@Component
@Slf4j
public class RateLimitAspect {
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private UserService userService;

    /**
     * 限流拦截：方法执行前检查是否超限
     *
     * 流程：生成限流 key → 获取/创建限流器 → 尝试获取令牌 → 失败则抛异常
     */
    @Before("@annotation(rateLimit)")
    public void doBefore(JoinPoint point, RateLimit rateLimit) {
        String key = generateRateLimitKey(point, rateLimit);
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        // 限流器不存在时才创建，避免重复设置
        if (!rateLimiter.isExists()) {
            // 设置限流器参数：每个时间窗口允许的请求数和时间窗口
            rateLimiter.trySetRate(RateType.OVERALL, rateLimit.rate(), rateLimit.rateInterval(), RateIntervalUnit.SECONDS);
            // 1 小时无请求后自动删除限流器，释放 Redis 内存
            rateLimiter.expire(Duration.ofHours(1));
        }

        // 尝试获取令牌，如果获取失败则限流
        if (!rateLimiter.tryAcquire(1)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, rateLimit.message());
        }
    }

    /**
     * 生成限流 key：根据限流类型（API/USER/IP）生成唯一标识
     *
     * 格式：rate_limit:[自定义前缀:]<类型>:<标识>
     *
     * 示例：
     * - API:   rate_limit:api:AppController.listGoodAppVOByPage
     *          └─ 通过 MethodSignature 反射获取 类名.方法名
     *
     * - USER:  rate_limit:user:100
     *          └─ 通过 getLoginUser() 获取用户 ID
     *          └─ 未登录降级为 IP 限流
     *
     * - IP:    rate_limit:ip:192.168.1.1
     *          └─ 通过 X-Forwarded-For / X-Real-IP / RemoteAddr 获取
     */
    private String generateRateLimitKey(JoinPoint point, RateLimit rateLimit) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append("rate_limit:");
        // 添加自定义前缀
        if (!rateLimit.key().isEmpty()) {
            keyBuilder.append(rateLimit.key()).append(":");
        }
        // 根据限流类型生成不同的key
        switch (rateLimit.limitType()) {
            case API:
                // 接口级别："方法名" 作为key的一部分
                MethodSignature signature = (MethodSignature) point.getSignature(); // 通过反射获取方法签名
                Method method = signature.getMethod();
                keyBuilder.append("api:").append(method.getDeclaringClass().getSimpleName()).append(".")
                        .append(method.getName()); // 拼接方法名
                break;
            case USER:
                // 用户级别：用户ID
                try {
                    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    if (attributes != null) {
                        HttpServletRequest request = attributes.getRequest();
                        User loginUser = userService.getLoginUser(request);
                        keyBuilder.append("user:").append(loginUser.getId());
                    } else {
                        // 无法获取请求上下文，使用IP限流
                        keyBuilder.append("ip:").append(getClientIP());
                    }
                } catch (BusinessException e) {
                    // 未登录用户使用IP限流
                    keyBuilder.append("ip:").append(getClientIP());
                }
                break;
            case IP:
                // IP级别：客户端IP
                keyBuilder.append("ip:").append(getClientIP());
                break;
            default:
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的限流类型");
        }
        return keyBuilder.toString();
    }

    /**
     * 从请求头中获取客户端真实 IP
     *
     * 获取流程：
     * 1. X-Forwarded-For（经过所有代理的 IP 链）
     * 2. X-Real-IP（经过一层代理的 IP）
     * 3. RemoteAddr（直连 IP，可能是代理 IP）
     *
     * 多级代理示例：客户端(1.1.1.1) → 代理1(2.2.2.2) → 代理2(3.3.3.3) → 服务器
     * - X-Forwarded-For: 1.1.1.1, 2.2.2.2
     * - 取第一个：1.1.1.1（真实客户端 IP）
     */
    private String getClientIP() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多级代理的情况
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }
}
