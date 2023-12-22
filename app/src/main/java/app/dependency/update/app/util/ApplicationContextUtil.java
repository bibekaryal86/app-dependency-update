package app.dependency.update.app.util;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
/*
 Utility class to access spring beans in static method/class
*/
public class ApplicationContextUtil implements ApplicationContextAware {
  private static ApplicationContext context;

  // suppressing sonarlint rule for static field maintainability
  @SuppressWarnings("java:S2696")
  @Override
  public void setApplicationContext(@NotNull ApplicationContext applicationContext)
      throws BeansException {
    context = applicationContext;
  }

  public static ApplicationContext getApplicationContext() {
    return context;
  }

  public static <T> T getBean(Class<T> clazz) {
    return context.getBean(clazz);
  }
}
