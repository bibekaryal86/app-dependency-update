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
  private final MavenRepoService mavenRepoService;
  private final ScriptFilesService scriptFilesService;
  private final EmailService emailService;

  public UpdateRepoService(
      final MavenRepoService mavenRepoService,
      final ScriptFilesService scriptFilesService,
      final EmailService emailService) {
    this.taskScheduler = new ConcurrentTaskScheduler(Executors.newScheduledThreadPool(30));
    this.mavenRepoService = mavenRepoService;
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
      updateReposScheduler(false, false, null, UpdateType.ALL, false, true);
    }
  }

  public boolean isTaskRunning() {
    ScheduledThreadPoolExecutor executor =
        (ScheduledThreadPoolExecutor) taskScheduler.getConcurrentExecutor();
    log.debug("Is Task Running: [ {} ]", executor.getActiveCount() > 0);
    return executor.getActiveCount() > 0;
  }

  public void updateReposScheduler(
      final boolean isRecreateCaches,
      final boolean isRecreateScriptFiles,
      final String branchName,
      final UpdateType updateType,
      final boolean isForceCreatePr,
      final boolean isDeleteUpdateDependenciesOnly) {
    if (updateType.equals(UpdateType.ALL)) {
      taskScheduler.schedule(
          () -> updateReposAll(isRecreateCaches, isRecreateScriptFiles),
          Instant.now().plusSeconds(3));
    } else {
      taskScheduler.schedule(
          () ->
              updateReposByUpdateType(
                  isRecreateCaches,
                  isRecreateScriptFiles,
                  branchName,
                  updateType,
                  isForceCreatePr,
                  isDeleteUpdateDependenciesOnly),
          Instant.now().plusSeconds(3));
    }
  }

  private void resetAllCaches() {
    AppInitDataUtils.clearAppInitData();
    mavenRepoService.clearPluginsMap();
    mavenRepoService.clearDependenciesMap();
  }

  private void setAllCaches() {
    AppInitDataUtils.appInitData();
    mavenRepoService.pluginsMap();
    mavenRepoService.dependenciesMap();
  }

  private boolean isGithubPrCreateFailed() {
    return !getRepositoriesWithPrError().isEmpty();
  }

  private void updateReposAll(final boolean isRecreateCaches, final boolean isRecreateScriptFiles) {
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
    mavenRepoService.updatePluginsInMongo();
    log.info("Update Repos All, Update Dependencies In Mongo...");
    mavenRepoService.updateDependenciesInMongo();
    log.info("Update Repos All, Set All Caches...");
    setAllCaches();

    // npm dependencies
    executeUpdateRepos(UpdateType.NPM_DEPENDENCIES);
    // gradle dependencies
    executeUpdateRepos(UpdateType.GRADLE_DEPENDENCIES);
    // wait 5 minutes to complete github PR checks and resume process
    taskScheduler.schedule(this::updateReposAllContinue, Instant.now().plusSeconds(300));
  }

  private void updateReposAllContinue() {
    log.info("Update Repos All Continue...");
    // merge PRs
    executeUpdateRepos(UpdateType.GITHUB_MERGE);
    // pull changes
    executeUpdateRepos(UpdateType.GITHUB_PULL);
    // check github pr create error and execute if needed
    updateReposContinueGithubPrCreateRetry();
    // email log file
    boolean isSendEmail =
        "true".equals(AppInitDataUtils.appInitData().getArgsMap().get(ENV_SEND_EMAIL_LOG));
    if (isSendEmail) {
      log.info("Update Repos All Continue, Sending Email...");
      emailService.sendLogEmail();
    }
  }

  /**
   * Check for errors when creating GitHub pull requests GitHub has a random limit on pull requests
   * creation at a given time per user For free personal user, it is about 10 pull requests at a
   * given time There is no documentation about this limit, but some GitHub issues do mention this
   * So if the app encounters this limit, retry pr create after 1 hour
   */
  private void updateReposContinueGithubPrCreateRetry() {
    log.info("Update Repos Continue Github PR Create Retry: [ {} ]", isGithubPrCreateFailed());
    if (isGithubPrCreateFailed()) {
      String branchName = String.format(BRANCH_UPDATE_DEPENDENCIES, LocalDate.now());
      taskScheduler.schedule(
          () -> executeUpdateReposGithubPrCreateRetry(branchName, false),
          Instant.now().plus(60, ChronoUnit.MINUTES));
      // wait 5 minutes to complete github PR checks and resume process
      taskScheduler.schedule(
          this::updateReposAllContinue, Instant.now().plus(66, ChronoUnit.MINUTES));
    }
  }

  private void updateReposByUpdateType(
      final boolean isRecreateCaches,
      final boolean isRecreateScriptFiles,
      final String branchName,
      final UpdateType updateType,
      final boolean isForceCreatePr,
      final boolean isDeleteUpdateDependenciesOnly) {
    log.info(
        "Update Repos By Update Type: [ {} ] | [ {} ] | [ {} ] | [ {} ] [ {} ] [ {} ]",
        isRecreateCaches,
        isRecreateScriptFiles,
        branchName,
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
      case GITHUB_BRANCH_DELETE -> executeUpdateReposGithubBranchDelete(
          isDeleteUpdateDependenciesOnly);
      default -> executeUpdateRepos(updateType);
    }

    updateReposContinueGithubPrCreateRetry();
  }

  private void executeUpdateRepos(final UpdateType updateType) {
    log.info("Execute Update Repos: [ {} ]", updateType);
    AppInitData appInitData = AppInitDataUtils.appInitData();
    switch (updateType) {
      case GITHUB_PULL -> new UpdateGithubPull(appInitData).updateGithubPull();
      case GITHUB_RESET -> new UpdateGithubReset(appInitData).updateGithubReset();
      case GITHUB_MERGE -> new UpdateGithubMerge(appInitData).updateGithubMerge();
      case GRADLE_DEPENDENCIES -> new UpdateGradleDependencies(appInitData, mavenRepoService)
          .updateGradleDependencies();
      case NPM_DEPENDENCIES -> new UpdateNpmDependencies(appInitData).updateNpmDependencies();
      default -> throw new AppDependencyUpdateRuntimeException(
          String.format("Invalid Update Type: %s", updateType));
    }
  }

  private void executeUpdateReposNpmSnapshot(final String branchName) {
    log.info("Execute Update Repos NPM Snapshot: [ {} ]", branchName);
    AppInitData appInitData = AppInitDataUtils.appInitData();
    new UpdateNpmSnapshots(appInitData, branchName).updateNpmSnapshots();
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
