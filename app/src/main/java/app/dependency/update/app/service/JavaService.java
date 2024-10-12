package app.dependency.update.app.service;

import static app.dependency.update.app.util.CommonUtils.parseIntSafe;
import static app.dependency.update.app.util.ConstantUtils.DOCKER_ALPINE;
import static app.dependency.update.app.util.ConstantUtils.DOCKER_JRE;

import app.dependency.update.app.connector.JavaConnector;
import app.dependency.update.app.model.JavaReleaseResponse;
import app.dependency.update.app.model.LatestVersion;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JavaService {

  private final JavaConnector javaConnector;
  private final DockerhubService dockerhubService;

  public JavaService(JavaConnector javaConnector, DockerhubService dockerhubService) {
    this.javaConnector = javaConnector;
    this.dockerhubService = dockerhubService;
  }

  public LatestVersion getLatestJavaVersion(
      final String latestGcpRuntimeVersion, final String latestDockerVersionFromMongo) {
    List<JavaReleaseResponse.JavaVersion> javaReleaseVersions = javaConnector.getJavaReleases();
    // get rid of non lts and sort by version descending
    Optional<JavaReleaseResponse.JavaVersion> optionalJavaReleaseVersion =
        javaReleaseVersions.stream()
            .filter(javaVersion -> "LTS".equals(javaVersion.getOptional()))
            .findFirst();

    JavaReleaseResponse.JavaVersion latestJavaRelease = optionalJavaReleaseVersion.orElse(null);
    log.info("Latest Java Release: [ {} ]", latestJavaRelease);

    if (latestJavaRelease == null) {
      log.error("Latest Java Release Null Error...");
      return null;
    }

    final String versionActual = latestJavaRelease.getSemver();
    final String versionFull = getVersionFull(versionActual);
    final String versionMajor = getVersionMajor(versionFull);
    final String versionDocker = getVersionDocker(versionMajor, latestDockerVersionFromMongo);
    final String versionGcp = getVersionGcp(versionMajor, latestGcpRuntimeVersion);

    return LatestVersion.builder()
        .versionActual(versionActual)
        .versionFull(versionFull)
        .versionMajor(versionMajor)
        .versionDocker(versionDocker)
        .versionGcp(versionGcp)
        .build();
  }

  /**
   * @param versionActual eg: 21.0.4+7.0.LTS
   * @return eg: 21.0,4
   */
  private String getVersionFull(final String versionActual) {
    return versionActual.trim().split("\\+")[0];
  }

  /**
   * @param versionFull eg: 21.0.4
   * @return eg: 21
   */
  private String getVersionMajor(final String versionFull) {
    return versionFull.trim().split("\\.")[0];
  }

  /**
   * @param versionMajor eg: 21
   * @return eg: eclipse-temurin:21-jre-alpine
   */
  private String getVersionDocker(
      final String versionMajor, final String latestDockerVersionFromMongo) {
    final String library = DOCKER_JRE;
    final String tag = versionMajor + "-jre-" + DOCKER_ALPINE;
    final boolean isNewDockerImageExists =
        dockerhubService.checkIfDockerImageTagExists(library, tag);
    if (isNewDockerImageExists) {
      return library + ":" + tag;
    }
    return latestDockerVersionFromMongo;
  }

  /**
   * @param versionMajor eg: 21
   * @return eg: java21
   */
  private String getVersionGcp(final String versionMajor, final String latestGcpRuntimeVersion) {
    if (parseIntSafe(versionMajor) > parseIntSafe(latestGcpRuntimeVersion)) {
      return "java" + latestGcpRuntimeVersion;
    }
    return "java" + versionMajor;
  }
}
