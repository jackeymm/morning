package org.springframework.boot.autoconfigure.data.redis;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;

public class MorningLettuceConnectionConfiguration extends LettuceConnectionConfiguration {

  public MorningLettuceConnectionConfiguration(RedisProperties properties,
      ObjectProvider<RedisSentinelConfiguration> sentinelConfigurationProvider,
      ObjectProvider<RedisClusterConfiguration> clusterConfigurationProvider) {
    super(properties, sentinelConfigurationProvider, clusterConfigurationProvider);
  }
}
