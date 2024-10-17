package app.dependency.update.app.service;

import app.dependency.update.app.connector.FlywayConnector;
import app.dependency.update.app.model.FlywayReleaseResponse;
import app.dependency.update.app.model.LatestVersion;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FlywayService {

  private final FlywayConnector flywayConnector;

  public FlywayService(FlywayConnector flywayConnector) {
    this.flywayConnector = flywayConnector;
  }

  public LatestVersion getLatestFlywayVersion() {
    List<FlywayReleaseResponse> flywayReleaseResponses = flywayConnector.getFlywayReleases();

    Optional<FlywayReleaseResponse> optionalLatestFlywayRelease =
        flywayReleaseResponses.stream()
            .filter(
                flywayReleaseResponse ->
                    !(flywayReleaseResponse.isPrerelease() || flywayReleaseResponse.isDraft()))
            .findFirst();

    FlywayReleaseResponse latestFlywayResponse = optionalLatestFlywayRelease.orElse(null);
    log.info("Latest Flyway Release: [{}]", latestFlywayResponse);

    if (latestFlywayResponse == null) {
      log.error("Latest Flyway Release Null Error...");
      return null;
    }

    final String versionActual = latestFlywayResponse.getTagName();
    final String versionFull = getVersionFull(versionActual);
    final String versionMajor = getVersionMajor(versionFull);

    return LatestVersion.builder()
        .versionActual(versionActual)
        .versionFull(versionFull)
        .versionMajor(versionMajor)
        .build();
  }

  /**
   * @param versionActual eg: v4.2.0 or codeql-bundle-v2.19.1
   * @return eg: 4.2.0 or 2.19.1
   */
  private String getVersionFull(final String versionActual) {
    return versionActual.replaceAll("[^0-9.]", "");
  }

  /**
   * @param versionFull eg: 4.2.0 or 2.19.1
   * @return eg: 4 or 2
   */
  private String getVersionMajor(final String versionFull) {
    return versionFull.trim().split("\\.")[0];
  }
}
