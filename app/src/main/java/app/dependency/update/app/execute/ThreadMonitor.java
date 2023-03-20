package app.dependency.update.app.execute;

import app.dependency.update.app.model.AppInitData;
import app.dependency.update.app.model.Repository;
import app.dependency.update.app.util.Util;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ThreadMonitor {
  private final AppInitData appInitData;

  public ThreadMonitor(AppInitData appInitData) {
    this.appInitData = appInitData;
    Executors.newScheduledThreadPool(1)
        .scheduleWithFixedDelay(this::monitorThreads, 3, 3, TimeUnit.SECONDS);
  }

  private void monitorThreads() {
    Set<Thread> threads = Thread.getAllStackTraces().keySet();
    monitorNpmThreads(threads);
  }

  private void monitorNpmThreads(Set<Thread> threads) {
    // thread names are set as repository names
    List<String> npmThreadNames =
        this.appInitData.getRepositories().stream()
            .filter(repository -> repository.getType().equals(Util.NPM))
            .map(Repository::getRepoName)
            .toList();

    // npm threads
    List<Thread> npmThreads =
        threads.stream().filter(thread -> npmThreadNames.contains(thread.getName())).toList();

    // npm threads with status
    Map<String, Thread.State> threadStatusMap =
        npmThreads.stream().collect(Collectors.toMap(Thread::getName, Thread::getState));

    // print threads and their status
    if (threadStatusMap.size() > 0) {
      log.info("Current NPM threads: {}", threadStatusMap);
    }
  }
}
