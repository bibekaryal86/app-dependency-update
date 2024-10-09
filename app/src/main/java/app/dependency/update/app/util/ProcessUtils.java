package app.dependency.update.app.util;

import app.dependency.update.app.model.ProcessedRepository;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProcessUtils {

  private static final AtomicBoolean exceptionCaught = new AtomicBoolean(false);
  private static final AtomicInteger mongoPluginsToUpdate = new AtomicInteger(0);
  private static final AtomicInteger mongoDependenciesToUpdate = new AtomicInteger(0);
  private static final AtomicInteger mongoPackagesToUpdate = new AtomicInteger(0);
  private static final AtomicInteger mongoNpmSkipsActive = new AtomicInteger(0);
  private static Set<String> repositoriesWithPrError = new HashSet<>();
  private static ConcurrentMap<String, ProcessedRepository> processedRepositories =
      new ConcurrentHashMap<>();

  public static void setExceptionCaught(boolean value) {
    exceptionCaught.set(value);
  }

  public static void setMongoPluginsToUpdate(int count) {
    mongoPluginsToUpdate.set(count);
  }

  public static void setMongoDependenciesToUpdate(int count) {
    mongoDependenciesToUpdate.set(count);
  }

  public static void setMongoPackagesToUpdate(int count) {
    mongoPackagesToUpdate.set(count);
  }

  public static void setMongoNpmSkipsActive(int count) {
    mongoNpmSkipsActive.set(count);
  }

  public static boolean getExceptionCaught() {
    return exceptionCaught.get();
  }

  public static int getMongoPluginsToUpdate() {
    return mongoPluginsToUpdate.get();
  }

  public static int getMongoDependenciesToUpdate() {
    return mongoDependenciesToUpdate.get();
  }

  public static int getMongoPackagesToUpdate() {
    return mongoPackagesToUpdate.get();
  }

  public static int getMongoNpmSkipsActive() {
    return mongoNpmSkipsActive.get();
  }

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

  public static void updateProcessedRepositoriesRepoType(String repoName, String repoType) {
    processedRepositories.computeIfPresent(
        repoName,
        (key, processedRepository) -> {
          processedRepository.setRepoType(repoType);
          return processedRepository;
        });
  }

  public static ConcurrentMap<String, ProcessedRepository> getProcessedRepositoriesMap() {
    return processedRepositories;
  }

  public static void resetProcessedRepositoriesAndSummary() {
    processedRepositories = new ConcurrentHashMap<>();
    setMongoPluginsToUpdate(0);
    setMongoDependenciesToUpdate(0);
    setMongoPackagesToUpdate(0);
    setMongoNpmSkipsActive(0);
    setExceptionCaught(false);
  }
}
