package app.dependency.update.app.service;

import static app.dependency.update.app.util.CommonUtils.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

@Configuration
public class UpdateRepoServiceOnDemand {

  private final TaskScheduler taskScheduler;
  private final AppInitDataService appInitDataService;
  private final MavenRepoService mavenRepoService;
  private final ScriptFilesService scriptFilesService;
  private final UpdateRepoService updateRepoService;

  public UpdateRepoServiceOnDemand(
      final AppInitDataService appInitDataService,
      final MavenRepoService mavenRepoService,
      final ScriptFilesService scriptFilesService,
      final UpdateRepoService updateRepoService) {
    this.taskScheduler = new ConcurrentTaskScheduler(Executors.newScheduledThreadPool(25));
    this.appInitDataService = appInitDataService;
    this.mavenRepoService = mavenRepoService;
    this.scriptFilesService = scriptFilesService;
    this.updateRepoService = updateRepoService;
  }

  @Async
  public void updateRepoOnDemand() {
    // clear caches
    taskScheduler.schedule(
        () -> {
          appInitDataService.clearAppInitData();
          mavenRepoService.clearPluginsMap();
          mavenRepoService.clearDependenciesMap();
        },
        instant(15, ChronoUnit.SECONDS));

    taskScheduler.schedule(appInitDataService::appInitData, instant(30, ChronoUnit.SECONDS));
    taskScheduler.schedule(mavenRepoService::pluginsMap, instant(35, ChronoUnit.SECONDS));
    taskScheduler.schedule(mavenRepoService::dependenciesMap, instant(40, ChronoUnit.SECONDS));
    taskScheduler.schedule(
        scriptFilesService::deleteTempScriptFiles, instant(45, ChronoUnit.SECONDS));
    taskScheduler.schedule(
        scriptFilesService::createTempScriptFiles, instant(50, ChronoUnit.SECONDS));
    taskScheduler.schedule(
        () -> updateRepoService.updateRepos(UpdateType.GITHUB_PULL),
        instant(1, ChronoUnit.MINUTES));
    taskScheduler.schedule(
        () -> updateRepoService.updateRepos(UpdateType.GRADLE_WRAPPER),
        instant(3, ChronoUnit.MINUTES));
    taskScheduler.schedule(
        () -> updateRepoService.updateRepos(UpdateType.GITHUB_MERGE, true),
        instant(13, ChronoUnit.MINUTES));
    taskScheduler.schedule(
        () -> updateRepoService.updateRepos(UpdateType.GITHUB_PULL),
        instant(16, ChronoUnit.MINUTES));
    taskScheduler.schedule(
        () -> updateRepoService.updateRepos(UpdateType.NPM_DEPENDENCIES),
        instant(19, ChronoUnit.MINUTES));
    taskScheduler.schedule(
        () -> updateRepoService.updateRepos(UpdateType.GRADLE_DEPENDENCIES),
        instant(23, ChronoUnit.MINUTES));
    taskScheduler.schedule(
        () -> updateRepoService.updateRepos(UpdateType.GITHUB_MERGE),
        instant(33, ChronoUnit.MINUTES));
    taskScheduler.schedule(
        () -> updateRepoService.updateRepos(UpdateType.GITHUB_PULL),
        instant(36, ChronoUnit.MINUTES));
    taskScheduler.schedule(
        scriptFilesService::deleteTempScriptFiles, instant(39, ChronoUnit.MINUTES));
  }

  @Async
  public void updateNpmSnapshot(final String branchName) {

    taskScheduler.schedule(
        scriptFilesService::deleteTempScriptFiles, instant(45, ChronoUnit.SECONDS));
    taskScheduler.schedule(
        scriptFilesService::createTempScriptFiles, instant(50, ChronoUnit.SECONDS));
    taskScheduler.schedule(
        () -> updateRepoService.updateRepos(UpdateType.GITHUB_PULL),
        instant(1, ChronoUnit.MINUTES));
    taskScheduler.schedule(
        () -> updateRepoService.updateRepos(UpdateType.NPM_SNAPSHOT, branchName),
        instant(3, ChronoUnit.MINUTES));
    taskScheduler.schedule(
        () -> updateRepoService.updateRepos(UpdateType.GITHUB_MERGE),
        instant(13, ChronoUnit.MINUTES));
    taskScheduler.schedule(
        () -> updateRepoService.updateRepos(UpdateType.GITHUB_PULL),
        instant(17, ChronoUnit.MINUTES));
    taskScheduler.schedule(
        scriptFilesService::deleteTempScriptFiles, instant(20, ChronoUnit.SECONDS));
  }

  private Instant instant(final long amountToAdd, ChronoUnit chronoUnit) {
    return Instant.now().plus(amountToAdd, chronoUnit);
  }
}
