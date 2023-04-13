package app.dependency.update.app.update;

import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import app.dependency.update.app.helper.ExecuteScriptFile;
import app.dependency.update.app.model.GradleReleaseResponse;
import app.dependency.update.app.model.Repository;
import app.dependency.update.app.model.ScriptFile;
import app.dependency.update.app.util.CommonUtil;
import app.dependency.update.app.util.ConnectorUtil;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdateGradleWrapper {

  private final List<Repository> repositories;
  private final ScriptFile scriptFile;

  public UpdateGradleWrapper(final List<Repository> repositories, final ScriptFile scriptFile) {
    this.repositories = repositories;
    this.scriptFile = scriptFile;
  }

  public void updateGradleWrapper() {
    List<Repository> gradleRepositories =
        getGradleRepositoriesWithGradleWrapperStatus(getLatestGradleVersion());
    log.info("Gradle Repositories with Gradle Wrapper Status: [ {} ]", gradleRepositories);

    gradleRepositories.forEach(this::executeUpdate);
  }

  private String getLatestGradleVersion() {
    // get rid of draft and prerelease and sort by name descending
    Optional<GradleReleaseResponse> optionalLatestGradleRelease =
        getGradleReleasesResponse().stream()
            .filter(
                gradleReleaseResponse ->
                    !(gradleReleaseResponse.isPrerelease() || gradleReleaseResponse.isDraft()))
            .max(Comparator.comparing(GradleReleaseResponse::getName));

    GradleReleaseResponse latestGradleRelease = optionalLatestGradleRelease.orElse(null);
    log.info("Latest Gradle Release: [ {} ]", optionalLatestGradleRelease);

    if (latestGradleRelease == null) {
      throw new AppDependencyUpdateRuntimeException("Latest Gradle Release Retrieve Error...");
    }

    return latestGradleRelease.getName();
  }

  @SuppressWarnings("unchecked")
  private List<GradleReleaseResponse> getGradleReleasesResponse() {
    return (List<GradleReleaseResponse>)
        ConnectorUtil.sendHttpRequest(
            CommonUtil.GRADLE_RELEASES_ENDPOINT,
            CommonUtil.HttpMethod.GET,
            null,
            null,
            new TypeToken<Collection<GradleReleaseResponse>>() {}.getType(),
            null);
  }

  private List<Repository> getGradleRepositoriesWithGradleWrapperStatus(
      final String latestVersion) {
    return this.repositories.stream()
        .map(
            repository -> {
              String currentVersion = getCurrentGradleVersionInRepo(repository);
              if (CommonUtil.isRequiresUpdate(currentVersion, latestVersion)) {
                return new Repository(
                    repository.getRepoPath(),
                    repository.getType(),
                    repository.getGradleModules(),
                    CommonUtil.isRequiresUpdate(currentVersion, latestVersion)
                        ? latestVersion
                        : null);
              }
              return null;
            })
        .filter(Objects::nonNull)
        .toList();
  }

  private String getCurrentGradleVersionInRepo(final Repository repository) {
    Path wrapperPath =
        Path.of(
            repository
                .getRepoPath()
                .toString()
                .concat(CommonUtil.PATH_DELIMITER)
                .concat(CommonUtil.GRADLE)
                .concat(CommonUtil.PATH_DELIMITER)
                .concat(CommonUtil.WRAPPER)
                .concat(CommonUtil.PATH_DELIMITER)
                .concat(CommonUtil.GRADLE_WRAPPER_PROPERTIES));
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
    Pattern pattern = Pattern.compile(CommonUtil.GRADLE_WRAPPER_REGEX);
    Matcher matcher = pattern.matcher(distributionUrl);
    if (matcher.find()) {
      return matcher.group();
    } else {
      return null;
    }
  }

  private void executeUpdate(final Repository repository) {
    log.info("Execute Gradle Update on: [ {} ]", repository);
    List<String> arguments = new LinkedList<>();
    arguments.add(repository.getRepoPath().toString());
    arguments.add(String.format(CommonUtil.BRANCH_UPDATE_WRAPPER, LocalDate.now()));
    arguments.add(repository.getGradleVersion());
    new ExecuteScriptFile(
            repository.getRepoName() + this.getClass().getSimpleName(), this.scriptFile, arguments)
        .start();
  }
}
