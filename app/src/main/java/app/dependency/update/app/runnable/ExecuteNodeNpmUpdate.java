package app.dependency.update.app.runnable;

import static app.dependency.update.app.util.CommonUtils.*;
import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.model.LatestVersionsModel;
import app.dependency.update.app.model.Repository;
import app.dependency.update.app.model.ScriptFile;
import app.dependency.update.app.util.ProcessUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExecuteNodeNpmUpdate implements Runnable {
  private final String threadName;
  private final LatestVersionsModel latestVersionsModel;
  private final Repository repository;
  private final ScriptFile scriptFile;
  private final List<String> arguments;
  private Thread thread;

  public ExecuteNodeNpmUpdate(
      final LatestVersionsModel latestVersionsModel,
      final Repository repository,
      final ScriptFile scriptFile,
      final List<String> arguments) {
    this.threadName = threadName(repository, this.getClass().getSimpleName());
    this.latestVersionsModel = latestVersionsModel;
    this.repository = repository;
    this.scriptFile = scriptFile;
    this.arguments = arguments;
  }

  @Override
  public void run() {
    executeNodeUpdate();
  }

  public Thread start() {
    if (this.thread == null) {
      this.thread = new Thread(this, this.threadName);
      this.thread.start();
    }
    return this.thread;
  }

  private void executeNodeUpdate() {
    executePackageJsonUpdate();

    new ExecuteGcpConfigsUpdate(
            this.repository, this.latestVersionsModel.getLatestVersionLanguages().getNode())
        .executeGcpConfigsUpdate();
    new ExecuteDockerfileUpdate(this.repository, this.latestVersionsModel)
        .executeDockerfileUpdate();
    new ExecuteGithubWorkflowsUpdate(this.repository, this.latestVersionsModel)
        .executeGithubWorkflowsUpdate();

    Thread executeThread =
        new ExecuteScriptFile(
                threadName(repository, "-" + this.getClass().getSimpleName()),
                // simple name used in thread name for current class already, so use "-"
                this.scriptFile,
                this.arguments)
            .start();
    join(executeThread);
  }

  private List<String> readFromFile(final Path path) {
    try {
      return Files.readAllLines(path);
    } catch (IOException ex) {
      ProcessUtils.setExceptionCaught(true);
      log.error("Error reading file: [ {} ]", path);
    }
    return Collections.emptyList();
  }

  private void writeToFile(final Path path, final List<String> content) {
    try {
      Files.write(path, content, StandardCharsets.UTF_8);
    } catch (IOException ex) {
      ProcessUtils.setExceptionCaught(true);
      log.error("Error Saving Updated File: [ {} ]", path, ex);
    }
  }

  private void executePackageJsonUpdate() {
    final Path packageJsonPath =
        Path.of(
            this.repository.getRepoPath().toString().concat(PATH_DELIMITER).concat(PACKAGE_JSON));
    List<String> packageJsonContent = readFromFile(packageJsonPath);

    if (packageJsonContent.isEmpty()) {
      ProcessUtils.setExceptionCaught(true);
      log.error("Package Json Content is empty: [ {} ]", this.repository.getRepoName());
    } else {
      modifyPyProjectToml(packageJsonPath, packageJsonContent);
    }
  }

  private void modifyPyProjectToml(
      final Path packageJsonPath, final List<String> packageJsonContent) {
    boolean isUpdated = false;
    List<String> updatedPackageJsonContent = new ArrayList<>();

    for (int i = 0; i < packageJsonContent.size(); i++) {
      String currentLine = packageJsonContent.get(i);
      String previousLine = "";
      if (i - 1 >= 0) {
        previousLine = packageJsonContent.get(i - 1);
      }

      if (previousLine.isEmpty()) {
        updatedPackageJsonContent.add(currentLine);
        continue;
      }

      if (currentLine.contains("node") && previousLine.contains("engines")) {
        String updatedCurrentLine = updateNodeInEngineBlock(currentLine);
        if (!updatedCurrentLine.equals(currentLine)) {
          isUpdated = true;
        }
        updatedPackageJsonContent.add(updatedCurrentLine);
      } else {
        updatedPackageJsonContent.add(currentLine);
      }
    }

    if (isUpdated) { // NOSONAR
      writeToFile(packageJsonPath, updatedPackageJsonContent);
    }
  }

  private String updateNodeInEngineBlock(final String currentLine) {
    final String[] currentArray = currentLine.split(":");

    if (currentArray.length == 2) {
      final String currentVersion = currentArray[1].replace("\"", "").trim();
      final String latestVersion =
          this.latestVersionsModel.getLatestVersionLanguages().getNode().getVersionMajor();

      if (currentVersion.equals(latestVersion)) {
        return currentLine;
      }

      return currentLine.replace(currentVersion, latestVersion);
    }

    return currentLine;
  }

  // suppressing sonarlint rule for interrupting thread
  @SuppressWarnings("java:S2142")
  private void join(final Thread thread) {
    try {
      thread.join();
    } catch (InterruptedException ex) {
      ProcessUtils.setExceptionCaught(true);
      log.error("Exception Join Thread", ex);
    }
  }
}
