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
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UpdateRepoService {

  private final AppInitDataService appInitDataService;
  private final MavenRepoService mavenRepoService;
  private final GradleConnector gradleConnector;

  public UpdateRepoService(
      final AppInitDataService appInitDataService,
      final MavenRepoService mavenRepoService,
      final GradleConnector gradleConnector) {
    this.appInitDataService = appInitDataService;
    this.mavenRepoService = mavenRepoService;
    this.gradleConnector = gradleConnector;
  }

  public void updateRepos(final UpdateType updateType) {
    updateRepos(updateType, false, null);
  }

  public void updateRepos(final UpdateType updateType, final boolean isWrapperMerge) {
    updateRepos(updateType, isWrapperMerge, null);
  }

  public void updateRepos(final UpdateType updateType, final String branchName) {
    updateRepos(updateType, false, branchName);
  }

  private void updateRepos(
      final UpdateType updateType, final boolean isWrapperMerge, final String branchName) {
    AppInitData appInitData = appInitDataService.appInitData();
    ScriptFile scriptFile = getScriptFile(updateType, appInitData.getScriptFiles());
    List<Repository> repositories = getRepositories(updateType, appInitData.getRepositories());
    List<String> arguments = new LinkedList<>();
    switch (updateType) {
      case ALL -> throw new AppDependencyUpdateRuntimeException("Invalid Update Type: ALL");
      case GITHUB_PULL -> repositories.forEach(
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
    if (updateType.equals(UpdateType.GITHUB_PULL) || updateType.equals(UpdateType.GITHUB_MERGE)) {
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
