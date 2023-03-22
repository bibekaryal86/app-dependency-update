package app.dependency.update.app.execute;

import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import app.dependency.update.app.model.GradleReleaseResponse;
import app.dependency.update.app.model.Repository;
import app.dependency.update.app.util.CommonUtil;
import app.dependency.update.app.util.ConnectorUtil;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GradleWrapperStatus {
  private final List<Repository> gradleRepositories;

  public GradleWrapperStatus(List<Repository> gradleRepositories) {
    this.gradleRepositories = gradleRepositories;
  }

  public List<Repository> getGradleWrapperStatus() {
    return getGradleRepositoriesWithGradleWrapperStatus(getCurrentGradleVersion());
  }

  private String getCurrentGradleVersion() {
    // get rid of draft and prerelease and sort by name descending
    Optional<GradleReleaseResponse> optionalLatestGradleRelease =
        getGradleReleasesResponse().stream()
            .filter(
                gradleReleaseResponse ->
                    !(gradleReleaseResponse.isPrerelease() || gradleReleaseResponse.isDraft()))
            .max(Comparator.comparing(GradleReleaseResponse::getName));

    GradleReleaseResponse latestGradleRelease = optionalLatestGradleRelease.orElse(null);
    log.info("Latest Gradle Release: {}", optionalLatestGradleRelease);

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
      String currentGradleVersion) {
    return this.gradleRepositories.stream()
        .map(
            repository -> {
              String currentGradleVersionInRepo = getCurrentGradleVersionInRepo(repository);
              return new Repository(
                  repository.getRepoPath(),
                  repository.getType(),
                  isGradleWrapperUpdateRequired(currentGradleVersion, currentGradleVersionInRepo)
                      ? currentGradleVersion
                      : null);
            })
        .toList();
  }

  private String getCurrentGradleVersionInRepo(Repository repository) {
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
      log.error("Error reading gradle-wrapper.properties: {}", repository);
    }
    return null;
  }

  private String parseDistributionUrlForGradleVersion(String distributionUrl) {
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

  private boolean isGradleWrapperUpdateRequired(
      String currentGradleVersion, String currentGradleVersionInRepo) {
    return currentGradleVersion.compareTo(currentGradleVersionInRepo) > 0;
  }
}