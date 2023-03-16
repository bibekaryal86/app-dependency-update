package app.dependency.update.app.util;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Util {
  // provided at runtime
  public static final String TIME_ZONE = "TZ";

  // constants
  public static final String COMMAND_PATH = "/bin/bash";
  public static final String SCRIPTS_FOLDER = "scripts";
  public static final String CHMOD_COMMAND = "chmod +x ";
  public static final String PARAM_REPO_HOME = "repo_home";

  public static String getSystemEnvProperty(String keyName) {
    return (System.getProperty(keyName) != null)
        ? System.getProperty(keyName)
        : System.getenv(keyName);
  }

  public static LocalDateTime getLocalDateTimeNow() {
    String timeZone =
        Optional.ofNullable(getSystemEnvProperty(TIME_ZONE)).orElse("America/Los_Angeles");
    return LocalDateTime.now(ZoneId.of(timeZone));
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

  public enum HttpMethod {
    POST,
    PUT,
    DELETE,
    GET;
  }
}
