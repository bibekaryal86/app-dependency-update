package app.dependency.update.app.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
public class InterceptorUtilsLoggingRequests implements HandlerInterceptor {

  @Override
  public boolean preHandle(
      final HttpServletRequest request,
      final @NonNull HttpServletResponse response,
      final @NonNull Object handler) {
    request.setAttribute("startTime", System.currentTimeMillis());
    if (!request.getRequestURI().contains("swagger-ui")
        && !request.getRequestURI().contains("api-docs")) {
      log.info("Receiving [ {} ] URL [ {} ]", request.getMethod(), request.getRequestURI());
    }
    return true;
  }

  @Override
  public void afterCompletion(
      final HttpServletRequest request,
      final @NonNull HttpServletResponse response,
      final @NonNull Object handler,
      Exception ex) {
    long duration = System.currentTimeMillis() - (Long) request.getAttribute("startTime");
    if (!request.getRequestURI().contains("swagger-ui")
        && !request.getRequestURI().contains("api-docs")) {
      log.info(
          "Returning [ {} ] Status Code [ {} ] URL [ {} ] AFTER [ {}ms]",
          request.getMethod(),
          response.getStatus(),
          request.getRequestURI(),
          duration);
    }
  }
}
