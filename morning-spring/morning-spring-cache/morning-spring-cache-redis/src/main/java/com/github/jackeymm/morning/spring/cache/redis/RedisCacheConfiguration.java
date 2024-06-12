package com.github.jackeymm.morning.spring.cache.redis;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@ConditionalOnClass({RedisOperations.class})
public class RedisCacheConfiguration {

  @Bean
  public RedisTemplate<String, ?> morningRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
    RedisTemplate<String, ?> template = new RedisTemplate<>();
    template.setConnectionFactory(redisConnectionFactory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new TimeAwareGenericJackson2JsonRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(new TimeAwareGenericJackson2JsonRedisSerializer());
    return template;
  }

}
