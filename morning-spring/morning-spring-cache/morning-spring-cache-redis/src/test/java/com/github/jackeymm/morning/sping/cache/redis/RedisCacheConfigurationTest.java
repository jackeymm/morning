package com.github.jackeymm.morning.sping.cache.redis;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.jackeymm.morning.spring.cache.redis.RedisCacheConfiguration;
import com.github.jackeymm.morning.test.contaniners.RedisContainer;
import com.github.jackeymm.morning.test.contaniners.Containers;
import java.util.Date;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.boot.test.context.SpringBootTest;

@RunWith(SpringRunner.class)
@ImportAutoConfiguration({RedisCacheConfiguration.class, RedisAutoConfiguration.class})
//@SpringBootTest
public class RedisCacheConfigurationTest {

  @ClassRule
  public static final RedisContainer redis = Containers.REDIS;
  private static final String KEY = "users";

  @Autowired
  private RedisTemplate<String, Date> redisTemplate;

  @Test
  public void shouldConnectToRedis() {
    Long size = redisTemplate.opsForList().size(KEY);
    assertThat(size).isZero();

    Date now = new Date();
    redisTemplate.opsForList().leftPush(KEY, now);

    size = redisTemplate.opsForList().size(KEY);
    assertThat(size).isOne();

    Date actual = redisTemplate.opsForList().leftPop(KEY);
    assertThat(actual).isEqualTo(now);
  }
}
