package com.hex.ailowcode.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import jakarta.annotation.Resource;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 缓存管理器配置
 * <p>
 * 配置说明：
 * - value: 缓存名称（区域）
 * - key: 缓存键生成方式
 * - condition: 缓存条件
 * <p>
 * 默认配置：
 * - 序列化方式：JSON（可读性好，跨语言友好）
 * - 默认过期时间：1 小时
 * - 不缓存 null 值
 */
@Configuration
public class RedisCacheManagerConfig {

    @Resource
    private RedisConnectionFactory redisConnectionFactory;

    /**
     * 配置 RedisCacheManager
     */
    @Bean
    public CacheManager cacheManager() {
        // 1. 创建 ObjectMapper（JSON 序列化配置）
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        // 2. 配置序列化方式
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        RedisSerializationContext.SerializationPair<Object> jsonPair =
                RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer);

        // 3. 默认缓存配置
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(jsonPair)                    // JSON 序列化
                .entryTtl(Duration.ofHours(1))                    // 默认 1 小时过期
                .disableCachingNullValues();                      // 不缓存 null 值

        // 4. 针对不同缓存区域的特殊配置
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        // 精选应用列表：30 分钟过期（数据变化较频繁）
        cacheConfigurations.put("good_app_page",
                defaultConfig.entryTtl(Duration.ofMinutes(30)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}