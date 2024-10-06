package app.dependency.update.app.service;

import app.dependency.update.app.connector.NginxConnector;
import app.dependency.update.app.connector.NodeConnector;
import app.dependency.update.app.model.NginxReleaseResponse;
import app.dependency.update.app.model.NodeReleaseResponse;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NginxService {

  private final NginxConnector nginxConnector;

  public NginxService(NginxConnector nginxConnector) {
    this.nginxConnector = nginxConnector;
  }

  /**
   * @return node version in format release-1.27.2
   */
  public String getLatestNginxVersion() {
    List<NginxReleaseResponse> nginxReleaseResponses = nginxConnector.getNginxReleases();
    Optional<NginxReleaseResponse> optionalNginxReleaseResponse =
            nginxReleaseResponses.stream()
            .findFirst();

    NginxReleaseResponse latestNginxRelease = optionalNginxReleaseResponse.orElse(null);
    log.info("Latest Nginx Release: [ {} ]", latestNginxRelease);

    if (latestNginxRelease == null) {
      log.error("Latest Nginx Release Null Error...");
      return null;
    }

    return latestNginxRelease.getName();
  }
}
