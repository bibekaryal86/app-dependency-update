package app.dependency.update.app.connector;

import app.dependency.update.app.model.GithubActionsReleaseResponse;
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
public class GithubActionsConnector {

  private final RestTemplate restTemplate;

  public GithubActionsConnector(final RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public List<GithubActionsReleaseResponse> getGithubActionsReleases(final String endpoint) {
    try {
      return restTemplate
          .exchange(
              endpoint,
              HttpMethod.GET,
              null,
              new ParameterizedTypeReference<List<GithubActionsReleaseResponse>>() {})
          .getBody();
    } catch (RestClientException ex) {
      log.error("ERROR Get Github Actions Releases", ex);
    }

    return Collections.emptyList();
  }
}
