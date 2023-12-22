package app.dependency.update.app.config;

import app.dependency.update.app.util.InterceptorUtilsLoggingRequests;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

  @Override
  public void addInterceptors(final InterceptorRegistry registry) {
    registry.addInterceptor(new InterceptorUtilsLoggingRequests());
  }
}
