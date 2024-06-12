package org.springframework.boot.autoconfigure.data.redis;

import com.github.jackeymm.morning.spring.cache.redis.MorningRedisProperties;
import com.github.jackeymm.morning.spring.cache.redis.RedisCacheConfiguration;
import io.lettuce.core.RedisClient;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import java.lang.invoke.MethodHandles;
import java.net.UnknownHostException;
import java.util.Map.Entry;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties.Lettuce;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties.Lettuce.Cluster.Refresh;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;

@Configuration
@ConditionalOnClass(RedisClient.class)
@AutoConfigureBefore(CacheAutoConfiguration.class)
public class MorningRedisAutoConfiguration implements BeanFactoryPostProcessor, EnvironmentAware,
    PriorityOrdered {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final RedisCacheConfiguration redisCacheConfiguration = new RedisCacheConfiguration();
  private ConfigurableEnvironment environment;

  private void init(DefaultListableBeanFactory beanFactory) throws UnknownHostException {
    MorningRedisProperties morningRedisProperties = bindMorningRedisProperties(beanFactory);
    ObjectProvider<RedisSentinelConfiguration> sentinelConfigurationProvider = beanFactory
        .getBeanProvider(RedisSentinelConfiguration.class);
    ObjectProvider<RedisClusterConfiguration> clusterConfigurationProvider = beanFactory
        .getBeanProvider(RedisClusterConfiguration.class);
    ObjectProvider<LettuceClientConfigurationBuilderCustomizer> builderCustomizers = beanFactory
        .getBeanProvider(LettuceClientConfigurationBuilderCustomizer.class);

    for (Entry<String, RedisProperties> entry : morningRedisProperties.getSources().entrySet()) {
      createRedisInstance(entry,
          beanFactory,
          entry.getKey().equals(morningRedisProperties.getPrimary()),
          sentinelConfigurationProvider,
          clusterConfigurationProvider,
          builderCustomizers);
    }
    LOG.info("Post processed morning redis configuration");
  }

  private MorningRedisProperties bindMorningRedisProperties(DefaultListableBeanFactory beanFactory) {
    MorningRedisProperties morningRedisProperties = redisProperties();

    registerCustomBean(beanFactory,
        "morning",
        "RedisProperties",
        morningRedisProperties,
        null,
        true,
        MorningRedisProperties.class);

    LOG.info("Bound redis multi source properties: {}", morningRedisProperties);
    return morningRedisProperties;
  }

  private MorningRedisProperties redisProperties() throws FatalBeanException {
    RedisProperties defaultProperties = redisProperties("spring.redis",
        RedisProperties.class,
        RedisProperties::new);

    MorningRedisProperties morningRedisProperties = redisProperties("morning.redis",
        MorningRedisProperties.class,
        () -> {
          throw new FatalBeanException("Could not bind redis multi source properties");
        });

    for (Entry<String, RedisProperties> entry : morningRedisProperties.getSources().entrySet()) {
      String name = entry.getKey();
      RedisProperties properties = entry.getValue();
      Lettuce sourceLettuce = defaultProperties.getLettuce();
      Lettuce targetLettuce = properties.getLettuce();

      if (targetLettuce.getPool() == null) {
        targetLettuce.setPool(sourceLettuce.getPool());
        LOG.info("Set {} default redis lettuce pool to {}", name, targetLettuce.getPool());
      }

      Refresh refresh = targetLettuce.getCluster().getRefresh();
      if (refresh.getPeriod() == null) {
        refresh.setPeriod(sourceLettuce.getCluster().getRefresh().getPeriod());
        refresh.setAdaptive(sourceLettuce.getCluster().getRefresh().isAdaptive());
        LOG.info("Set {} default redis lettuce refresh period to {}", name, refresh.getPeriod());
      }
    }
    return morningRedisProperties;
  }

  private <T> T redisProperties(String prefix, Class<T> type, Supplier<T> supplier) {
    return new Binder(
        ConfigurationPropertySources.from(environment.getPropertySources()))
        .bind(prefix, Bindable.of(type))
        .orElseGet(supplier);
  }

  private void createRedisInstance(Entry<String, RedisProperties> entry,
      DefaultListableBeanFactory beanFactory,
      boolean primary,
      ObjectProvider<RedisSentinelConfiguration> sentinelConfigProvider,
      ObjectProvider<RedisClusterConfiguration> clusterConfigProvider,
      ObjectProvider<LettuceClientConfigurationBuilderCustomizer> builderCustomizers)
      throws UnknownHostException {

    String redisName = entry.getKey();
    RedisProperties redisProperties = entry.getValue();
    ClientResources clientResources = createClientResources(redisName, primary, beanFactory);
    LettuceConnectionFactory connectionFactory = createConnectionFactory(
        beanFactory,
        clientResources,
        redisName,
        primary,
        redisProperties,
        sentinelConfigProvider,
        clusterConfigProvider,
        builderCustomizers);

    createStringRedisTemplate(beanFactory, connectionFactory, redisName, primary);
    createRedisTemplate(beanFactory, connectionFactory, redisName, primary);
  }

  private ClientResources createClientResources(String redisName,
      boolean primary,
      DefaultListableBeanFactory beanFactory) {
    ClientResources clientResources = DefaultClientResources.create();
    registerCustomBean(beanFactory, redisName, "ClientResources", clientResources, "shutdown",
        primary, ClientResources.class);
    return clientResources;
  }

  private LettuceConnectionFactory createConnectionFactory(DefaultListableBeanFactory beanFactory,
      ClientResources clientResources,
      final String redisName,
      final boolean primary,
      final RedisProperties redisProperties,
      ObjectProvider<RedisSentinelConfiguration> sentinelConfigProvider,
      ObjectProvider<RedisClusterConfiguration> clusterConfigProvider,
      ObjectProvider<LettuceClientConfigurationBuilderCustomizer> builderCustomizers)
      throws UnknownHostException {

    LettuceConnectionFactory connectionFactory = lettuceConnectionFactory(
        clientResources, redisProperties, sentinelConfigProvider, clusterConfigProvider,
        builderCustomizers);

    registerCustomBean(beanFactory, redisName, "RedisConnectionFactory", connectionFactory, null,
        primary, LettuceConnectionFactory.class);
    return connectionFactory;
  }

  private LettuceConnectionFactory lettuceConnectionFactory(ClientResources clientResources,
      RedisProperties redisProperties,
      ObjectProvider<RedisSentinelConfiguration> sentinelConfigurationProvider,
      ObjectProvider<RedisClusterConfiguration> clusterConfigurationProvider,
      ObjectProvider<LettuceClientConfigurationBuilderCustomizer> builderCustomizers)
      throws UnknownHostException {
    MorningLettuceConnectionConfiguration configuration = new MorningLettuceConnectionConfiguration(
        redisProperties,
        sentinelConfigurationProvider,
        clusterConfigurationProvider);
    LettuceConnectionFactory redisConnectionFactory = configuration
        .redisConnectionFactory(builderCustomizers, clientResources);
    redisConnectionFactory.afterPropertiesSet();
    return redisConnectionFactory;
  }

  private void createRedisTemplate(DefaultListableBeanFactory beanFactory,
      LettuceConnectionFactory redisConnectionFactory,
      String redisName,
      boolean primary) {
    createRedisTemplate(
        beanFactory,
        redisConnectionFactory,
        redisName,
        primary,
        redisCacheConfiguration.morningRedisTemplate(redisConnectionFactory)
    );
  }

  private void createStringRedisTemplate(DefaultListableBeanFactory beanFactory,
      LettuceConnectionFactory redisConnectionFactory,
      String redisName,
      boolean primary) {
    createRedisTemplate(
        beanFactory,
        redisConnectionFactory,
        redisName,
        primary,
        new StringRedisTemplate()
    );
  }

  private <T> void createRedisTemplate(DefaultListableBeanFactory beanFactory,
      LettuceConnectionFactory redisConnectionFactory,
      String redisName,
      boolean primary,
      RedisTemplate<String, T> redisTemplate) {

    Class<?> beanClass = redisTemplate.getClass();

    redisTemplate.setConnectionFactory(redisConnectionFactory);
    redisTemplate.afterPropertiesSet();
    registerCustomBean(beanFactory, redisName, beanClass.getSimpleName(), redisTemplate, null,
        primary, RedisTemplate.class);
  }

  private <T> void registerCustomBean(
      DefaultListableBeanFactory beanFactory,
      String beanName,
      String beanType,
      T bean,
      String destroyMethodName,
      boolean primary,
      Class<T> beanClass) {

    RootBeanDefinition beanDefinition = new RootBeanDefinition(beanClass, () -> bean);
    beanDefinition.setDestroyMethodName(destroyMethodName);
    beanDefinition.setPrimary(primary);

    final String finalName = beanName + beanType;
    beanFactory.registerBeanDefinition(finalName, beanDefinition);
    LOG.info("Created {} with beanName = {}", beanClass.getSimpleName(), finalName);
  }

  @Override
  public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory)
      throws BeansException {
    try {
      init((DefaultListableBeanFactory) beanFactory);
    } catch (UnknownHostException e) {
      throw new IllegalStateException("Failed to initialize redis multi source configuration", e);
    }
  }

  @Override
  public void setEnvironment(@NonNull Environment environment) {
    this.environment = (ConfigurableEnvironment) environment;
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE - 1;
  }
}
