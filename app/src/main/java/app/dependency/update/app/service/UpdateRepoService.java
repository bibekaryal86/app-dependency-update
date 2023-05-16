package app.dependency.update.app.service;

import static app.dependency.update.app.util.CommonUtils.*;

import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import app.dependency.update.app.model.AppInitData;
import app.dependency.update.app.runnable.UpdateGithubMerge;
import app.dependency.update.app.runnable.UpdateGithubPull;
import app.dependency.update.app.runnable.UpdateGithubReset;
import app.dependency.update.app.runnable.UpdateGradleDependencies;
import app.dependency.update.app.runnable.UpdateNpmDependencies;
import app.dependency.update.app.runnable.UpdateNpmSnapshots;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UpdateRepoService {

  private final ConcurrentTaskScheduler taskScheduler;
  private final AppInitDataService appInitDataService;
  private final MavenRepoService mavenRepoService;
  private final ScriptFilesService scriptFilesService;

  private static final Integer SCHED_BEGIN = 1;

  public UpdateRepoService(
      final AppInitDataService appInitDataService,
      final MavenRepoService mavenRepoService,
      final ScriptFilesService scriptFilesService) {
    this.taskScheduler = new ConcurrentTaskScheduler(Executors.newScheduledThreadPool(25));
    this.appInitDataService = appInitDataService;
    this.mavenRepoService = mavenRepoService;
    this.scriptFilesService = scriptFilesService;
  }

  @Scheduled(cron = "0 0 20 * * *")
  void updateReposScheduler() {
    if (isTaskRunning()) {
      log.info("Something is running, rescheduling 30 minutes from now...");
      taskScheduler.schedule(this::updateReposScheduler, instant(30, ChronoUnit.MINUTES));
    } else {
      updateRepos();
    }
  }

  public boolean isTaskRunning() {
    ScheduledThreadPoolExecutor executor =
        (ScheduledThreadPoolExecutor) taskScheduler.getConcurrentExecutor();
    log.info("Is Task Running: [ {} ]", executor.getQueue().peek() != null);
    return executor.getQueue().peek() != null;
  }

  public void updateRepos() {
    // clear caches
    taskScheduler.schedule(
        () -> {
          appInitDataService.clearAppInitData();
          mavenRepoService.clearPluginsMap();
          mavenRepoService.clearDependenciesMap();
        },
        instant(SCHED_BEGIN + (long) 1, ChronoUnit.SECONDS));
    taskScheduler.schedule(
        appInitDataService::appInitData, instant(SCHED_BEGIN + (long) 3, ChronoUnit.SECONDS));
    taskScheduler.schedule(
        mavenRepoService::pluginsMap, instant(SCHED_BEGIN + (long) 7, ChronoUnit.SECONDS));
    taskScheduler.schedule(
        mavenRepoService::updateDependenciesInMongo,
        instant(SCHED_BEGIN + (long) 10, ChronoUnit.SECONDS));
    taskScheduler.schedule(
        mavenRepoService::dependenciesMap, instant(SCHED_BEGIN + (long) 75, ChronoUnit.SECONDS));
    taskScheduler.schedule(
        scriptFilesService::deleteTempScriptFilesBegin,
        instant(SCHED_BEGIN + (long) 15, ChronoUnit.SECONDS));
    taskScheduler.schedule(
        scriptFilesService::createTempScriptFiles,
        instant(SCHED_BEGIN + (long) 17, ChronoUnit.SECONDS));
    taskScheduler.schedule(
        () -> updateRepos(UpdateType.GITHUB_PULL, null),
        instant(SCHED_BEGIN + (long) 30, ChronoUnit.SECONDS));
    taskScheduler.schedule(
        () -> updateRepos(UpdateType.NPM_DEPENDENCIES, null),
        instant(SCHED_BEGIN + (long) 1, ChronoUnit.MINUTES));
    taskScheduler.schedule(
        () -> updateRepos(UpdateType.GRADLE_DEPENDENCIES, null),
        instant(SCHED_BEGIN + (long) 3, ChronoUnit.MINUTES));
    taskScheduler.schedule(
        () -> updateRepos(UpdateType.GITHUB_MERGE, null),
        instant(SCHED_BEGIN + (long) 12, ChronoUnit.MINUTES));
    taskScheduler.schedule(
        () -> updateRepos(UpdateType.GITHUB_PULL, null),
        instant(SCHED_BEGIN + (long) 14, ChronoUnit.MINUTES));
    taskScheduler.schedule(
        scriptFilesService::deleteTempScriptFilesEnd,
        instant(SCHED_BEGIN + (long) 15, ChronoUnit.MINUTES));
  }

  public void updateRepos(final UpdateType updateType) {
    taskScheduler.schedule(
        scriptFilesService::deleteTempScriptFilesBegin,
        instant(SCHED_BEGIN + (long) 1, ChronoUnit.SECONDS));
    taskScheduler.schedule(
        scriptFilesService::createTempScriptFiles,
        instant(SCHED_BEGIN + (long) 4, ChronoUnit.SECONDS));
    taskScheduler.schedule(
        () -> updateRepos(updateType, null), instant(SCHED_BEGIN + (long) 10, ChronoUnit.SECONDS));
    taskScheduler.schedule(
        scriptFilesService::deleteTempScriptFilesEnd,
        instant(SCHED_BEGIN + (long) 5, ChronoUnit.MINUTES));
  }

  public void updateRepos(final String branchName) {
    taskScheduler.schedule(
        scriptFilesService::deleteTempScriptFilesBegin,
        instant(SCHED_BEGIN + (long) 1, ChronoUnit.SECONDS));
    taskScheduler.schedule(
        scriptFilesService::createTempScriptFiles,
        instant(SCHED_BEGIN + (long) 4, ChronoUnit.SECONDS));
    taskScheduler.schedule(
        () -> updateRepos(UpdateType.GITHUB_PULL, null),
        instant(SCHED_BEGIN + (long) 7, ChronoUnit.SECONDS));
    taskScheduler.schedule(
        () -> updateRepos(UpdateType.NPM_SNAPSHOT, branchName),
        instant(SCHED_BEGIN + (long) 10, ChronoUnit.SECONDS));
    taskScheduler.schedule(
        () -> updateRepos(UpdateType.GITHUB_MERGE, null),
        instant(SCHED_BEGIN + (long) 5, ChronoUnit.MINUTES));
    taskScheduler.schedule(
        () -> updateRepos(UpdateType.GITHUB_PULL, null),
        instant(SCHED_BEGIN + (long) 8, ChronoUnit.MINUTES));
    taskScheduler.schedule(
        scriptFilesService::deleteTempScriptFilesEnd,
        instant(SCHED_BEGIN + (long) 9, ChronoUnit.MINUTES));
  }

  private void updateRepos(final UpdateType updateType, final String branchName) {
    AppInitData appInitData = appInitDataService.appInitData();
    switch (updateType) {
      case ALL -> throw new AppDependencyUpdateRuntimeException("Invalid Update Type: ALL");
      case GITHUB_PULL -> new UpdateGithubPull(appInitData).updateGithubPull();
      case GITHUB_RESET -> new UpdateGithubReset(appInitData).updateGithubReset();
      case GITHUB_MERGE -> new UpdateGithubMerge(appInitData).updateGithubMerge();
      case GRADLE_DEPENDENCIES -> new UpdateGradleDependencies(
              appInitData, mavenRepoService.pluginsMap(), mavenRepoService.dependenciesMap())
          .updateGradleDependencies();
      case NPM_DEPENDENCIES -> new UpdateNpmDependencies(appInitData).updateNpmDependencies();
      case NPM_SNAPSHOT -> new UpdateNpmSnapshots(appInitData, branchName).updateNpmSnapshots();
    }
  }

  private Instant instant(final long amountToAdd, ChronoUnit chronoUnit) {
    return Instant.now().plus(amountToAdd, chronoUnit);
  }
}
