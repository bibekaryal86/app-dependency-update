package app.dependency.update.app.util;

import app.dependency.update.app.model.AppInitData;
import app.dependency.update.app.model.Repository;
import app.dependency.update.app.service.AppInitDataService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ThreadMonitor {

  private final AppInitDataService appInitDataService;

  public ThreadMonitor(final AppInitDataService appInitDataService) {
    this.appInitDataService = appInitDataService;
  }

  public void monitorThreads() {
    final AppInitData appInitData = appInitDataService.appInitData();
    final Set<Thread> threads = Thread.getAllStackTraces().keySet();
    Map<String, Thread.State> threadStatusMap = monitorThreads(appInitData, threads);

    if (threadStatusMap.size() > 0) {
      log.debug("Current Threads Executing Updates: [ {} ]", threadStatusMap);
    }
  }

  private Map<String, Thread.State> monitorThreads(
      final AppInitData appInitData, final Set<Thread> threads) {
    // thread names always begin with repository names
    List<String> threadNames =
        appInitData.getRepositories().stream().map(Repository::getRepoName).toList();

    // running threads that match thread names pattern
    List<Thread> runningThreads =
        threads.stream()
            .filter(
                thread ->
                    threadNames.stream()
                        .anyMatch(threadName -> thread.getName().startsWith(threadName)))
            .toList();

    return runningThreads.stream().collect(Collectors.toMap(Thread::getName, Thread::getState));
  }
}
