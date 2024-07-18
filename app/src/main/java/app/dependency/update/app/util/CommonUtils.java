package app.dependency.update.app.util;

import app.dependency.update.app.model.ProcessedRepository;
import app.dependency.update.app.model.Repository;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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

  private static Set<String> repositoriesWithPrError = new HashSet<>();
  private static ConcurrentMap<String, ProcessedRepository> processedRepositories =
      new ConcurrentHashMap<>();

  public static synchronized void addRepositoriesWithPrError(final String repoName) {
    repositoriesWithPrError.add(repoName);
  }

  public static synchronized void removeRepositoriesWithPrError(final String repoName) {
    repositoriesWithPrError.remove(repoName);
  }

  public static synchronized Set<String> getRepositoriesWithPrError() {
    return repositoriesWithPrError;
  }

  public static synchronized void resetRepositoriesWithPrError() {
    repositoriesWithPrError = new HashSet<>();
  }

  public static void addProcessedRepositories(
      String repoName, boolean isPrCreateAttempted, boolean isPrCreateError) {
    processedRepositories.put(
        repoName,
        ProcessedRepository.builder()
            .repoName(repoName)
            .isPrCreated(isPrCreateAttempted && !isPrCreateError)
            .isPrCreateError(isPrCreateError)
            .build());
  }

  public static void updateProcessedRepositoriesToPrMerged(String repoName) {
    processedRepositories.computeIfPresent(
        repoName,
        (key, processedRepository) -> {
          processedRepository.setPrMerged(true);
          return processedRepository;
        });
  }

  public static ConcurrentMap<String, ProcessedRepository> getProcessedRepositoriesMap() {
    return processedRepositories;
  }

  public static void resetProcessedRepositories() {
    processedRepositories = new ConcurrentHashMap<>();
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
