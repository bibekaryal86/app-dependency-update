package app.dependency.update.app.connector;

import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.model.PypiSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class PypiConnector {

  private final RestTemplate restTemplate;

  public PypiConnector(final RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public PypiSearchResponse getPypiSearchResponse(final String name) {
    try {
      return restTemplate.getForObject(
          String.format(PYPI_SEARCH_ENDPOINT, name), PypiSearchResponse.class);
    } catch (RestClientException ex) {
      log.error("ERROR in Get Pypi Search Response: [ {} ]", name, ex);
    }

    return null;
  }
}
