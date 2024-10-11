package app.dependency.update.app.runnable;

import app.dependency.update.app.model.LatestVersion;
import app.dependency.update.app.model.Repository;
import app.dependency.update.app.util.ProcessUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

@Slf4j
public class ExecuteGcpConfigsUpdate {
  private final Repository repository;
  private final LatestVersion latestVersion;
  private final Path yamlFilePath;

  public ExecuteGcpConfigsUpdate(final Repository repository, final LatestVersion latestVersion) {
    this.repository = repository;
    this.latestVersion = latestVersion;
    yamlFilePath = this.repository.getRepoPath().resolve("gcp/app.yaml");
  }

  public boolean executeGcpConfigsUpdate() {
    if (Files.exists(this.yamlFilePath)) {
      List<String> yamlData = readGcpAppYaml();
      yamlData = updateGcpAppYaml(yamlData);
      return writeGcpAppYaml(yamlData);
    }
    return false;
  }

  private List<String> readGcpAppYaml() {
    try {
      return Files.readAllLines(this.yamlFilePath);
    } catch (IOException ex) {
      ProcessUtils.setErrorsOrExceptions(true);
      log.error("Error Reading GCP App Yaml of Repository [{}]", this.repository.getRepoName());
      return Collections.emptyList();
    }
  }

  private List<String> updateGcpAppYaml(final List<String> yamlData) {
    if (CollectionUtils.isEmpty(yamlData)) {
      return yamlData;
    }

    final String latestGcpRuntime = this.latestVersion.getVersionGcp();
    // assumption: gcp app yaml's first line is always runtime
    final String currentGcpRuntime = getCurrentGcpRuntime(yamlData.getFirst());

    if (currentGcpRuntime == null || latestGcpRuntime.equals(currentGcpRuntime)) {
      return Collections.emptyList();
    }

    final String updatedRuntime = yamlData.getFirst().replace(currentGcpRuntime, latestGcpRuntime);
    yamlData.set(0, updatedRuntime);
    return yamlData;
  }

  private String getCurrentGcpRuntime(final String runtimeLine) {
    final String[] runtimeArray = runtimeLine.split(":");

    if (runtimeArray.length != 2) {
      ProcessUtils.setErrorsOrExceptions(true);
      log.error("Malformed GCP App Yaml Runtime: [{}]", runtimeLine);
      return null;
    }

    final String runtimeValue = runtimeArray[1].trim();

    if (!isSupportedRuntime(runtimeValue)) {
      ProcessUtils.setErrorsOrExceptions(true);
      log.error("Incorrect GCP App Yaml Runtime: [{}]", runtimeLine);
      return null;
    }

    return runtimeArray[1].trim();
  }

  private boolean isSupportedRuntime(String runtime) {
    return runtime.contains("java") || runtime.contains("node") || runtime.contains("python");
  }

  private boolean writeGcpAppYaml(final List<String> yamlData) {
    if (CollectionUtils.isEmpty(yamlData)) {
      return false;
    }

    try {
      Files.write(this.yamlFilePath, yamlData, StandardCharsets.UTF_8);
      return true;
    } catch (IOException ex) {
      ProcessUtils.setErrorsOrExceptions(true);
      log.error(
          "Error Writing Updated GCP App Yaml of repository: [{}]", this.repository.getRepoName());
      return false;
    }
  }
}
