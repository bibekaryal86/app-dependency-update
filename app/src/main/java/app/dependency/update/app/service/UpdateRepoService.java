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
import app.dependency.update.app.runnable.*;
import app.dependency.update.app.util.AppInitDataUtils;
import app.dependency.update.app.util.ProcessUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
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
    if (updateType.equals(UpdateType.ALL)) {
      taskScheduler.schedule(
          () -> updateReposAll(isRecreateCaches, isRecreateScriptFiles, isProcessSummaryRequired),
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
      final boolean isProcessSummaryRequired) {
    log.info(
        "Update Repos All [ {} ] | [ {} ] | [ {} ]",
        isRecreateCaches,
        isRecreateScriptFiles,
        isProcessSummaryRequired);
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
        () -> updateReposAllContinue(isProcessSummaryRequired, UpdateType.ALL),
        Instant.now().plusSeconds(300));
  }

  private void updateReposAllContinue(
      final boolean isProcessSummaryRequired, final UpdateType updateType) {
    log.info("Update Repos All Continue...");
    // merge PRs
    executeUpdateRepos(UpdateType.GITHUB_MERGE);
    // pull changes
    executeUpdateRepos(UpdateType.GITHUB_PULL);
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
      taskScheduler.schedule(
          () -> executeUpdateReposGithubPrCreateRetry(branchName, false),
          Instant.now().plus(60, ChronoUnit.MINUTES));
      // wait 5 minutes to complete github PR checks and resume process
      taskScheduler.schedule(
          () -> updateReposAllContinue(isProcessSummaryRequired, updateType),
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

    updateReposContinueGithubPrCreateRetry(false, updateType);
    // reset processed repositories
    resetProcessedRepositoriesAndSummary();
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
      String html = getProcessSummaryContent(processSummary);
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
            .updateType(updateType)
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

    // TODO save to database
    return processSummary;
  }

  private String getProcessSummaryContent(ProcessSummary processSummary) {
    StringBuilder html = new StringBuilder();
    html.append(
        """
            <html>
              <head>
                <style>
                  th {
                      border-bottom: 2px solid #9e9e9e;
                      position: sticky;
                      top: 0;
                      background-color: lightgrey;
                    }
                  td {
                    padding: 5px;
                    text-align: left;
                    border-bottom: 1px solid #9e9e9e;
                  }
                  td:first-child {
                     text-align: left;
                   }
                   td:not(:first-child) {
                     text-align: center;
                   }
                </style>
              </head>
              <body>
            """);

    html.append(
        """
          <p style='font-size: 14px; font-weight: bold;'>App Dependency Update Process Summary: %s</p>
          <table cellpadding='10' cellspacing='0' style='font-size: 12px; border-collapse: collapse;'>
            <tr>
              <th>Item</th>
              <th>Value</th>
            </tr>
            <tr>
              <td>Mongo Plugins To Update</td>
              <td>%d</td>
            </tr>
            <tr>
              <td>Mongo Dependencies To Update</td>
              <td>%d</td>
            </tr>
            <tr>
              <td>Mongo Packages To Update</td>
              <td>%d</td>
            </tr>
            <tr>
              <td>Mongo NPM Skips Active</td>
              <td>%d</td>
            </tr>
            <tr>
              <td>Total PR Created Count</td>
              <td>%d</td>
            </tr>
            <tr>
              <td>Total PR Create Errors Count</td>
              <td>%d</td>
            </tr>
            <tr>
              <td>Total PR Merged Count</td>
              <td>%d</td>
            </tr>
          </table>
        """
            .formatted(
                processSummary.getUpdateType().name(),
                processSummary.getMongoPluginsToUpdate(),
                processSummary.getMongoDependenciesToUpdate(),
                processSummary.getMongoPackagesToUpdate(),
                processSummary.getMongoNpmSkipsActive(),
                processSummary.getTotalPrCreatedCount(),
                processSummary.getTotalPrCreateErrorsCount(),
                processSummary.getTotalPrMergedCount()));

    html.append(
        """
          <br />
          <p style='font-size: 14px; font-weight: bold;'>Processed Repositories</p>
          <table border='1' cellpadding='10' cellspacing='0' style='border-collapse: collapse; width: 100%;'>
            <tr>
              <th>Repository</th>
              <th>PR Created</th>
              <th>PR Create Error</th>
              <th>PR Merged</th>
            </tr>
        """);

    for (ProcessedRepository processedRepository : processSummary.getProcessedRepositories()) {
      html.append("<tr>");
      html.append("<td>").append(processedRepository.getRepoName()).append("</td>");
      html.append("<td>").append(processedRepository.getRepoType()).append("</td>");
      html.append("<td>").append(processedRepository.isPrCreated() ? "Y" : "N").append("</td>");
      html.append("<td>").append(processedRepository.isPrCreateError() ? "Y" : "N").append("</td>");
      html.append("<td>").append(processedRepository.isPrMerged() ? "Y" : "N").append("</td>");
      html.append("</tr>");
    }

    html.append("""
          </table>
          </body>
        </html>
        """);

    return html.toString();
  }
}
