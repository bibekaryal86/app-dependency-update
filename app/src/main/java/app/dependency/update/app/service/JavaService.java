package app.dependency.update.app.service;

import app.dependency.update.app.connector.JavaConnector;
import app.dependency.update.app.model.JavaReleaseResponse;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JavaService {

  private final JavaConnector javaConnector;

  public JavaService(JavaConnector javaConnector) {
    this.javaConnector = javaConnector;
  }

  /**
   * @return java version in format 21.0.4+7.0.LTS
   */
  public String getLatestJavaVersion() {
    List<JavaReleaseResponse.JavaVersion> javaReleaseVersions = javaConnector.getJavaReleases();
    // get rid of non lts and sort by version descending
    Optional<JavaReleaseResponse.JavaVersion> optionalJavaReleaseVersion =
        javaReleaseVersions.stream()
            .filter(javaVersion -> "LTS".equals(javaVersion.getOptional()))
            .findFirst();

    JavaReleaseResponse.JavaVersion latestJavaRelease = optionalJavaReleaseVersion.orElse(null);
    log.info("Latest Java Release: [ {} ]", latestJavaRelease);

    if (latestJavaRelease == null) {
      log.error("Latest Java Release Null Error...");
      return null;
    }

    return latestJavaRelease.getSemver();
  }
}
