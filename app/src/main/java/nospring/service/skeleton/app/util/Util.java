package nospring.service.skeleton.app.util;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Util {
  // provided at runtime
  public static final String SERVER_PORT = "PORT";
  public static final String TIME_ZONE = "TZ";
  public static final String PROFILE = "SPRING_PROFILES_ACTIVE";
  public static final String BASIC_AUTH_USR = "BASIC_AUTH_USR";
  public static final String BASIC_AUTH_PWD = "BASIC_AUTH_PWD";

  // server context-path
  public static final String CONTEXT_PATH = "/nospring-service-skeleton"; // NOSONAR

  // others
  public static final int SERVER_MAX_THREADS = 100;
  public static final int SERVER_MIN_THREADS = 20;
  public static final int SERVER_IDLE_TIMEOUT = 120;

  public static String getSystemEnvProperty(String keyName) {
    return (System.getProperty(keyName) != null)
        ? System.getProperty(keyName)
        : System.getenv(keyName);
  }

  public static LocalDateTime getLocalDateTimeNow() {
    return LocalDateTime.now(ZoneId.of(getSystemEnvProperty(Util.TIME_ZONE)));
  }

  public static boolean hasText(String s) {
    return (s != null && !s.trim().isEmpty());
  }

  public static boolean isEmpty(Collection<?> c) {
    return (c == null || c.isEmpty());
  }

  public static boolean isEmpty(Map<?, ?> m) {
    return (m == null || m.isEmpty());
  }

  public static Gson getGson() {
    return new GsonBuilder()
        .setExclusionStrategies(
            new ExclusionStrategy() {
              public boolean shouldSkipField(FieldAttributes f) {
                return (f == null);
              }

              public boolean shouldSkipClass(Class<?> clazz) {
                return false;
              }
            })
        .create();
  }

  public static boolean isAuthenticatedRequest(HttpServletRequest request) {
    String username = getSystemEnvProperty(BASIC_AUTH_USR);
    String password = getSystemEnvProperty(BASIC_AUTH_PWD);
    String authorization =
        Base64.getEncoder().encodeToString(String.format("%s:%s", username, password).getBytes());
    String headerAuth = request.getHeader("Authorization");
    return hasText(headerAuth) && headerAuth.equals(String.format("Basic %s", authorization));
  }

  public static String getRequestPathParameter(
      HttpServletRequest request, int length, int position) {
    String[] requestUriArray = request.getRequestURI().split("/");
    if (requestUriArray.length == length && hasText(requestUriArray[position])) {
      return requestUriArray[position];
    }
    return null;
  }

  public static Object getRequestBody(HttpServletRequest request, Class<?> clazz) {
    try {
      return getGson().fromJson(request.getReader(), clazz);
    } catch (Exception ex) {
      return null;
    }
  }
}
