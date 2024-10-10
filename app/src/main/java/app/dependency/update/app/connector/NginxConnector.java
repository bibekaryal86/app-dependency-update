package app.dependency.update.app.connector;

import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.model.NginxReleaseResponse;
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
public class NginxConnector {

  private final RestTemplate restTemplate;

  public NginxConnector(final RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public List<NginxReleaseResponse> getNginxReleases() {
    try {
      return restTemplate
          .exchange(
              NGINX_TAGS_ENDPOINT,
              HttpMethod.GET,
              null,
              new ParameterizedTypeReference<List<NginxReleaseResponse>>() {})
          .getBody();
    } catch (RestClientException ex) {
      ProcessUtils.setErrorsOrExceptions(true);
      log.error("ERROR Get Nginx Releases", ex);
    }

    return Collections.emptyList();
  }
}
