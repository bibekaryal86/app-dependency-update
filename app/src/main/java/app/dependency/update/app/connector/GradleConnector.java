package app.dependency.update.app.connector;

import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.model.GradleReleaseResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class GradleConnector {

  private final RestTemplate restTemplate;

  public GradleConnector(final RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public List<GradleReleaseResponse> getGradleReleases() {
    try {
      return restTemplate
          .exchange(
              GRADLE_RELEASES_ENDPOINT,
              HttpMethod.GET,
              null,
              new ParameterizedTypeReference<List<GradleReleaseResponse>>() {})
          .getBody();
    } catch (RestClientException ex) {
      log.error("ERROR Get Gradle Releases", ex);
    }

    return Collections.emptyList();
  }

  public Document getGradlePlugins(final String group) {
    try {
      String url = String.format(GRADLE_PLUGINS_ENDPOINT, group);
      return Jsoup.connect(url).get();
    } catch (IOException ex) {
      log.error("ERROR Get Gradle Plugins: [ {} ]", group, ex);
    }
    return null;
  }
}
