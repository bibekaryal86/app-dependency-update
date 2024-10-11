package app.dependency.update.app.service;

import app.dependency.update.app.connector.DockerhubConnector;
import app.dependency.update.app.model.DockerhubResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DockerhubService {

  private final DockerhubConnector dockerhubConnector;

  public DockerhubService(final DockerhubConnector dockerhubConnector) {
    this.dockerhubConnector = dockerhubConnector;
  }

  public boolean checkIfDockerImageTagExists(final String library, final String tag) {
    log.debug("Check If Docker Image Tag Exists: [{}], [{}]", library, tag);

    DockerhubResponse dockerhubResponse = dockerhubConnector.getDockerImageTag(library, tag);
    return dockerhubResponse != null
        && dockerhubResponse.getName() != null
        && !dockerhubResponse.getName().trim().isEmpty();
  }
}
