package app.dependency.update.app.util;

import app.dependency.update.app.model.Repository;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CommonUtils {

  public static String getSystemEnvProperty(final String keyName) {
    return System.getProperty(keyName) != null
        ? System.getProperty(keyName)
        : System.getenv(keyName);
  }

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

  public static String threadName(final String className) {
    return className + "--" + className;
  }

  public static boolean isCheckPreReleaseVersion(final String version) {
    String versionLowercase = version.toLowerCase();
    return versionLowercase.contains("alpha")
        || versionLowercase.contains("a")
        || versionLowercase.contains("beta")
        || versionLowercase.contains("b")
        || versionLowercase.contains("rc")
        || versionLowercase.contains("m")
        || versionLowercase.contains("snapshot");
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

  public static boolean checkDependenciesUpdate(final UpdateType updateType) {
    return updateType == UpdateType.ALL
        || updateType == UpdateType.NPM_DEPENDENCIES
        || updateType == UpdateType.GRADLE_DEPENDENCIES
        || updateType == UpdateType.PYTHON_DEPENDENCIES;
  }

  /**
   * @param versionFull eg: 3.12 or 3.12.7
   * @return eg: 312 or 3.12
   */
  public static String getVersionMajorMinor(final String versionFull, final boolean includePeriod) {
    String[] parts = versionFull.split("\\.");

    if (parts.length >= 2) {
      if (includePeriod) {
        return parts[0] + "." + parts[1];
      } else {
        return parts[0] + parts[1];
      }
    } else {
      return versionFull;
    }
  }

  public static int compareVersions(String version1, String version2) {
    String[] v1Parts = version1.split("\\.");
    String[] v2Parts = version2.split("\\.");

    int length =
        Math.max(v1Parts.length, v2Parts.length); // Compare based on the longest version string
    for (int i = 0; i < length; i++) {
      int v1Part = i < v1Parts.length ? Integer.parseInt(v1Parts[i]) : 0;
      int v2Part = i < v2Parts.length ? Integer.parseInt(v2Parts[i]) : 0;

      if (v1Part < v2Part) {
        return -1; // version1 is smaller
      } else if (v1Part > v2Part) {
        return 1; // version1 is greater
      }
    }
    return 0; // Both versions are equal
  }

  public static int parseIntSafe(final String input) {
    try {
      return Integer.parseInt(input);
    } catch (NumberFormatException ex) {
      return 0;
    }
  }

  public enum UpdateType {
    ALL,
    GITHUB_BRANCH_DELETE,
    GITHUB_PR_CREATE,
    GITHUB_PULL,
    GITHUB_MERGE,
    GITHUB_RESET,
    GRADLE_DEPENDENCIES,
    GRADLE_SPOTLESS,
    NPM_DEPENDENCIES,
    NPM_SNAPSHOT,
    PYTHON_DEPENDENCIES
  }

  public enum CacheType {
    ALL,
    APP_INIT_DATA,
    PLUGINS_MAP,
    DEPENDENCIES_MAP
  }

  public enum LogLevelChange {
    INFO,
    DEBUG
  }
}
