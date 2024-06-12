package com.github.jackeymm.morning.spring.cache.redis;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS;
import static com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.NON_FINAL;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

public class TimeAwareGenericJackson2JsonRedisSerializer extends
    GenericJackson2JsonRedisSerializer {

  private static final ObjectMapper mapper = new ObjectMapper();

  public TimeAwareGenericJackson2JsonRedisSerializer() {
    super(mapper);

    registerNullValueSerializer(mapper, null);

    mapper.registerModule(new JavaTimeModule());
    mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(), NON_FINAL, As.PROPERTY);
    mapper.enable(WRITE_DATES_AS_TIMESTAMPS);
    mapper.disable(WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
    mapper.disable(READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
    mapper.disable(FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }
}
