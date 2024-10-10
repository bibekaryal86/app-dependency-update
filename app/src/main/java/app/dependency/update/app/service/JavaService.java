package app.dependency.update.app.service;

import static app.dependency.update.app.util.ConstantUtils.DOCKER_ALPINE;
import static app.dependency.update.app.util.ConstantUtils.DOCKER_JRE;

import app.dependency.update.app.connector.JavaConnector;
import app.dependency.update.app.model.JavaReleaseResponse;
import app.dependency.update.app.model.LatestVersion;
import app.dependency.update.app.util.ProcessUtils;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JavaService {

  private final JavaConnector javaConnector;

  public JavaService(JavaConnector javaConnector) {
    this.javaConnector = javaConnector;
  }

  public LatestVersion getLatestJavaVersion() {
    List<JavaReleaseResponse.JavaVersion> javaReleaseVersions = javaConnector.getJavaReleases();
    // get rid of non lts and sort by version descending
    Optional<JavaReleaseResponse.JavaVersion> optionalJavaReleaseVersion =
        javaReleaseVersions.stream()
            .filter(javaVersion -> "LTS".equals(javaVersion.getOptional()))
            .findFirst();

    JavaReleaseResponse.JavaVersion latestJavaRelease = optionalJavaReleaseVersion.orElse(null);
    log.info("Latest Java Release: [ {} ]", latestJavaRelease);

    if (latestJavaRelease == null) {
      ProcessUtils.setExceptionCaught(true);
      log.error("Latest Java Release Null Error...");
      return null;
    }

    final String versionActual = latestJavaRelease.getSemver();
    final String versionFull = getVersionFull(versionActual);
    final String versionMajor = getVersionMajor(versionFull);
    final String versionDocker = getVersionDocker(versionMajor);
    final String versionGcp = getVersionGcp(versionMajor);

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
  private String getVersionDocker(final String versionMajor) {
    return DOCKER_JRE + ":" + versionMajor + "-jre-" + DOCKER_ALPINE;
  }

  /**
   * @param versionMajor eg: 21
   * @return eg: java21
   */
  private String getVersionGcp(final String versionMajor) {
    return "java" + versionMajor;
  }
}
