package com.ailearn.config;

import com.ailearn.common.CacheNames;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 配置:String 键 + JSON 值(便于在 redis-cli 直接查看),
 * 并自定义 Spring Cache 的序列化方式,避免默认 JDK 序列化导致的乱码与版本问题。
 */
@Configuration
public class RedisConfig {

    /**
     * 统一的对象映射:支持 Java8 时间类型,并写入类型信息以便反序列化。
     *
     * <p>关键:<b>必须关闭 FAIL_ON_UNKNOWN_PROPERTIES</b>。VO 里的 accuracy / rate / correctRate
     * 是计算属性(只有 getter、没有字段与 setter),序列化后会出现在 JSON 中,
     * 若开启严格模式,命中缓存反序列化时会抛 UnrecognizedPropertyException → 接口 500。</p>
     */
    private ObjectMapper cacheObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        return mapper;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        GenericJackson2JsonRedisSerializer valueSerializer =
                new GenericJackson2JsonRedisSerializer(cacheObjectMapper());

        StringRedisSerializer keySerializer = new StringRedisSerializer();
        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 缓存管理器:按用途设置不同过期时间,键前缀 ailearn:cache:,值用 JSON 序列化。
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .prefixCacheNameWith("ailearn:cache:")
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(cacheObjectMapper())))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> configs = new HashMap<>();
        // 热门题目变化快,缓存短一些
        configs.put(CacheNames.HOT_QUESTIONS, base.entryTtl(Duration.ofMinutes(5)));
        // 题目详情 30 分钟
        configs.put(CacheNames.QUESTION_DETAIL, base.entryTtl(Duration.ofMinutes(30)));
        // 科目与知识点属于低频变更数据,缓存 2 小时
        configs.put(CacheNames.SUBJECT_LIST, base.entryTtl(Duration.ofHours(2)));
        configs.put(CacheNames.KNOWLEDGE_TREE, base.entryTtl(Duration.ofHours(2)));
        // 考试详情 10 分钟
        configs.put(CacheNames.EXAM_DETAIL, base.entryTtl(Duration.ofMinutes(10)));
        // 学习统计 10 分钟
        configs.put(CacheNames.USER_STATS, base.entryTtl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(base)
                .withInitialCacheConfigurations(configs)
                .build();
    }
}
