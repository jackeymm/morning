package com.github.jackeymm.morning.test.contaniners;

import java.util.Map;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

public class RedisContainer extends LongLiveContainer<RedisContainer> {

  public static final int REDIS_PORT = 6379;

  public RedisContainer() {
    super("redis:5.0.14-alpine");
  }

  @Override
  protected void configure() {
    super.configure();
    withExposedPorts(REDIS_PORT);
  }

  @Override
  Map<String, String> environmentVariables() {
    return ImmutableMap.of(
        "spring.redis.host", getContainerIpAddress(),
        "spring.redis.port", String.valueOf(getMappedPort(REDIS_PORT))
    );
  }
}
