package app.dependency.update.app.service;

import static app.dependency.update.app.util.CommonUtils.getVersionMajorMinor;
import static app.dependency.update.app.util.CommonUtils.isCheckPreReleaseVersion;
import static app.dependency.update.app.util.CommonUtils.parseIntSafe;
import static app.dependency.update.app.util.ConstantUtils.DOCKER_ALPINE;

import app.dependency.update.app.connector.PythonConnector;
import app.dependency.update.app.model.LatestVersion;
import app.dependency.update.app.model.PythonReleaseResponse;
import app.dependency.update.app.util.ProcessUtils;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PythonService {

  private final PythonConnector pythonConnector;

  public PythonService(PythonConnector pythonConnector) {
    this.pythonConnector = pythonConnector;
  }

  public LatestVersion getLatestPythonVersion(final String latestGcpRuntimeVersion) {
    List<PythonReleaseResponse> pythonReleaseResponses = pythonConnector.getPythonReleases();
    // get rid of alpha, beta and release candidates by version descending
    Optional<PythonReleaseResponse> optionalPythonReleaseResponse =
        pythonReleaseResponses.stream()
            .filter(
                pythonReleaseResponse -> !isCheckPreReleaseVersion(pythonReleaseResponse.getName()))
            .findFirst();

    PythonReleaseResponse latestPythonResponse = optionalPythonReleaseResponse.orElse(null);
    log.info("Latest Python Release: [ {} ]", latestPythonResponse);

    if (latestPythonResponse == null) {
      ProcessUtils.setErrorsOrExceptions(true);
      log.error("Latest Python Release Null Error...");
      return null;
    }

    final String versionActual = latestPythonResponse.getName();
    final String versionFull = getVersionFull(versionActual);
    final String versionDocker = getVersionDocker(versionFull);
    final String versionGcp = getVersionGcp(versionFull, latestGcpRuntimeVersion);

    return LatestVersion.builder()
        .versionActual(versionActual)
        .versionFull(versionFull)
        .versionDocker(versionDocker)
        .versionGcp(versionGcp)
        .build();
  }

  /**
   * @param versionActual eg: v3.12.7
   * @return eg: 3.12.7
   */
  private String getVersionFull(final String versionActual) {
    return versionActual.replaceAll("[^0-9.]", "");
  }

  /**
   * @param versionFull eg: 3.12 or 3.12.7
   * @return eg: python:3.12-alpine or python:3.12.7-alpine
   */
  private String getVersionDocker(final String versionFull) {
    return "python:" + versionFull + "-" + DOCKER_ALPINE;
  }

  /**
   * @param versionFull eg: 3.12.7
   * @return eg: python312
   */
  private String getVersionGcp(final String versionFull, final String latestGcpRuntimeVersion) {
    final String versionMajorMinor = getVersionMajorMinor(versionFull, false);
    if (parseIntSafe(versionMajorMinor) < parseIntSafe(latestGcpRuntimeVersion)) {
      return "python" + latestGcpRuntimeVersion;
    }
    return "python" + versionMajorMinor;
  }
}
