package app.dependency.update.app.service;

import static app.dependency.update.app.util.CommonUtils.isCheckPreReleaseVersion;
import static app.dependency.update.app.util.ConstantUtils.DOCKER_ALPINE;

import app.dependency.update.app.connector.NginxConnector;
import app.dependency.update.app.model.LatestVersion;
import app.dependency.update.app.model.NginxReleaseResponse;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NginxService {

  private final NginxConnector nginxConnector;
  private final DockerhubService dockerhubService;

  public NginxService(NginxConnector nginxConnector, DockerhubService dockerhubService) {
    this.nginxConnector = nginxConnector;
    this.dockerhubService = dockerhubService;
  }

  public LatestVersion getLatestNginxVersion(final String latestDockerVersionFromMongo) {
    List<NginxReleaseResponse> nginxReleaseResponses = nginxConnector.getNginxReleases();
    Optional<NginxReleaseResponse> optionalNginxReleaseResponse =
        nginxReleaseResponses.stream()
            .filter(
                nginxReleaseResponse -> !isCheckPreReleaseVersion(nginxReleaseResponse.getName()))
            .findFirst();

    NginxReleaseResponse latestNginxRelease = optionalNginxReleaseResponse.orElse(null);
    log.info("Latest Nginx Release: [ {} ]", latestNginxRelease);

    if (latestNginxRelease == null) {
      log.error("Latest Nginx Release Null Error...");
      return null;
    }

    final String versionActual = latestNginxRelease.getName();
    final String versionFull = getVersionFull(versionActual);
    final String versionDocker = getVersionDocker(versionFull, latestDockerVersionFromMongo);

    return LatestVersion.builder()
        .versionActual(versionActual)
        .versionFull(versionFull)
        .versionDocker(versionDocker)
        .build();
  }

  /**
   * @param versionActual eg: release-1.27.2
   * @return eg: 1.27.2
   */
  private String getVersionFull(final String versionActual) {
    return versionActual.trim().split("-")[1];
  }

  /**
   * @param versionFull eg: 1.27.2
   * @return eg: nginx:1.27.2-alpine
   */
  private String getVersionDocker(
      final String versionFull, final String latestDockerVersionFromMongo) {
    final String library = "nginx";
    final String tag = versionFull + "-" + DOCKER_ALPINE;
    final boolean isNewDockerImageExists =
        dockerhubService.checkIfDockerImageTagExists(library, tag);
    if (isNewDockerImageExists) {
      return library + ":" + tag;
    }
    return latestDockerVersionFromMongo;
  }
}
