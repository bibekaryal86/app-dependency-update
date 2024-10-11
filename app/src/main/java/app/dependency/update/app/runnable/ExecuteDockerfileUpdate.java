package app.dependency.update.app.runnable;

import static app.dependency.update.app.util.ConstantUtils.DOCKER_JRE;

import app.dependency.update.app.model.LatestVersionsModel;
import app.dependency.update.app.model.Repository;
import app.dependency.update.app.util.ProcessUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

@Slf4j
public class ExecuteDockerfileUpdate {
  private final Repository repository;
  private final LatestVersionsModel latestVersionsModel;
  private final Path dockerfilePath;

  public ExecuteDockerfileUpdate(
      final Repository repository, final LatestVersionsModel latestVersionsModel) {
    this.repository = repository;
    this.latestVersionsModel = latestVersionsModel;
    dockerfilePath = this.repository.getRepoPath().resolve("Dockerfile");
  }

  public boolean executeDockerfileUpdate() {
    if (Files.exists(this.dockerfilePath)) {
      List<String> dockerfileData = readDockerfile();
      dockerfileData = updateDockerfile(dockerfileData);
      return writeDockerfile(dockerfileData);
    }
    return false;
  }

  private List<String> readDockerfile() {
    try {
      return Files.readAllLines(this.dockerfilePath);
    } catch (IOException ex) {
      ProcessUtils.setErrorsOrExceptions(true);
      log.error("Error Reading Dockerfile of Repository [{}]", this.repository.getRepoName());
      return Collections.emptyList();
    }
  }

  private List<String> updateDockerfile(final List<String> dockerfileData) {
    if (CollectionUtils.isEmpty(dockerfileData)) {
      return dockerfileData;
    }

    boolean isUpdated = false;
    List<String> updatedDockerfileData = new ArrayList<>();

    for (String line : dockerfileData) {
      if (line.startsWith("FROM")) {
        final String currentVersion = getCurrentVersionDocker(line);
        final String latestVersion = getLatestVersionDocker(line);

        if (isSupportedFrom(currentVersion)
            && isSupportedFrom(latestVersion)
            && !currentVersion.equals(latestVersion)) {
          final String updatedLine = line.replace(currentVersion, latestVersion);
          updatedDockerfileData.add(updatedLine);
          isUpdated = true;
        } else {
          updatedDockerfileData.add(line);
        }
      } else {
        updatedDockerfileData.add(line);
      }
    }

    return isUpdated ? updatedDockerfileData : Collections.emptyList();
  }

  private String getCurrentVersionDocker(final String fromLine) {
    final String[] fromArray = fromLine.split(" ");
    if (fromArray.length > 1) {
      return fromArray[1];
    }
    return fromLine;
  }

  private String getLatestVersionDocker(final String fromLine) {
    if (fromLine.contains("gradle")) {
      return this.latestVersionsModel.getLatestVersionBuildTools().getGradle().getVersionDocker();
    }
    if (fromLine.contains(DOCKER_JRE)) {
      return this.latestVersionsModel.getLatestVersionLanguages().getJava().getVersionDocker();
    }
    if (fromLine.contains("node")) {
      return this.latestVersionsModel.getLatestVersionLanguages().getNode().getVersionDocker();
    }
    if (fromLine.contains("nginx")) {
      return this.latestVersionsModel.getLatestVersionAppServers().getNginx().getVersionDocker();
    }
    if (fromLine.contains("python")) {
      return this.latestVersionsModel.getLatestVersionLanguages().getPython().getVersionDocker();
    }
    return fromLine;
  }

  private boolean isSupportedFrom(String from) {
    return from.startsWith("gradle")
        || from.contains(DOCKER_JRE)
        || from.contains("node")
        || from.contains("nginx")
        || from.contains("python");
  }

  private boolean writeDockerfile(final List<String> dockerfileData) {
    if (CollectionUtils.isEmpty(dockerfileData)) {
      return false;
    }

    try {
      Files.write(this.dockerfilePath, dockerfileData, StandardCharsets.UTF_8);
      return true;
    } catch (IOException ex) {
      ProcessUtils.setErrorsOrExceptions(true);
      log.error(
          "Error Writing Updated Dockerfile of repository: [{}]", this.repository.getRepoName());
      return false;
    }
  }
}
