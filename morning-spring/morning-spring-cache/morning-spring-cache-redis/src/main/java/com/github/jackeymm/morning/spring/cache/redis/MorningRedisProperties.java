package com.github.jackeymm.morning.spring.cache.redis;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "morning.redis")
public class MorningRedisProperties {

  private String primary;

  @NestedConfigurationProperty
  private Map<String, RedisProperties> sources = new HashMap<>();

  public String getPrimary() {
    return primary;
  }

  public void setPrimary(String primary) {
    this.primary = primary;
  }

  public Map<String, RedisProperties> getSources() {
    return sources;
  }

  public void setSources(Map<String, RedisProperties> sources) {
    this.sources = sources;
  }
}
