package app.dependency.update.app.service;

import static app.dependency.update.app.util.CommonUtils.parseIntSafe;
import static app.dependency.update.app.util.ConstantUtils.DOCKER_ALPINE;

import app.dependency.update.app.connector.NodeConnector;
import app.dependency.update.app.model.LatestVersion;
import app.dependency.update.app.model.NodeReleaseResponse;
import app.dependency.update.app.util.ProcessUtils;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NodeService {

  private final NodeConnector nodeConnector;

  public NodeService(NodeConnector nodeConnector) {
    this.nodeConnector = nodeConnector;
  }

  public LatestVersion getLatestNodeVersion(final String latestGcpRuntimeVersion) {
    List<NodeReleaseResponse> nodeReleaseResponses = nodeConnector.getNodeReleases();
    // get rid of non lts and sort by version descending
    Optional<NodeReleaseResponse> optionalNodeReleaseResponse =
        nodeReleaseResponses.stream()
            .filter(nodeReleaseResponse -> !"false".equals(nodeReleaseResponse.getLts()))
            .findFirst();

    NodeReleaseResponse latestNodeRelease = optionalNodeReleaseResponse.orElse(null);
    log.info("Latest Node Release: [ {} ]", latestNodeRelease);

    if (latestNodeRelease == null) {
      ProcessUtils.setErrorsOrExceptions(true);
      log.error("Latest Node Release Null Error...");
      return null;
    }

    final String versionActual = latestNodeRelease.getVersion();
    final String versionFull = getVersionFull(versionActual);
    final String versionMajor = getVersionMajor(versionFull);
    final String versionDocker = getVersionDocker(versionMajor);
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
   * @param versionActual eg: v20.18.0
   * @return eg: 20.18.0
   */
  private String getVersionFull(final String versionActual) {
    return versionActual.replaceAll("[^0-9.]", "");
  }

  /**
   * @param versionFull eg: 20.18.0
   * @return eg: 20
   */
  private String getVersionMajor(final String versionFull) {
    return versionFull.trim().split("\\.")[0];
  }

  /**
   * @param versionMajor eg: 20
   * @return eg: node:20-alpine
   */
  private String getVersionDocker(final String versionMajor) {
    return "node:" + versionMajor + "-" + DOCKER_ALPINE;
  }

  /**
   * @param versionMajor eg: 20
   * @return eg: nodejs20
   */
  private String getVersionGcp(final String versionMajor, final String latestGcpRuntimeVersion) {
    if (parseIntSafe(versionMajor) < parseIntSafe(latestGcpRuntimeVersion)) {
      return "nodejs" + latestGcpRuntimeVersion;
    }
    return "nodejs" + versionMajor;
  }
}
