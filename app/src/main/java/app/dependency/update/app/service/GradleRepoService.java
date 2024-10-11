package app.dependency.update.app.service;

import static app.dependency.update.app.util.CommonUtils.isCheckPreReleaseVersion;
import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.connector.GradleConnector;
import app.dependency.update.app.model.GradleReleaseResponse;
import app.dependency.update.app.model.LatestVersion;
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
  private final DockerhubService dockerhubService;

  public GradleRepoService(GradleConnector gradleConnector, DockerhubService dockerhubService) {
    this.gradleConnector = gradleConnector;
    this.dockerhubService = dockerhubService;
  }

  public LatestVersion getLatestGradleVersion(
      final String latestJavaVersionMajor, final String latestDockerVersionFromMongo) {
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
      log.error("Latest Gradle Release Null Error...");
      return null;
    }

    final String versionFull = latestGradleRelease.getName();
    final String versionDocker =
        getVersionDocker(versionFull, latestJavaVersionMajor, latestDockerVersionFromMongo);

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
          log.error("ERROR Latest Version Element is NULL: [ {} ]", group);
        }
      } else {
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
      log.error("ERROR Get Latest Gradle Plugin Version Wrong Length: [ {} ]", latestVersionText);
    }
    return null;
  }

  /**
   * @param versionFull eg: 8.10 or 8.10.2
   * @return eg: 8.10-jdk21-alpine or 8.10.2-jdk21-alpine
   */
  private String getVersionDocker(
      final String versionFull,
      final String latestJavaVersionMajor,
      final String latestDockerVersionFromMongo) {
    final String library = "gradle";
    final String tag = versionFull + "-jdk" + latestJavaVersionMajor + "-" + DOCKER_ALPINE;
    final boolean isNewDockerImageExists =
        dockerhubService.checkIfDockerImageTagExists(library, tag);
    if (isNewDockerImageExists) {
      return library + ":" + tag;
    }
    return latestDockerVersionFromMongo;
  }
}
