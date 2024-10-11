package app.dependency.update.app.connector;

import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.model.JavaReleaseResponse;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class JavaConnector {

  private final RestTemplate restTemplate;

  public JavaConnector(final RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public List<JavaReleaseResponse.JavaVersion> getJavaReleases() {
    try {
      return Objects.requireNonNull(
              restTemplate
                  .exchange(JAVA_RELEASES_ENDPOINT, HttpMethod.GET, null, JavaReleaseResponse.class)
                  .getBody())
          .getVersions();
    } catch (RestClientException ex) {
      log.error("ERROR Get Java Releases", ex);
    }

    return Collections.emptyList();
  }
}
