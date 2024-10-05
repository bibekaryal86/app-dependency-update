package app.dependency.update.app.service;

import static app.dependency.update.app.util.CommonUtils.isCheckPreReleaseVersion;

import app.dependency.update.app.connector.PythonConnector;
import app.dependency.update.app.model.PythonReleaseResponse;
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

  /**
   * @return python version in format v3.12.1
   */
  public String getLatestPythonVersion() {
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
      log.error("Latest Python Release Null Error...");
      return null;
    }

    return latestPythonResponse.getName();
  }
}
