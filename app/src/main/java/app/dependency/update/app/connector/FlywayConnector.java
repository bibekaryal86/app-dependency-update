package app.dependency.update.app.connector;

import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.model.FlywayReleaseResponse;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class FlywayConnector {

  private final RestTemplate restTemplate;

  public FlywayConnector(final RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public List<FlywayReleaseResponse> getFlywayReleases() {
    try {
      return restTemplate
          .exchange(
              FLYWAY_RELEASES_ENDPOINT,
              HttpMethod.GET,
              null,
              new ParameterizedTypeReference<List<FlywayReleaseResponse>>() {})
          .getBody();
    } catch (RestClientException ex) {
      log.error("ERROR Get Flyway Releases", ex);
    }

    return Collections.emptyList();
  }
}
