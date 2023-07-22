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

  public UpdateRepoService(
      final AppInitDataService appInitDataService,
      final MavenRepoService mavenRepoService,
      final ScriptFilesService scriptFilesService) {
    this.taskScheduler = new ConcurrentTaskScheduler(Executors.newScheduledThreadPool(30));
    this.appInitDataService = appInitDataService;
    this.mavenRepoService = mavenRepoService;
    this.scriptFilesService = scriptFilesService;
  }

  @Scheduled(cron = "0 0 20 * * *")
  void updateReposScheduler() {
    if (isTaskRunning()) {
      log.info("Something is running, rescheduling 30 minutes from now...");
      taskScheduler.schedule(
          this::updateReposScheduler, Instant.now().plus(30, ChronoUnit.MINUTES));
    } else {
      updateReposScheduler(true, false, null, UpdateType.ALL);
    }
  }

  public boolean isTaskRunning() {
    ScheduledThreadPoolExecutor executor =
        (ScheduledThreadPoolExecutor) taskScheduler.getConcurrentExecutor();
    log.info("Is Task Running: [ {} ] | [ {} ]", executor.getQueue().peek() != null, executor.getActiveCount());
    return executor.getQueue().peek() != null;
  }

  public void updateReposScheduler(
      final boolean isRecreateCaches,
      final boolean isRecreateScriptFiles,
      final String branchName,
      final UpdateType updateType) {
    if (updateType.equals(UpdateType.ALL)) {
      taskScheduler.schedule(
          () -> updateReposAll(isRecreateCaches, isRecreateScriptFiles),
          Instant.now().plusSeconds(3));
    } else {
      taskScheduler.schedule(
          () ->
              updateReposByUpdateType(
                  isRecreateCaches, isRecreateScriptFiles, branchName, updateType),
          Instant.now().plusSeconds(3));
    }
  }

  private void resetAllCaches() {
    appInitDataService.clearAppInitData();
    mavenRepoService.clearPluginsMap();
    mavenRepoService.clearDependenciesMap();
  }

  private void setAllCaches() {
    appInitDataService.appInitData();
    mavenRepoService.pluginsMap();
    mavenRepoService.dependenciesMap();
  }

  private void updateReposAll(final boolean isRecreateCaches, final boolean isRecreateScriptFiles) {
    // clear and set caches as needed
    if (isRecreateCaches) {
      resetAllCaches();
      setAllCaches();
    }

    // delete and create script files as needed
    if (isRecreateScriptFiles || !scriptFilesService.isScriptFilesExistInDirectory()) {
      scriptFilesService.deleteTempScriptFiles();
      scriptFilesService.createTempScriptFiles();
    }

    // pull changes
    executeUpdateRepos(UpdateType.GITHUB_PULL);

    // clear and set caches after pull (gradle version in repo could have changed)
    resetAllCaches();
    mavenRepoService.updateDependenciesInMongo();
    setAllCaches();

    // npm dependencies
    executeUpdateRepos(UpdateType.NPM_DEPENDENCIES);
    // gradle dependencies
    executeUpdateRepos(UpdateType.GRADLE_DEPENDENCIES);
    // merge PRs
    executeUpdateRepos(UpdateType.GITHUB_MERGE);
    // pull changes
    executeUpdateRepos(UpdateType.GITHUB_PULL);
  }

  private void updateReposByUpdateType(
      final boolean isRecreateCaches,
      final boolean isRecreateScriptFiles,
      final String branchName,
      final UpdateType updateType) {

    // clear and set caches as needed
    if (isRecreateCaches) {
      resetAllCaches();
      setAllCaches();
    }

    // delete and create script files as needed
    if (isRecreateScriptFiles || !scriptFilesService.isScriptFilesExistInDirectory()) {
      scriptFilesService.deleteTempScriptFiles();
      scriptFilesService.createTempScriptFiles();
    }

    if (updateType.equals(UpdateType.NPM_SNAPSHOT)) {
      executeUpdateReposNpmSnapshot(branchName);
    } else {
      executeUpdateRepos(updateType);
    }
  }

  private void executeUpdateRepos(final UpdateType updateType) {
    AppInitData appInitData = appInitDataService.appInitData();
    switch (updateType) {
      case ALL, NPM_SNAPSHOT -> throw new AppDependencyUpdateRuntimeException(
          String.format("Invalid Update Type: %s", updateType));
      case GITHUB_PULL -> new UpdateGithubPull(appInitData).updateGithubPull();
      case GITHUB_RESET -> new UpdateGithubReset(appInitData).updateGithubReset();
      case GITHUB_MERGE -> new UpdateGithubMerge(appInitData).updateGithubMerge();
      case GRADLE_DEPENDENCIES -> new UpdateGradleDependencies(
              appInitData, mavenRepoService.pluginsMap(), mavenRepoService.dependenciesMap())
          .updateGradleDependencies();
      case NPM_DEPENDENCIES -> new UpdateNpmDependencies(appInitData).updateNpmDependencies();
    }
  }

  private void executeUpdateReposNpmSnapshot(final String branchName) {
    AppInitData appInitData = appInitDataService.appInitData();
    new UpdateNpmSnapshots(appInitData, branchName);
  }
}
