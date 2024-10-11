package app.dependency.update.app.connector;

import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.model.MavenSearchResponse;
import app.dependency.update.app.util.ProcessUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class MavenConnector {

  private final RestTemplate restTemplate;

  public MavenConnector(final RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public MavenSearchResponse getMavenSearchResponse(final String group, final String artifact) {
    try {
      return restTemplate.getForObject(
          String.format(MAVEN_SEARCH_ENDPOINT, group, artifact), MavenSearchResponse.class);
    } catch (RestClientException ex) {
      ProcessUtils.setErrorsOrExceptions(true);
      log.error("ERROR in Get Maven Search Response: [ {} ] [ {} ]", group, artifact, ex);
    }

    return null;
  }
}
