package com.hex.ailowcode.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
        // 1. ObjectMapper 配置：添加 @class 类型信息，解决反序列化类型丢失问题
        // 问题：缓存 Page<AppVO> 后，Redis 读取时无法知道 records 里是 AppVO
        // 解决方案：序列化时添加 "@class":"com.xxx.AppVO"，反序列化时自动还原类型
        //
        // Redis 存储示例：
        // {
        //   "@class": "com.mybatisflex.core.paginate.Page",
        //   "records": [{"@class": "com.hex.ailowcode.model.vo.AppVO", "id": 1, ...}],
        //   "total": 100
        // }
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 注册 JavaTimeModule 支持 LocalDateTime 等日期时间类型
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        // 2. 序列化方式：Key 用字符串，Value 用 JSON（带 @class）
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        RedisSerializationContext.SerializationPair<Object> jsonPair =
                RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer);

        // 3. 默认配置：key用String序列化 value用JSON序列化，1 小时过期，不缓存 null
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(jsonPair)
                .entryTtl(Duration.ofHours(1))
                .disableCachingNullValues();

        // 4. 分组特殊配置，继承default配置，然后覆盖指定的配置
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        // 4.1 精选应用分组good_app_page配置: 三十分钟过期
        cacheConfigurations.put("good_app_page", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        // ... 其他分组缓存配置

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}