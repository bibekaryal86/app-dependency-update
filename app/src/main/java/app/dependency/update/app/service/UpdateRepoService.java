package app.dependency.update.app.service;

import static app.dependency.update.app.util.CommonUtils.*;
import static app.dependency.update.app.util.ConstantUtils.BRANCH_UPDATE_DEPENDENCIES;
import static app.dependency.update.app.util.ConstantUtils.ENV_REPO_NAME;
import static app.dependency.update.app.util.ConstantUtils.ENV_SEND_EMAIL;
import static app.dependency.update.app.util.ConstantUtils.PATH_DELIMITER;
import static app.dependency.update.app.util.ProcessUtils.getRepositoriesWithPrError;
import static app.dependency.update.app.util.ProcessUtils.resetProcessedRepositoriesAndSummary;
import static app.dependency.update.app.util.ProcessUtils.updateProcessedRepositoriesRepoType;

import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import app.dependency.update.app.model.AppInitData;
import app.dependency.update.app.model.ProcessSummary;
import app.dependency.update.app.model.ProcessedRepository;
import app.dependency.update.app.model.Repository;
import app.dependency.update.app.model.entities.ProcessSummaries;
import app.dependency.update.app.runnable.*;
import app.dependency.update.app.util.AppInitDataUtils;
import app.dependency.update.app.util.ProcessSummaryEmailUtils;
import app.dependency.update.app.util.ProcessUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
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
      final boolean isProcessSummaryRequired) {
    // reset processed repository map from previous run if anything remaining
    resetProcessedRepositoriesAndSummary();
    if (checkDependenciesUpdate(updateType)) {
      taskScheduler.schedule(
          () ->
              updateReposAllDependencies(
                  updateType, isRecreateCaches, isRecreateScriptFiles, isProcessSummaryRequired),
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

  private void updateReposAllDependencies(
      final UpdateType updateType,
      final boolean isRecreateCaches,
      final boolean isRecreateScriptFiles,
      final boolean isProcessSummaryRequired) {
    log.info(
        "Update Repos All Dependencies: [ {} ] [ {} ] | [ {} ] | [ {} ]",
        updateType,
        isRecreateCaches,
        isRecreateScriptFiles,
        isProcessSummaryRequired);
    // clear and set caches as needed
    if (isRecreateCaches) {
      log.info("Update Repos All Dependencies, Recreating Caches...");
      resetAllCaches();
      setAllCaches();
    }

    // delete and create script files as needed
    if (isRecreateScriptFiles || scriptFilesService.isScriptFilesMissingInFileSystem()) {
      log.info("Update Repos All Dependencies, Recreating Script Files...");
      scriptFilesService.deleteTempScriptFiles();
      scriptFilesService.createTempScriptFiles();
    }

    AppInitData appInitData = AppInitDataUtils.appInitData();

    // checkout main branch
    executeUpdateGithubReset(appInitData);
    // pull changes
    executeUpdateGithubPull(appInitData);

    // clear and set caches after pull (gradle version in repo could have changed)
    log.info("Update Repos All Dependencies, Reset All Caches...");
    resetAllCaches();
    log.info("Update Repos All Dependencies, Update Plugins In Mongo...");
    mongoRepoService.updatePluginsInMongo(mongoRepoService.pluginsMap());
    log.info("Update Repos All Dependencies, Update Dependencies In Mongo...");
    mongoRepoService.updateDependenciesInMongo(mongoRepoService.dependenciesMap());
    log.info("Update Repos All Dependencies, Update Packages In Mongo...");
    mongoRepoService.updatePackagesInMongo(mongoRepoService.packagesMap());
    log.info("Update Repos All Dependencies, Set All Caches...");
    setAllCaches();

    if (updateType == UpdateType.ALL || updateType == UpdateType.NPM_DEPENDENCIES) {
      executeUpdateNpmDependencies(appInitData);
    }

    if (updateType == UpdateType.ALL || updateType == UpdateType.GRADLE_DEPENDENCIES) {
      executeUpdateGradleDependencies(appInitData);
    }

    if (updateType == UpdateType.ALL || updateType == UpdateType.PYTHON_DEPENDENCIES) {
      executeUpdatePythonDependencies(appInitData);
    }

    // wait 5 minutes to complete github PR checks and resume process
    taskScheduler.schedule(
        () -> updateReposAllDependenciesContinue(isProcessSummaryRequired, updateType, appInitData),
        Instant.now().plusSeconds(300));
  }

  private void updateReposAllDependenciesContinue(
      final boolean isProcessSummaryRequired,
      final UpdateType updateType,
      final AppInitData appInitData) {
    log.info(
        "Update Repos All Dependencies Continue: [ {} ] | [ {} ]",
        isProcessSummaryRequired,
        updateType);
    // merge PRs
    executeUpdateGithubMerge(appInitData);
    // pull changes
    executeUpdateGithubPull(appInitData);
    // check github pr create error and execute if needed
    updateReposContinueGithubPrCreateRetry(isProcessSummaryRequired, updateType);
    // send process summary email if applicable
    makeProcessSummary(isProcessSummaryRequired, updateType);
    // this is the final step, clear processed repositories
    resetProcessedRepositoriesAndSummary();
  }

  /**
   * Check for errors when creating GitHub pull requests GitHub has a random limit on pull requests
   * creation at a given time per user For free personal user, it is about 10 pull requests at a
   * given time There is no documentation about this limit, but some GitHub issues do mention this
   * So if the app encounters this limit, retry pr create after 1 hour
   */
  private void updateReposContinueGithubPrCreateRetry(
      final boolean isProcessSummaryRequired, final UpdateType updateType) {
    log.info("Update Repos Continue Github PR Create Retry: [ {} ]", isGithubPrCreateFailed());
    if (isGithubPrCreateFailed()) {
      String branchName = String.format(BRANCH_UPDATE_DEPENDENCIES, LocalDate.now());
      AppInitData appInitData = AppInitDataUtils.appInitData();
      taskScheduler.schedule(
          () -> executeUpdateReposGithubPrCreateRetry(branchName, false, appInitData),
          Instant.now().plus(60, ChronoUnit.MINUTES));
      // wait 5 minutes to complete github PR checks and resume process
      taskScheduler.schedule(
          () ->
              updateReposAllDependenciesContinue(isProcessSummaryRequired, updateType, appInitData),
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

    AppInitData appInitData = AppInitDataUtils.appInitData();

    // ALL, NPM_DEPENDENCIES, GRADLE_DEPENDENCIES, PYTHON_DEPENDENCIES
    // Above 4 types are handled separately and should not reach here
    switch (updateType) {
      case GITHUB_PULL -> executeUpdateGithubPull(appInitData);
      case GITHUB_RESET -> executeUpdateGithubReset(appInitData);
      case GITHUB_MERGE -> executeUpdateGithubMerge(appInitData);
      case NPM_SNAPSHOT -> executeUpdateReposNpmSnapshot(branchName, appInitData);
      case GITHUB_PR_CREATE ->
          executeUpdateReposGithubPrCreateRetry(branchName, isForceCreatePr, appInitData);
      case GITHUB_BRANCH_DELETE ->
          executeUpdateReposGithubBranchDelete(isDeleteUpdateDependenciesOnly, appInitData);
      case GRADLE_SPOTLESS -> executeUpdateReposGradleSpotless(branchName, repoName, appInitData);
      default ->
          throw new AppDependencyUpdateRuntimeException(
              String.format("Invalid Update Type: %s", updateType));
    }

    updateReposContinueGithubPrCreateRetry(false, updateType);
    // reset processed repositories
    resetProcessedRepositoriesAndSummary();
  }

  private void executeUpdateNpmDependencies(final AppInitData appInitData) {
    log.info("Execute Update Node Dependencies...");
    new UpdateNpmDependencies(appInitData, mongoRepoService).updateNpmDependencies();
  }

  private void executeUpdateGradleDependencies(final AppInitData appInitData) {
    log.info("Execute Update Gradle Dependencies...");
    new UpdateGradleDependencies(appInitData, mongoRepoService).updateGradleDependencies();
  }

  private void executeUpdatePythonDependencies(final AppInitData appInitData) {
    log.info("Execute Update Python Dependencies...");
    new UpdatePythonDependencies(appInitData, mongoRepoService).updatePythonDependencies();
  }

  private void executeUpdateGithubPull(final AppInitData appInitData) {
    log.info("Execute Update Github Pull...");
    new UpdateGithubPull(appInitData).updateGithubPull();
  }

  private void executeUpdateGithubReset(final AppInitData appInitData) {
    log.info("Execute Update Github Info...");
    new UpdateGithubReset(appInitData).updateGithubReset();
  }

  private void executeUpdateGithubMerge(final AppInitData appInitData) {
    log.info("Execute Update Github Merge...");
    new UpdateGithubMerge(appInitData).updateGithubMerge();
  }

  private void executeUpdateReposNpmSnapshot(
      final String branchName, final AppInitData appInitData) {
    log.info("Execute Update Repos NPM Snapshot: [ {} ]", branchName);
    new UpdateNpmSnapshots(appInitData, branchName).updateNpmSnapshots();
  }

  private void executeUpdateReposGradleSpotless(
      final String branchName, final String repoName, final AppInitData appInitData) {
    log.info("Execute Update Repos Gradle Spotless: [ {} ] [ {} ]", branchName, repoName);
    new UpdateGradleSpotless(appInitData, branchName, repoName).updateGradleSpotless();
  }

  private void executeUpdateReposGithubBranchDelete(
      final boolean isDeleteUpdateDependenciesOnly, final AppInitData appInitData) {
    log.info("Execute Update Repos GitHub Branch Delete: [ {} ]", isDeleteUpdateDependenciesOnly);
    new UpdateGithubBranchDelete(appInitData, isDeleteUpdateDependenciesOnly)
        .updateGithubBranchDelete();
  }

  private void executeUpdateReposGithubPrCreateRetry(
      final String branchName, final boolean isForceCreatePr, final AppInitData appInitData) {
    log.info(
        "Execute Update Repos Github PR Create Retry: [ {} ] | [ {} ]",
        branchName,
        isForceCreatePr);
    Set<String> beginSet = new HashSet<>(getRepositoriesWithPrError());
    List<Repository> repositories =
        isForceCreatePr
            ? appInitData.getRepositories()
            : appInitData.getRepositories().stream()
                .filter(repository -> beginSet.contains(repository.getRepoName()))
                .toList();
    new UpdateGithubPrCreate(repositories, appInitData, branchName).updateGithubPrCreate();
  }

  private void makeProcessSummary(
      final boolean isProcessSummaryRequired, final UpdateType updateType) {
    boolean isSendEmail =
        "true".equals(AppInitDataUtils.appInitData().getArgsMap().get(ENV_SEND_EMAIL));

    log.info(
        "Make Process Summary: [ {} ] | [ {} ] | [ {} ]",
        isSendEmail,
        isProcessSummaryRequired,
        updateType);

    ProcessSummary processSummary = null;
    if (isProcessSummaryRequired) {
      processSummary = processSummary(updateType);
    }

    if (isSendEmail && processSummary != null) {
      String subject = "App Dependency Update Daily Logs";
      String html = ProcessSummaryEmailUtils.getProcessSummaryContent(processSummary);
      log.debug(html);
      String attachmentFileName = String.format("app_dep_update_logs_%s.log", LocalDate.now());
      String attachment = getLogFileContent();
      emailService.sendEmail(subject, null, html, attachmentFileName, attachment);
    }
  }

  private String getLogFileContent() {
    String logHome =
        AppInitDataUtils.appInitData()
            .getArgsMap()
            .get(ENV_REPO_NAME)
            .concat("/logs/app-dependency-update");
    try {
      Path path = Path.of(logHome + PATH_DELIMITER + "app-dependency-update.log");
      byte[] fileContent = Files.readAllBytes(path);
      return Base64.getEncoder().encodeToString(fileContent);
    } catch (Exception ex) {
      log.error("Get Log File Content Error...", ex);
    }
    return null;
  }

  private ProcessSummary processSummary(final UpdateType updateType) {
    Map<String, ProcessedRepository> processedRepositoryMap =
        ProcessUtils.getProcessedRepositoriesMap();
    List<ProcessedRepository> processedRepositories =
        new ArrayList<>(ProcessUtils.getProcessedRepositoriesMap().values().stream().toList());
    List<Repository> allRepositories = AppInitDataUtils.appInitData().getRepositories();

    for (Repository repository : allRepositories) {
      if (processedRepositoryMap.containsKey(repository.getRepoName())) {
        updateProcessedRepositoriesRepoType(
            repository.getRepoName(), repository.getType().name().split("_")[0]);
      } else {
        processedRepositories.add(
            ProcessedRepository.builder()
                .repoName(repository.getRepoName())
                .repoType(repository.getType().name().split("_")[0])
                .isPrCreated(false)
                .isPrCreateError(false)
                .isPrMerged(false)
                .build());
      }
    }

    processedRepositories.sort(Comparator.comparing(ProcessedRepository::getRepoName));

    ProcessSummary processSummary =
        ProcessSummary.builder()
            .updateType(updateType.name())
            .mongoPluginsToUpdate(ProcessUtils.getMongoPluginsToUpdate())
            .mongoDependenciesToUpdate(ProcessUtils.getMongoDependenciesToUpdate())
            .mongoPackagesToUpdate(ProcessUtils.getMongoPackagesToUpdate())
            .mongoNpmSkipsActive(ProcessUtils.getMongoNpmSkipsActive())
            .totalPrCreatedCount(
                (int)
                    processedRepositories.stream().filter(ProcessedRepository::isPrCreated).count())
            .totalPrCreateErrorsCount(
                (int)
                    processedRepositories.stream()
                        .filter(ProcessedRepository::isPrCreateError)
                        .count())
            .totalPrMergedCount(
                (int)
                    processedRepositories.stream().filter(ProcessedRepository::isPrMerged).count())
            .processedRepositories(processedRepositories)
            .build();

    // save to repository
    ProcessSummaries processSummaries =
        ProcessSummaries.builder().updateDateTime(LocalDateTime.now()).build();
    BeanUtils.copyProperties(processSummary, processSummaries);
    mongoRepoService.saveProcessSummaries(processSummaries);

    return processSummary;
  }
}
