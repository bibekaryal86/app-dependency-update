package app.dependency.update.app.execute;

import app.dependency.update.app.model.AppInitData;
import app.dependency.update.app.model.Repository;
import app.dependency.update.app.util.CommonUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ThreadMonitor {

  public ThreadMonitor() {
    Executors.newScheduledThreadPool(1)
        .scheduleWithFixedDelay(this::monitorThreads, 5, 3, TimeUnit.SECONDS);
  }

  private void monitorThreads() {
    final AppInitData appInitData = CommonUtil.getAppInitData();
    final Set<Thread> threads = Thread.getAllStackTraces().keySet();
    Map<String, Thread.State> threadStatusMap = new HashMap<>();
    threadStatusMap.putAll(monitorNpmThreads(appInitData, threads));
    threadStatusMap.putAll(monitorGradleWrapperThreads(appInitData, threads));
    threadStatusMap.putAll(monitorGradleDependenciesThreads(appInitData, threads));

    if (threadStatusMap.size() > 0) {
      log.warn("Current Threads Executing Updates: {}", threadStatusMap);
    } else {
      maybeAnimation();
    }
  }

  private Map<String, Thread.State> monitorNpmThreads(
      final AppInitData appInitData, final Set<Thread> threads) {
    // thread names are set as repository names
    List<String> npmThreadNames =
        appInitData.getRepositories().stream()
            .filter(repository -> repository.getType().equals(CommonUtil.NPM))
            .map(Repository::getRepoName)
            .toList();

    // npm threads
    List<Thread> npmThreads =
        threads.stream().filter(thread -> npmThreadNames.contains(thread.getName())).toList();

    // npm threads with status
    return npmThreads.stream().collect(Collectors.toMap(Thread::getName, Thread::getState));
  }

  private Map<String, Thread.State> monitorGradleWrapperThreads(
      final AppInitData appInitData, final Set<Thread> threads) {
    List<String> gradleWrapperThreadNames =
        appInitData.getRepositories().stream()
            .filter(repository -> repository.getType().equals(CommonUtil.GRADLE))
            .map(repository -> repository.getRepoName() + "_" + CommonUtil.WRAPPER)
            .toList();

    // gradle wrapper threads
    List<Thread> gradleWrapperThreads =
        threads.stream()
            .filter(thread -> gradleWrapperThreadNames.contains(thread.getName()))
            .toList();

    // gradle wrapper threads with status
    return gradleWrapperThreads.stream()
        .collect(Collectors.toMap(Thread::getName, Thread::getState));
  }

  private Map<String, Thread.State> monitorGradleDependenciesThreads(
      final AppInitData appInitData, final Set<Thread> threads) {
    List<String> gradleThreadNames =
        appInitData.getRepositories().stream()
            .filter(repository -> repository.getType().equals(CommonUtil.GRADLE))
            .map(Repository::getRepoName)
            .toList();

    // gradle threads
    List<Thread> gradleThreads =
        threads.stream().filter(thread -> gradleThreadNames.contains(thread.getName())).toList();

    // gradle threads with status
    return gradleThreads.stream().collect(Collectors.toMap(Thread::getName, Thread::getState));
  }

  // suppressing sonarlint rule to use logger instead os system out
  @SuppressWarnings("java:S106")
  private static void maybeAnimation() {
    try {
      System.out.print("\r");
      Thread.sleep(500);
      System.out.print("[ / ]");
      System.out.print("\r");
      Thread.sleep(500);
      System.out.print("[ - ]");
      System.out.print("\r");
      Thread.sleep(500);
      System.out.print("[ \\ ]");
      System.out.print("\r");
      Thread.sleep(500);
      System.out.print("[ | ]");
      System.out.print("\r");
      Thread.sleep(500);
      System.out.print("\r");
      System.out.print("\r");
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
    }
  }
}
