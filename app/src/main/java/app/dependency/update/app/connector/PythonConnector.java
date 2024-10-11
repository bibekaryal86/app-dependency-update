package app.dependency.update.app.connector;

import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.model.PythonReleaseResponse;
import app.dependency.update.app.util.ProcessUtils;
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
public class PythonConnector {

  private final RestTemplate restTemplate;

  public PythonConnector(final RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public List<PythonReleaseResponse> getPythonReleases() {
    try {
      return restTemplate
          .exchange(
              PYTHON_RELEASES_ENDPOINT,
              HttpMethod.GET,
              null,
              new ParameterizedTypeReference<List<PythonReleaseResponse>>() {})
          .getBody();
    } catch (RestClientException ex) {
      ProcessUtils.setErrorsOrExceptions(true);
      log.error("ERROR Get Python Releases", ex);
    }

    return Collections.emptyList();
  }
}
