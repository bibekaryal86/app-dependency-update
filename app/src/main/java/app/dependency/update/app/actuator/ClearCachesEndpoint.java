package app.dependency.update.app.actuator;

import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@SuppressWarnings("unused")
@Slf4j
@Component
@Endpoint(id = "clearCaches")
public class ClearCachesEndpoint {

  private final CacheManager cacheManager;

  public ClearCachesEndpoint(final CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  @ReadOperation
  public String clearCaches() {
    log.info("Firing Clear Caches Actuator!!!");
    List<String> cacheNames = cacheManager.getCacheNames().stream().toList();
    cacheManager
        .getCacheNames()
        .forEach(cacheName -> Objects.requireNonNull(cacheManager.getCache(cacheName)).clear());
    return String.format(
        "{\"status\": \"Finished Clear Caches Actuator!\", \"cacheNames\": \"%s\"}", cacheNames);
  }

  @ReadOperation
  public String clearCache(@Selector String cacheName) {
    log.info("Firing Clear Cache Actuator: [ {} ]!!!", cacheName);
    Objects.requireNonNull(cacheManager.getCache(cacheName)).clear();
    return String.format(
        "{\"status\": \"Finished Clear Cache Actuator!\", \"cacheName\": \"%s\"}", cacheName);
  }
}
