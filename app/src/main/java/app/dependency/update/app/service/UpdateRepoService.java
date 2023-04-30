package app.dependency.update.app.service;

import static app.dependency.update.app.util.CommonUtils.*;

import app.dependency.update.app.connector.GradleConnector;
import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import app.dependency.update.app.model.AppInitData;
import app.dependency.update.app.model.GradleReleaseResponse;
import app.dependency.update.app.runnable.UpdateGithubMerge;
import app.dependency.update.app.runnable.UpdateGithubPull;
import app.dependency.update.app.runnable.UpdateGithubReset;
import app.dependency.update.app.runnable.UpdateGradleDependencies;
import app.dependency.update.app.runnable.UpdateGradleWrapper;
import app.dependency.update.app.runnable.UpdateNpmDependencies;
import app.dependency.update.app.runnable.UpdateNpmSnapshots;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UpdateRepoService {

  private final AppInitDataService appInitDataService;
  private final MavenRepoService mavenRepoService;
  private final GradleConnector gradleConnector;
  private final ScriptFilesService scriptFilesService;

  private static final Integer SCHED_BEGIN = 1;

  public UpdateRepoService(
      final AppInitDataService appInitDataService,
      final MavenRepoService mavenRepoService,
      final GradleConnector gradleConnector,
      final ScriptFilesService scriptFilesService) {
    this.appInitDataService = appInitDataService;
    this.mavenRepoService = mavenRepoService;
    this.gradleConnector = gradleConnector;
    this.scriptFilesService = scriptFilesService;
  }

  @Async
  @Scheduled(cron = "0 0 20 * * *")
  public void updateRepos() {
    TaskScheduler taskScheduler = new ConcurrentTaskScheduler(Executors.newScheduledThreadPool(25));
    if (getPseudoSemaphore() > 0) {
      log.warn("Something is already running, trying again in 60 minutes...");
      taskScheduler.schedule(this::updateRepos, instant(SCHED_BEGIN + 60, ChronoUnit.MINUTES));
    } else {
      setPseudoSemaphore(1);
      // clear caches
      taskScheduler.schedule(
          () -> {
            appInitDataService.clearAppInitData();
            mavenRepoService.clearPluginsMap();
            mavenRepoService.clearDependenciesMap();
          },
          instant(SCHED_BEGIN, ChronoUnit.SECONDS));

      taskScheduler.schedule(
          appInitDataService::appInitData, instant(SCHED_BEGIN, ChronoUnit.MINUTES));
      taskScheduler.schedule(
          mavenRepoService::pluginsMap, instant(SCHED_BEGIN, ChronoUnit.MINUTES));
      taskScheduler.schedule(
          mavenRepoService::updateDependenciesInMongo, instant(SCHED_BEGIN, ChronoUnit.MINUTES));
      taskScheduler.schedule(
          scriptFilesService::deleteTempScriptFilesBegin,
          instant(SCHED_BEGIN + 1, ChronoUnit.MINUTES));
      taskScheduler.schedule(
          scriptFilesService::createTempScriptFiles, instant(SCHED_BEGIN + 2, ChronoUnit.MINUTES));
      taskScheduler.schedule(
          () -> updateRepos(UpdateType.GITHUB_PULL, false, null),
          instant(SCHED_BEGIN + 3, ChronoUnit.MINUTES));
      taskScheduler.schedule(
          () -> updateRepos(UpdateType.GRADLE_WRAPPER, false, null),
          instant(SCHED_BEGIN + 4, ChronoUnit.MINUTES));
      taskScheduler.schedule(
          () -> updateRepos(UpdateType.GITHUB_MERGE, true, null),
          instant(SCHED_BEGIN + 13, ChronoUnit.MINUTES));
      taskScheduler.schedule(
          () -> updateRepos(UpdateType.GITHUB_PULL, false, null),
          instant(SCHED_BEGIN + 16, ChronoUnit.MINUTES));
      taskScheduler.schedule(
          () -> updateRepos(UpdateType.NPM_DEPENDENCIES, false, null),
          instant(SCHED_BEGIN + 19, ChronoUnit.MINUTES));
      taskScheduler.schedule(
          () -> updateRepos(UpdateType.GRADLE_DEPENDENCIES, false, null),
          instant(SCHED_BEGIN + 23, ChronoUnit.MINUTES));
      taskScheduler.schedule(
          () -> updateRepos(UpdateType.GITHUB_MERGE, false, null),
          instant(SCHED_BEGIN + 33, ChronoUnit.MINUTES));
      taskScheduler.schedule(
          () -> updateRepos(UpdateType.GITHUB_PULL, false, null),
          instant(SCHED_BEGIN + 36, ChronoUnit.MINUTES));
      taskScheduler.schedule(
          scriptFilesService::deleteTempScriptFilesEnd,
          instant(SCHED_BEGIN + 39, ChronoUnit.MINUTES));
    }
  }

  @Async
  public void updateRepos(final UpdateType updateType, final boolean isWrapperMerge) {
    TaskScheduler taskScheduler = new ConcurrentTaskScheduler(Executors.newScheduledThreadPool(25));
    setPseudoSemaphore(1);
    taskScheduler.schedule(
        scriptFilesService::deleteTempScriptFilesBegin,
        instant(SCHED_BEGIN + 1, ChronoUnit.MINUTES));
    taskScheduler.schedule(
        scriptFilesService::createTempScriptFiles, instant(SCHED_BEGIN + 2, ChronoUnit.MINUTES));
    taskScheduler.schedule(
        () -> updateRepos(updateType, isWrapperMerge, null),
        instant(SCHED_BEGIN + 3, ChronoUnit.MINUTES));
    taskScheduler.schedule(
        scriptFilesService::deleteTempScriptFilesEnd,
        instant(SCHED_BEGIN + 10, ChronoUnit.MINUTES));
  }

  @Async
  public void updateRepos(final String branchName) {
    TaskScheduler taskScheduler = new ConcurrentTaskScheduler(Executors.newScheduledThreadPool(25));
    setPseudoSemaphore(1);
    taskScheduler.schedule(
        scriptFilesService::deleteTempScriptFilesBegin, instant(SCHED_BEGIN, ChronoUnit.MINUTES));
    taskScheduler.schedule(
        scriptFilesService::createTempScriptFiles, instant(SCHED_BEGIN + 1, ChronoUnit.MINUTES));
    taskScheduler.schedule(
        () -> updateRepos(UpdateType.GITHUB_PULL, false, null),
        instant(SCHED_BEGIN + 2, ChronoUnit.MINUTES));
    taskScheduler.schedule(
        () -> updateRepos(UpdateType.NPM_SNAPSHOT, false, branchName),
        instant(3, ChronoUnit.MINUTES));
    taskScheduler.schedule(
        () -> updateRepos(UpdateType.GITHUB_MERGE, false, null),
        instant(SCHED_BEGIN + 13, ChronoUnit.MINUTES));
    taskScheduler.schedule(
        () -> updateRepos(UpdateType.GITHUB_PULL, false, null),
        instant(SCHED_BEGIN + 17, ChronoUnit.MINUTES));
    taskScheduler.schedule(
        scriptFilesService::deleteTempScriptFilesEnd,
        instant(SCHED_BEGIN + 20, ChronoUnit.SECONDS));
  }

  private void updateRepos(
      final UpdateType updateType, final boolean isWrapperMerge, final String branchName) {
    AppInitData appInitData = appInitDataService.appInitData();
    switch (updateType) {
      case ALL -> throw new AppDependencyUpdateRuntimeException("Invalid Update Type: ALL");
      case GITHUB_PULL -> new UpdateGithubPull(appInitData).updateGithubPull();
      case GITHUB_RESET -> new UpdateGithubReset(appInitData).updateGithubReset();
      case GITHUB_MERGE -> new UpdateGithubMerge(appInitData, isWrapperMerge).updateGithubMerge();
      case GRADLE_WRAPPER -> new UpdateGradleWrapper(appInitData, getLatestGradleVersion())
          .updateGradleWrapper();
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

  private String getLatestGradleVersion() {
    List<GradleReleaseResponse> gradleReleaseResponses = gradleConnector.getGradleReleases();
    // get rid of draft and prerelease and sort by name descending
    Optional<GradleReleaseResponse> optionalLatestGradleRelease =
        gradleReleaseResponses.stream()
            .filter(
                gradleReleaseResponse ->
                    !(gradleReleaseResponse.isPrerelease() || gradleReleaseResponse.isDraft()))
            .max(Comparator.comparing(GradleReleaseResponse::getName));

    GradleReleaseResponse latestGradleRelease = optionalLatestGradleRelease.orElse(null);
    log.info("Latest Gradle Release: [ {} ]", optionalLatestGradleRelease);

    if (latestGradleRelease == null) {
      throw new AppDependencyUpdateRuntimeException("Latest Gradle Release Null Error...");
    }

    return latestGradleRelease.getName();
  }
}
