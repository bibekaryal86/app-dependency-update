package app.dependency.update.app.runnable;

import static app.dependency.update.app.util.CommonUtils.*;
import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.model.Repository;
import app.dependency.update.app.model.ScriptFile;
import app.dependency.update.app.model.dto.Packages;
import app.dependency.update.app.service.MavenRepoService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExecutePythonUpdate implements Runnable {
  private final String threadName;
  private final Repository repository;
  private final ScriptFile scriptFile;
  private final List<String> arguments;
  private final Map<String, Packages> packagesMap;
  private final MavenRepoService mavenRepoService;
  private Thread thread;
  private boolean isExecuteScriptRequired = false;

  public ExecutePythonUpdate(
      final Repository repository,
      final ScriptFile scriptFile,
      final List<String> arguments,
      final MavenRepoService mavenRepoService) {
    this.threadName = threadName(repository, this.getClass().getSimpleName());
    this.repository = repository;
    this.scriptFile = scriptFile;
    this.arguments = arguments;
    this.packagesMap = mavenRepoService.packagesMap();
    this.mavenRepoService = mavenRepoService;
  }

  @Override
  public void run() {
    executePythonUpdate();
  }

  public Thread start() {
    if (this.thread == null) {
      this.thread = new Thread(this, this.threadName);
      this.thread.start();
    }
    return this.thread;
  }

  private void executePythonUpdate() {
    executePyProjectTomlUpdate();
    executeRequirementsTxtUpdate();

    if (this.isExecuteScriptRequired) {
      Thread executeThread =
          new ExecuteScriptFile(
                  threadName(repository, "-" + this.getClass().getSimpleName()),
                  // simple name used in thread name for current class already, so use "-"
                  this.scriptFile,
                  this.arguments)
              .start();
      join(executeThread);
    }
  }

  private void executeRequirementsTxtUpdate() {}

  private void writeToFile(final Path path, final List<String> content) {
    try {
      Files.write(path, content, StandardCharsets.UTF_8);
      this.isExecuteScriptRequired = true;
    } catch (IOException ex) {
      log.error("Error Saving Updated File: [ {} ]", path, ex);
    }
  }

  // TODO requirements.txt

  /*
   * PYPROJECT.TOML UPDATE
   */

  private void executePyProjectTomlUpdate() {
    Path pyProjectTomlPath =
        Path.of(
            this.repository.getRepoPath().toString().concat(PATH_DELIMITER).concat(PYPROJECT_TOML));
    List<String> pyProjectContent = readPyProjectToml(pyProjectTomlPath);

    if (pyProjectContent == null) {
      log.error("PyProject Toml Content is null: [ {} ]", this.repository.getRepoPath());
    } else {
      modifyPyProjectToml(pyProjectTomlPath, pyProjectContent);
    }
  }

  private List<String> readPyProjectToml(final Path pyProjectTomlPath) {
    try {
      return Files.readAllLines(pyProjectTomlPath);
    } catch (IOException ex) {
      log.error("Error reading pyproject.toml: [ {} ]", this.repository.getRepoName());
    }
    return null;
  }

  private void modifyPyProjectToml(
      final Path pyProjectTomlPath, final List<String> pyProjectContent) {
    boolean isUpdated = false;
    List<String> updatedPyProjectContent = new ArrayList<>();
    for (String s : pyProjectContent) {
      if (isRequiresBuildTools(s)) {
        String updatedS = updateBuildTools(s);
        if (!updatedS.equals(s)) {
          isUpdated = true;
        }
        updatedPyProjectContent.add(updatedS);
      } else {
        updatedPyProjectContent.add(s);
      }
    }

    if (isUpdated) {
      writeToFile(pyProjectTomlPath, updatedPyProjectContent);
    }
  }

  private boolean isRequiresBuildTools(final String line) {
    return line.contains("requires") && line.contains("setuptools") && line.contains("wheel");
  }

  private String updateBuildTools(final String line) {
    // extract build tools
    List<String> buildTools = new ArrayList<>();
    Pattern pattern = Pattern.compile(PYTHON_PYPROJECT_TOML_BUILDTOOLS_REGEX);
    Matcher matcher = pattern.matcher(line);

    while (matcher.find()) {
      buildTools.add(matcher.group(1));
    }

    return updateBuildTools(line, buildTools);
  }

  private String updateBuildTools(final String line, final List<String> buildTools) {
    String updatedLine = line;
    for (String buildTool : buildTools) {
      String[] buildToolArray;
      if (buildTool.contains(">=")) {
        buildToolArray = buildTool.split(">=");
      } else buildToolArray = buildTool.split("==");

      if (buildToolArray.length == 2) {
        String name = buildToolArray[0].trim();
        String version = buildToolArray[1].trim();
        Packages onePackage = this.packagesMap.get(name);

        if (onePackage == null) {
          // It is likely plugin information is not available in the local repository
          log.info("Packages information missing in local repo: [ {} ]", name);
          // Save to mongo repository
          mavenRepoService.savePackage(name, version);
        }

        String latestVersion = "";
        if (onePackage != null && !onePackage.isSkipVersion()) {
          latestVersion = onePackage.getVersion();
        }

        if (isRequiresUpdate(version, latestVersion)) {
          updatedLine = updatedLine.replace(version, latestVersion);
        }
      } else {
        log.error("Build Tool Array Size Incorrect: [ {} ]", buildTool);
      }
    }
    return updatedLine;
  }

  // suppressing sonarlint rule for interrupting thread
  @SuppressWarnings("java:S2142")
  private void join(final Thread thread) {
    try {
      thread.join();
    } catch (InterruptedException ex) {
      log.error("Exception Join Thread", ex);
    }
  }
}
