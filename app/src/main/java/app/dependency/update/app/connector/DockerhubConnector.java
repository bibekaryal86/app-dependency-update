package app.dependency.update.app.connector;

import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.model.DockerhubResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class DockerhubConnector {

  private final RestTemplate restTemplate;

  public DockerhubConnector(final RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public DockerhubResponse getDockerImageTag(final String library, final String tag) {
    try {
      return restTemplate
          .exchange(
              String.format(DOCKER_TAG_LOOKUP_ENDPOINT, library, tag),
              HttpMethod.GET,
              null,
              DockerhubResponse.class)
          .getBody();
    } catch (RestClientException ex) {
      log.error("ERROR Get Docker Tag Release", ex);
    }
    return null;
  }
}
