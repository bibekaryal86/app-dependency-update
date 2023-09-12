package app.dependency.update.app.service;

import app.dependency.update.app.connector.PypiConnector;
import app.dependency.update.app.model.PypiSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PypiRepoService {

  private final PypiConnector pypiConnector;

  public PypiRepoService(PypiConnector pypiConnector) {
    this.pypiConnector = pypiConnector;
  }

  public String getLatestPackageVersion(final String name) {
    PypiSearchResponse pypiSearchResponse = pypiConnector.getPypiSearchResponse(name);

    if (pypiSearchResponse == null
        || pypiSearchResponse.getInfo() == null
        || pypiSearchResponse.getInfo().isYanked()) {
      return null;
    }

    return pypiSearchResponse.getInfo().getVersion();
  }
}
