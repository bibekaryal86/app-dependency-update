package app.dependency.update.app.util;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Collection;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CommonUtil {
  // constants
  public static final String PATH_DELIMITER = "/";
  public static final String GRADLE_WRAPPER_REGEX = "(?<=\\-)(.*?)(?=\\-)";
  public static final String COMMAND_PATH = PATH_DELIMITER + "bin" + PATH_DELIMITER + "bash";
  public static final String SCRIPTS_DIRECTORY = "scripts";
  public static final String CHMOD_COMMAND = "chmod +x ";
  public static final String JAVA_SYSTEM_TMPDIR = System.getProperty("java.io.tmpdir");
  public static final String NPM = "npm";
  public static final String GRADLE = "gradle";
  public static final String WRAPPER = "wrapper";
  public static final String GRADLE_WRAPPER_PROPERTIES = GRADLE + "-" + WRAPPER + ".properties";
  public static final String MONGODB_DATABASE_NAME = "repository";

  // provided at runtime
  public static final String PARAM_REPO_HOME = "repo_home";
  public static final String ENV_MONGO_USERNAME = "mongo_user";
  public static final String ENV_MONGO_PASSWORD = "mongo_pwd";

  // endpoints
  public static final String GRADLE_RELEASES_ENDPOINT =
      "https://api.github.com/repos/gradle/gradle/releases";
  public static final String MAVEN_SEARCH_ENDPOINT =
      "https://search.maven.org/solrsearch/select?wt=json&q=g:%s+AND+a:%s";

  public static boolean isEmpty(Collection<?> c) {
    return (c == null || c.isEmpty());
  }

  public static String getSystemEnvProperty(String keyName, String defaultValue) {
    String envProperty =
        System.getProperty(keyName) != null ? System.getProperty(keyName) : System.getenv(keyName);
    return envProperty == null ? defaultValue : envProperty;
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
    GET
  }
}
