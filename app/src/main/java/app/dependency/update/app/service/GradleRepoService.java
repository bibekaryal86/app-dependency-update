package app.dependency.update.app.service;

import static app.dependency.update.app.util.CommonUtils.isCheckPreReleaseVersion;
import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.connector.GradleConnector;
import app.dependency.update.app.model.GradleReleaseResponse;
import app.dependency.update.app.model.LatestVersion;
import app.dependency.update.app.util.ProcessUtils;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GradleRepoService {

  private final GradleConnector gradleConnector;

  public GradleRepoService(GradleConnector gradleConnector) {
    this.gradleConnector = gradleConnector;
  }

  public LatestVersion getLatestGradleVersion(final String latestJavaVersionMajor) {
    List<GradleReleaseResponse> gradleReleaseResponses = gradleConnector.getGradleReleases();
    Optional<GradleReleaseResponse> optionalLatestGradleRelease =
        gradleReleaseResponses.stream()
            .filter(
                gradleReleaseResponse ->
                    !(gradleReleaseResponse.isPrerelease() || gradleReleaseResponse.isDraft()))
            .findFirst();
    GradleReleaseResponse latestGradleRelease = optionalLatestGradleRelease.orElse(null);
    log.info("Latest Gradle Release: [ {} ]", latestGradleRelease);

    if (latestGradleRelease == null) {
      ProcessUtils.setErrorsOrExceptions(true);
      log.error("Latest Gradle Release Null Error...");
      return null;
    }

    final String versionFull = latestGradleRelease.getName();
    final String versionMajor = getVersionMajor(versionFull);
    final String versionDocker = getVersionDocker(versionMajor, latestJavaVersionMajor);

    return LatestVersion.builder()
        .versionActual(versionFull)
        .versionFull(versionFull)
        .versionDocker(versionDocker)
        .build();
  }

  public String getLatestGradlePlugin(final String group) {
    log.debug("Get Latest Gradle Plugin: [ {} ]", group);
    Document document = gradleConnector.getGradlePlugins(group);
    log.debug("Gradle Plugin Document: [ {} ] | [ {} ]", group, document);
    if (document != null) {
      Element versionElement = document.getElementsByClass("version-info").first();

      if (versionElement != null) {
        Element latestVersionElement = versionElement.selectFirst("h3");

        if (latestVersionElement != null) {
          String latestVersionText = latestVersionElement.text();
          return getLatestVersion(latestVersionText);
        } else {
          ProcessUtils.setErrorsOrExceptions(true);
          log.error("ERROR Latest Version Element is NULL: [ {} ]", group);
        }
      } else {
        ProcessUtils.setErrorsOrExceptions(true);
        log.error("ERROR Version Element is NULL: [ {} ]", group);
      }
    }
    return null;
  }

  private String getLatestVersion(final String latestVersionText) {
    String[] latestVersionTextArray = latestVersionText.split(" ");
    if (latestVersionTextArray.length == 3) {
      String version = latestVersionTextArray[1];
      if (!isCheckPreReleaseVersion(version)) {
        return version;
      }
    } else {
      ProcessUtils.setErrorsOrExceptions(true);
      log.error("ERROR Get Latest Gradle Plugin Version Wrong Length: [ {} ]", latestVersionText);
    }
    return null;
  }

  /**
   * @param versionFull eg: 8.10 or 8.10.2
   * @return eg: 8
   */
  private String getVersionMajor(final String versionFull) {
    return versionFull.trim().split("\\.")[0];
  }

  /**
   * @param versionFull eg: 8.10 or 8.10.2
   * @return eg: 8.10-jdk21-alpine or 8.10.2-jdk21-alpine
   */
  private String getVersionDocker(final String versionFull, final String latestJavaVersionMajor) {
    return "gradle:" + versionFull + "-jdk" + latestJavaVersionMajor + "-" + DOCKER_ALPINE;
  }
}
