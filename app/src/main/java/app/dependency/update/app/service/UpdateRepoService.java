package app.dependency.update.app.service;

import static app.dependency.update.app.util.CommonUtils.*;
import static app.dependency.update.app.util.ConstantUtils.BRANCH_UPDATE_DEPENDENCIES;
import static app.dependency.update.app.util.ConstantUtils.ENV_SEND_EMAIL_LOG;

import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import app.dependency.update.app.model.AppInitData;
import app.dependency.update.app.model.Repository;
import app.dependency.update.app.runnable.*;
import app.dependency.update.app.util.AppInitDataUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
  private final MongoRepoService mongoRepoService;
  private final ScriptFilesService scriptFilesService;
  private final EmailService emailService;

  public UpdateRepoService(
      final MongoRepoService mongoRepoService,
      final ScriptFilesService scriptFilesService,
      final EmailService emailService) {
    this.taskScheduler = new ConcurrentTaskScheduler(Executors.newScheduledThreadPool(30));
    this.mongoRepoService = mongoRepoService;
    this.scriptFilesService = scriptFilesService;
    this.emailService = emailService;
  }

  @Scheduled(cron = "0 0 20 * * *")
  void updateReposScheduler() {
    if (isTaskRunning()) {
      log.info("Something is running, rescheduling 30 minutes from now...");
      taskScheduler.schedule(
          this::updateReposScheduler, Instant.now().plus(30, ChronoUnit.MINUTES));
    } else {
      log.info("Starting Scheduler to Update Repos...");
      updateRepos(false, false, null, null, UpdateType.ALL, false, true, true);
    }
  }

  public boolean isTaskRunning() {
    ScheduledThreadPoolExecutor executor =
        (ScheduledThreadPoolExecutor) taskScheduler.getConcurrentExecutor();
    log.debug("Is Task Running: [ {} ]", executor.getActiveCount() > 0);
    return executor.getActiveCount() > 0;
  }

  public void updateRepos(
      final boolean isRecreateCaches,
      final boolean isRecreateScriptFiles,
      final String branchName,
      final String repoName,
      final UpdateType updateType,
      final boolean isForceCreatePr,
      final boolean isDeleteUpdateDependenciesOnly,
      final boolean isShouldSendEmail) {
    // reset processed repository map from previous run if anything remaining
    resetProcessedRepositories();
    if (updateType.equals(UpdateType.ALL)) {
      taskScheduler.schedule(
          () -> updateReposAll(isRecreateCaches, isRecreateScriptFiles, isShouldSendEmail),
          Instant.now().plusSeconds(3));
    } else {
      taskScheduler.schedule(
          () ->
              updateReposByUpdateType(
                  isRecreateCaches,
                  isRecreateScriptFiles,
                  branchName,
                  repoName,
                  updateType,
                  isForceCreatePr,
                  isDeleteUpdateDependenciesOnly),
          Instant.now().plusSeconds(3));
    }
  }

  private void resetAllCaches() {
    AppInitDataUtils.clearAppInitData();
    mongoRepoService.clearPluginsMap();
    mongoRepoService.clearDependenciesMap();
    mongoRepoService.clearPackagesMap();
    mongoRepoService.clearNpmSkipsMap();
  }

  private void setAllCaches() {
    AppInitDataUtils.appInitData();
    mongoRepoService.pluginsMap();
    mongoRepoService.dependenciesMap();
    mongoRepoService.packagesMap();
    mongoRepoService.npmSkipsMap();
  }

  private boolean isGithubPrCreateFailed() {
    return !getRepositoriesWithPrError().isEmpty();
  }

  private void updateReposAll(
      final boolean isRecreateCaches,
      final boolean isRecreateScriptFiles,
      final boolean isShouldSendEmail) {
    log.info("Update Repos All [ {} ] | [ {} ]", isRecreateCaches, isRecreateScriptFiles);
    // clear and set caches as needed
    if (isRecreateCaches) {
      log.info("Update Repos All, Recreating Caches...");
      resetAllCaches();
      setAllCaches();
    }

    // delete and create script files as needed
    if (isRecreateScriptFiles || scriptFilesService.isScriptFilesMissingInFileSystem()) {
      log.info("Update Repos All, Recreating Script Files...");
      scriptFilesService.deleteTempScriptFiles();
      scriptFilesService.createTempScriptFiles();
    }

    // pull changes
    executeUpdateRepos(UpdateType.GITHUB_PULL);

    // clear and set caches after pull (gradle version in repo could have changed)
    log.info("Update Repos All, Reset All Caches...");
    resetAllCaches();
    log.info("Update Repos All, Update Plugins In Mongo...");
    mongoRepoService.updatePluginsInMongo(mongoRepoService.pluginsMap());
    log.info("Update Repos All, Update Dependencies In Mongo...");
    mongoRepoService.updateDependenciesInMongo(mongoRepoService.dependenciesMap());
    log.info("Update Repos All, Update Packages In Mongo...");
    mongoRepoService.updatePackagesInMongo(mongoRepoService.packagesMap());
    log.info("Update Repos All, Set All Caches...");
    setAllCaches();

    // npm dependencies
    executeUpdateRepos(UpdateType.NPM_DEPENDENCIES);
    // gradle dependencies
    executeUpdateRepos(UpdateType.GRADLE_DEPENDENCIES);
    // python dependencies
    executeUpdateRepos(UpdateType.PYTHON_DEPENDENCIES);
    // wait 5 minutes to complete github PR checks and resume process
    taskScheduler.schedule(
        () -> updateReposAllContinue(isShouldSendEmail), Instant.now().plusSeconds(300));
  }

  private void updateReposAllContinue(final boolean isShouldSendEmail) {
    log.info("Update Repos All Continue...");
    // merge PRs
    executeUpdateRepos(UpdateType.GITHUB_MERGE);
    // pull changes
    executeUpdateRepos(UpdateType.GITHUB_PULL);
    // check github pr create error and execute if needed
    updateReposContinueGithubPrCreateRetry(isShouldSendEmail);
    // email log file
    boolean isSendEmail =
        "true".equals(AppInitDataUtils.appInitData().getArgsMap().get(ENV_SEND_EMAIL_LOG));
    if (isSendEmail && isShouldSendEmail) {
      log.info("Update Repos All Continue, Sending Email...");
      emailService.sendLogEmail();
    }
    // this is the final step, clear processed repositories
    resetProcessedRepositories();
  }

  /**
   * Check for errors when creating GitHub pull requests GitHub has a random limit on pull requests
   * creation at a given time per user For free personal user, it is about 10 pull requests at a
   * given time There is no documentation about this limit, but some GitHub issues do mention this
   * So if the app encounters this limit, retry pr create after 1 hour
   */
  private void updateReposContinueGithubPrCreateRetry(final boolean isShouldSendEmail) {
    log.info("Update Repos Continue Github PR Create Retry: [ {} ]", isGithubPrCreateFailed());
    if (isGithubPrCreateFailed()) {
      String branchName = String.format(BRANCH_UPDATE_DEPENDENCIES, LocalDate.now());
      taskScheduler.schedule(
          () -> executeUpdateReposGithubPrCreateRetry(branchName, false),
          Instant.now().plus(60, ChronoUnit.MINUTES));
      // wait 5 minutes to complete github PR checks and resume process
      taskScheduler.schedule(
          () -> updateReposAllContinue(isShouldSendEmail),
          Instant.now().plus(66, ChronoUnit.MINUTES));
    }
  }

  private void updateReposByUpdateType(
      final boolean isRecreateCaches,
      final boolean isRecreateScriptFiles,
      final String branchName,
      final String repoName,
      final UpdateType updateType,
      final boolean isForceCreatePr,
      final boolean isDeleteUpdateDependenciesOnly) {
    log.info(
        "Update Repos By Update Type: [ {} ] | [ {} ] | [ {} ] | [ {} ] | [ {} ] | [ {} ] | [ {} ]",
        isRecreateCaches,
        isRecreateScriptFiles,
        branchName,
        repoName,
        updateType,
        isForceCreatePr,
        isDeleteUpdateDependenciesOnly);

    // clear and set caches as needed
    if (isRecreateCaches) {
      resetAllCaches();
      setAllCaches();
    }

    // delete and create script files as needed
    if (isRecreateScriptFiles || scriptFilesService.isScriptFilesMissingInFileSystem()) {
      scriptFilesService.deleteTempScriptFiles();
      scriptFilesService.createTempScriptFiles();
    }

    switch (updateType) {
      case NPM_SNAPSHOT -> executeUpdateReposNpmSnapshot(branchName);
      case GITHUB_PR_CREATE -> executeUpdateReposGithubPrCreateRetry(branchName, isForceCreatePr);
      case GITHUB_BRANCH_DELETE ->
          executeUpdateReposGithubBranchDelete(isDeleteUpdateDependenciesOnly);
      case GRADLE_SPOTLESS -> executeUpdateReposGradleSpotless(branchName, repoName);
      default -> executeUpdateRepos(updateType);
    }

    updateReposContinueGithubPrCreateRetry(false);
    // reset processed repositories
    resetProcessedRepositories();
  }

  private void executeUpdateRepos(final UpdateType updateType) {
    log.info("Execute Update Repos: [ {} ]", updateType);
    AppInitData appInitData = AppInitDataUtils.appInitData();
    switch (updateType) {
      case GITHUB_PULL -> new UpdateGithubPull(appInitData).updateGithubPull();
      case GITHUB_RESET -> new UpdateGithubReset(appInitData).updateGithubReset();
      case GITHUB_MERGE -> new UpdateGithubMerge(appInitData).updateGithubMerge();
      case GRADLE_DEPENDENCIES ->
          new UpdateGradleDependencies(appInitData, mongoRepoService).updateGradleDependencies();
      case NPM_DEPENDENCIES ->
          new UpdateNpmDependencies(appInitData, mongoRepoService).updateNpmDependencies();
      case PYTHON_DEPENDENCIES ->
          new UpdatePythonDependencies(appInitData, mongoRepoService).updatePythonDependencies();
      default ->
          throw new AppDependencyUpdateRuntimeException(
              String.format("Invalid Update Type: %s", updateType));
    }
  }

  private void executeUpdateReposNpmSnapshot(final String branchName) {
    log.info("Execute Update Repos NPM Snapshot: [ {} ]", branchName);
    AppInitData appInitData = AppInitDataUtils.appInitData();
    new UpdateNpmSnapshots(appInitData, branchName).updateNpmSnapshots();
  }

  private void executeUpdateReposGradleSpotless(final String branchName, final String repoName) {
    log.info("Execute Update Repos Gradle Spotless: [ {} ] [ {} ]", branchName, repoName);
    AppInitData appInitData = AppInitDataUtils.appInitData();
    new UpdateGradleSpotless(appInitData, branchName, repoName).updateGradleSpotless();
  }

  private void executeUpdateReposGithubBranchDelete(final boolean isDeleteUpdateDependenciesOnly) {
    log.info("Execute Update Repos GitHub Branch Delete: [ {} ]", isDeleteUpdateDependenciesOnly);
    AppInitData appInitData = AppInitDataUtils.appInitData();
    new UpdateGithubBranchDelete(appInitData, isDeleteUpdateDependenciesOnly)
        .updateGithubBranchDelete();
  }

  private void executeUpdateReposGithubPrCreateRetry(
      final String branchName, final boolean isForceCreatePr) {
    log.info(
        "Execute Update Repos Github PR Create Retry: [ {} ] | [ {} ]",
        branchName,
        isForceCreatePr);
    Set<String> beginSet = new HashSet<>(getRepositoriesWithPrError());
    AppInitData appInitData = AppInitDataUtils.appInitData();
    List<Repository> repositories =
        isForceCreatePr
            ? appInitData.getRepositories()
            : appInitData.getRepositories().stream()
                .filter(repository -> beginSet.contains(repository.getRepoName()))
                .toList();
    new UpdateGithubPrCreate(repositories, appInitData, branchName).updateGithubPrCreate();
  }
}
