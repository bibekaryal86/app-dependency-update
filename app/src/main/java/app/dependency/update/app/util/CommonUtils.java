package app.dependency.update.app.util;

import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.model.Repository;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CommonUtils {

  public static String getSystemEnvProperty(final String keyName, final String defaultValue) {
    String envProperty =
        System.getProperty(keyName) != null ? System.getProperty(keyName) : System.getenv(keyName);
    return envProperty == null ? defaultValue : envProperty;
  }

  public static boolean isEmpty(final String s) {
    return (s == null || s.trim().isEmpty());
  }

  public static boolean isEmpty(final Collection<?> c) {
    return (c == null || c.isEmpty());
  }

  public static String leftTrim(final String line) {
    return line.replaceAll("^\\s+", "");
  }

  public static String threadName(final Repository repository, final String className) {
    return repository.getRepoName() + "--" + className;
  }

  public static String getVersionToCompare(final String version) {
    List<String> strList = Stream.of(version.split("\\.")).limit(3).toList();
    StringBuilder sb = new StringBuilder();
    for (String s : strList) {
      try {
        if (Integer.parseInt(s) < 10) {
          sb.append("0").append(s);
        } else {
          sb.append(s);
        }
      } catch (NumberFormatException ignored) {
        // ignore exception
      }
    }
    return sb.toString();
  }

  public static boolean isRequiresUpdate(final String currentVersion, final String latestVersion) {
    if (isEmpty(currentVersion) || isEmpty(latestVersion)) {
      return false;
    }
    return getVersionToCompare(latestVersion).compareTo(getVersionToCompare(currentVersion)) > 0;
  }

  public static int getPseudoSemaphore() {
    return PSEUDO_SEMAPHORE;
  }

  public static void setPseudoSemaphore(int value) {
    PSEUDO_SEMAPHORE = value;
  }

  public enum UpdateType {
    ALL,
    GITHUB_PULL,
    GITHUB_MERGE,
    GITHUB_RESET,
    GRADLE_DEPENDENCIES,
    GRADLE_WRAPPER,
    NPM_DEPENDENCIES,
    NPM_SNAPSHOT
  }
}
