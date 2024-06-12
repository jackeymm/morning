package com.github.jackeymm.morning.test.contaniners;

import java.util.HashMap;
import java.util.Map;
import org.junit.runner.Description;
import org.testcontainers.containers.GenericContainer;

public abstract class LongLiveContainer<T extends GenericContainer<T>> extends GenericContainer<T> {

  public static final String CONTAINER_SOCKET_PROVIDER_TIMEOUT = "testcontainers.npipesocketprovider.timeout";
  private final Map<String, String> environment;

  public LongLiveContainer(String imageName) {
    super(imageName);
    this.environment = new HashMap<>();
  }

  @Override
  protected void starting(Description description) {
    System.setProperty(CONTAINER_SOCKET_PROVIDER_TIMEOUT, "1");
    super.starting(description);
    initEnvironment();
  }

  @Override
  protected void finished(Description description) {
    clearEnvironment();
    System.clearProperty(CONTAINER_SOCKET_PROVIDER_TIMEOUT);
  }

  private void initEnvironment() {
    environment.putAll(environmentVariables());
    environment.forEach(System::setProperty);
  }

  private void clearEnvironment() {
    environment.keySet().forEach(System::clearProperty);
  }

  abstract Map<String, String> environmentVariables();
}
