package com.hex.ailowcode;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

//启用 Spring 基于 AspectJ 注解的 AOP 自动代理，
// 并将生成的代理对象暴露到 AopContext 中，以解决目标 Bean 内部方法调用无法触发 AOP 逻辑的问题。
@EnableCaching // 启用SpringDataRedis注解
@EnableAspectJAutoProxy(exposeProxy = true)
@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
@MapperScan("com.hex.ailowcode.mapper")
public class AiLowcodeApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiLowcodeApplication.class, args);
    }
}
