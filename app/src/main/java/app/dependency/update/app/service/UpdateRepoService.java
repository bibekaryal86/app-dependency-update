package app.dependency.update.app.service;

import static app.dependency.update.app.util.CommonUtils.*;
import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.connector.GradleConnector;
import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import app.dependency.update.app.model.AppInitData;
import app.dependency.update.app.model.GradleReleaseResponse;
import app.dependency.update.app.model.Repository;
import app.dependency.update.app.model.ScriptFile;
import app.dependency.update.app.model.dto.Dependencies;
import app.dependency.update.app.model.dto.Plugins;
import app.dependency.update.app.runnable.ExecuteBuildGradleUpdate;
import app.dependency.update.app.runnable.ExecuteScriptFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UpdateRepoService {

  private final TaskScheduler taskScheduler;
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
    this.taskScheduler = new ConcurrentTaskScheduler(Executors.newScheduledThreadPool(100));
    this.appInitDataService = appInitDataService;
    this.mavenRepoService = mavenRepoService;
    this.gradleConnector = gradleConnector;
    this.scriptFilesService = scriptFilesService;
  }

  @Async
  @Scheduled(cron = "0 0 20 * * *")
  public void updateRepos() {
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
          mavenRepoService::dependenciesMap, instant(SCHED_BEGIN, ChronoUnit.MINUTES));
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
    setPseudoSemaphore(1);
    taskScheduler.schedule(
        scriptFilesService::deleteTempScriptFilesBegin,
        instant(SCHED_BEGIN + 1, ChronoUnit.MINUTES));
    taskScheduler.schedule(
        scriptFilesService::createTempScriptFiles, instant(SCHED_BEGIN + 2, ChronoUnit.MINUTES));
    if (isWrapperMerge) {
      taskScheduler.schedule(
          () -> updateRepos(updateType, true, null), instant(SCHED_BEGIN + 3, ChronoUnit.MINUTES));
    } else {
      taskScheduler.schedule(
          () -> updateRepos(updateType, false, null), instant(SCHED_BEGIN + 3, ChronoUnit.MINUTES));
    }
    taskScheduler.schedule(
        scriptFilesService::deleteTempScriptFilesEnd,
        instant(SCHED_BEGIN + 10, ChronoUnit.MINUTES));
  }

  @Async
  public void updateRepos(final String branchName) {
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
    ScriptFile scriptFile = getScriptFile(updateType, appInitData.getScriptFiles());
    List<Repository> repositories = getRepositories(updateType, appInitData.getRepositories());
    List<String> arguments = new LinkedList<>();
    switch (updateType) {
      case ALL -> throw new AppDependencyUpdateRuntimeException("Invalid Update Type: ALL");
      case GITHUB_PULL, GITHUB_RESET -> repositories.forEach(
          repository -> {
            arguments.clear();
            arguments.add(repository.getRepoPath().toString());
            updateRepo(repository, scriptFile, arguments);
          });
      case GITHUB_MERGE -> repositories.forEach(
          repository -> {
            arguments.clear();
            arguments.add(repository.getRepoPath().toString());
            arguments.add(
                isWrapperMerge
                    ? String.format(BRANCH_UPDATE_WRAPPER, LocalDate.now())
                    : String.format(BRANCH_UPDATE_DEPENDENCIES, LocalDate.now()));
            updateRepo(repository, scriptFile, arguments);
          });
      case GRADLE_WRAPPER -> repositories.forEach(
          repository -> {
            arguments.clear();
            arguments.add(repository.getRepoPath().toString());
            arguments.add(String.format(BRANCH_UPDATE_WRAPPER, LocalDate.now()));
            arguments.add(repository.getGradleVersion());
            updateRepo(repository, scriptFile, arguments);
          });
      case GRADLE_DEPENDENCIES, NPM_DEPENDENCIES -> repositories.forEach(
          repository -> {
            arguments.clear();
            arguments.add(repository.getRepoPath().toString());
            arguments.add(String.format(BRANCH_UPDATE_DEPENDENCIES, LocalDate.now()));
            updateRepo(repository, scriptFile, arguments);
          });
      case NPM_SNAPSHOT -> repositories.forEach(
          repository -> {
            arguments.clear();
            arguments.add(repository.getRepoPath().toString());
            arguments.add(branchName);
            updateRepo(repository, scriptFile, arguments);
          });
    }
  }

  private Instant instant(final long amountToAdd, ChronoUnit chronoUnit) {
    return Instant.now().plus(amountToAdd, chronoUnit);
  }

  private ScriptFile getScriptFile(final UpdateType type, final List<ScriptFile> scriptFiles) {
    Optional<ScriptFile> optionalScriptFile =
        scriptFiles.stream().filter(scriptFile -> type.equals(scriptFile.getType())).findFirst();
    if (optionalScriptFile.isEmpty()) {
      throw new AppDependencyUpdateRuntimeException(
          String.format("Script Not Found: [ %s ]", type));
    }
    return optionalScriptFile.get();
  }

  private List<Repository> getRepositories(
      final UpdateType updateType, final List<Repository> repositories) {
    if (updateType.equals(UpdateType.GITHUB_PULL)
        || updateType.equals(UpdateType.GITHUB_MERGE)
        || updateType.equals(UpdateType.GITHUB_RESET)) {
      return repositories;
    }

    if (updateType.equals(UpdateType.GRADLE_WRAPPER)) {
      return getGradleRepositoriesWithGradleWrapperStatus(repositories, getLatestGradleVersion());
    } else if (updateType.equals(UpdateType.NPM_SNAPSHOT)) {
      return repositories.stream()
          .filter(repository -> UpdateType.NPM_DEPENDENCIES.equals(repository.getType()))
          .toList();
    }

    return repositories.stream()
        .filter(repository -> updateType.equals(repository.getType()))
        .toList();
  }

  private void updateRepo(
      final Repository repository, final ScriptFile scriptFile, List<String> arguments) {
    log.info("Update Repo: [ {} ] [ {} ] [ {} ]", repository, scriptFile, arguments);

    if (scriptFile.getType().equals(UpdateType.GRADLE_DEPENDENCIES)) {
      Map<String, Plugins> pluginsMap = mavenRepoService.pluginsMap();
      Map<String, Dependencies> dependenciesMap = mavenRepoService.dependenciesMap();
      new ExecuteBuildGradleUpdate(repository, scriptFile, arguments, pluginsMap, dependenciesMap)
          .start();
    } else {
      new ExecuteScriptFile(threadName(repository, scriptFile), scriptFile, arguments).start();
    }
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

  private List<Repository> getGradleRepositoriesWithGradleWrapperStatus(
      final List<Repository> repositories, final String latestVersion) {
    return repositories.stream()
        .filter(repository -> repository.getType().equals(UpdateType.GRADLE_DEPENDENCIES))
        .map(
            repository -> {
              String currentVersion = getCurrentGradleVersionInRepo(repository);
              boolean isRequiresUpdate = isRequiresUpdate(currentVersion, latestVersion);
              if (isRequiresUpdate) {
                return new Repository(
                    repository.getRepoPath(),
                    repository.getType(),
                    repository.getGradleModules(),
                    latestVersion);
              }
              return null;
            })
        .filter(Objects::nonNull)
        .toList();
  }

  private String getCurrentGradleVersionInRepo(final Repository repository) {
    Path wrapperPath =
        Path.of(repository.getRepoPath().toString().concat(GRADLE_WRAPPER_PROPERTIES));
    try {
      List<String> allLines = Files.readAllLines(wrapperPath);
      String distributionUrl =
          allLines.stream()
              .filter(line -> line.startsWith("distributionUrl"))
              .findFirst()
              .orElse(null);

      if (distributionUrl != null) {
        return parseDistributionUrlForGradleVersion(distributionUrl);
      }
    } catch (IOException e) {
      log.error("Error reading gradle-wrapper.properties: [ {} ]", repository);
    }
    return null;
  }

  private String parseDistributionUrlForGradleVersion(final String distributionUrl) {
    // matches text between two hyphens
    // eg: distributionUrl=https\://services.gradle.org/distributions/gradle-8.0-bin.zip
    Pattern pattern = Pattern.compile(GRADLE_WRAPPER_REGEX);
    Matcher matcher = pattern.matcher(distributionUrl);
    if (matcher.find()) {
      return matcher.group();
    } else {
      return null;
    }
  }
}
