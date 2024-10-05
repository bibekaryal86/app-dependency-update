package app.dependency.update.app.service;

import app.dependency.update.app.connector.NodeConnector;
import app.dependency.update.app.model.NodeReleaseResponse;
import java.util.Comparator;
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

  /**
   * @return node version in format v22.5.1
   */
  public String getLatestNodeVersion() {
    List<NodeReleaseResponse> nodeReleaseResponses = nodeConnector.getNodeReleases();
    // get rid of non lts and sort by version descending
    Optional<NodeReleaseResponse> optionalNodeReleaseResponse =
        nodeReleaseResponses.stream()
            .filter(NodeReleaseResponse::isLts)
            .max(Comparator.comparing(NodeReleaseResponse::getVersion));

    NodeReleaseResponse latestNodeRelease = optionalNodeReleaseResponse.orElse(null);
    log.info("Latest Node Release: [ {} ]", latestNodeRelease);

    if (latestNodeRelease == null) {
      log.error("Latest Node Release Null Error...");
      return null;
    }

    return latestNodeRelease.getVersion();
  }
}
