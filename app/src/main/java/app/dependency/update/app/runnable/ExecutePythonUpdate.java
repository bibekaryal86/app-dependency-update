package app.dependency.update.app.runnable;

import static app.dependency.update.app.util.CommonUtils.*;
import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.model.LatestVersionsModel;
import app.dependency.update.app.model.Repository;
import app.dependency.update.app.model.ScriptFile;
import app.dependency.update.app.model.entities.Packages;
import app.dependency.update.app.service.MongoRepoService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExecutePythonUpdate implements Runnable {
  private final String threadName;
  private final LatestVersionsModel latestVersionsModel;
  private final Repository repository;
  private final ScriptFile scriptFile;
  private final List<String> arguments;
  private final Map<String, Packages> packagesMap;
  private final MongoRepoService mongoRepoService;
  private Thread thread;
  private boolean isExecuteScriptRequired = false;

  public ExecutePythonUpdate(
      final LatestVersionsModel latestVersionsModel,
      final Repository repository,
      final ScriptFile scriptFile,
      final List<String> arguments,
      final MongoRepoService mongoRepoService) {
    this.threadName = threadName(repository, this.getClass().getSimpleName());
    this.latestVersionsModel = latestVersionsModel;
    this.repository = repository;
    this.scriptFile = scriptFile;
    this.arguments = arguments;
    this.packagesMap = mongoRepoService.packagesMap();
    this.mongoRepoService = mongoRepoService;
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

    final boolean isGcpConfigUpdated =
        new ExecuteGcpConfigsUpdate(
                this.repository, this.latestVersionsModel.getLatestVersionLanguages().getPython())
            .executeGcpConfigsUpdate();
    final boolean isDockerfileUpdated =
        new ExecuteDockerfileUpdate(this.repository, this.latestVersionsModel)
            .executeDockerfileUpdate();
    final boolean isGithubWorkflowsUpdated =
        new ExecuteGithubWorkflowsUpdate(this.repository, this.latestVersionsModel)
            .executeGithubWorkflowsUpdate();

    if (this.isExecuteScriptRequired
        || isGcpConfigUpdated
        || isDockerfileUpdated
        || isGithubWorkflowsUpdated) {
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

  private List<String> readFromFile(final Path path) {
    try {
      return Files.readAllLines(path);
    } catch (IOException ex) {
      log.error("Error reading file: [ {} ]", path);
    }
    return Collections.emptyList();
  }

  private void writeToFile(final Path path, final List<String> content) {
    try {
      Files.write(path, content, StandardCharsets.UTF_8);
      this.isExecuteScriptRequired = true;
    } catch (IOException ex) {
      log.error("Error Saving Updated File: [ {} ]", path, ex);
    }
  }

  private void executeRequirementsTxtUpdate() {
    for (String requirementsTxt : this.repository.getRequirementsTxts()) {
      updateRequirementsTxt(requirementsTxt);
    }
  }

  private void updateRequirementsTxt(final String requirementsTxt) {
    Path requirementsTxtPath =
        Path.of(
            this.repository
                .getRepoPath()
                .toString()
                .concat(PATH_DELIMITER)
                .concat(requirementsTxt));
    List<String> requirementsTxtContent = readFromFile(requirementsTxtPath);

    if (requirementsTxtContent.isEmpty()) {
      log.error(
          "[ {} ] content is empty: in [ {} ]", requirementsTxt, this.repository.getRepoName());
    } else {
      modifyRequirementsTxt(requirementsTxtPath, requirementsTxtContent);
    }
  }

  private void modifyRequirementsTxt(
      final Path requirementsTxtPath, final List<String> requirementsTxtContent) {
    boolean isUpdated = false;
    List<String> updatedRequirementsTxtContent = new ArrayList<>();

    for (String s : requirementsTxtContent) {
      String updatedS = updateRequirement(s);
      if (!s.equals(updatedS)) {
        isUpdated = true;
      }
      updatedRequirementsTxtContent.add(updatedS);
    }

    if (isUpdated) { // NOSONAR
      writeToFile(requirementsTxtPath, updatedRequirementsTxtContent);
    }
  }

  private String updateRequirement(final String requirement) {
    // ignore commented out lines
    if (requirement.startsWith("#")) {
      return requirement;
    }
    String updatedLine = requirement;
    String[] requirementArray;
    if (requirement.contains(">=")) {
      requirementArray = requirement.split(">=");
    } else {
      requirementArray = requirement.split("==");
    }

    if (requirementArray.length == 2) {
      String name = requirementArray[0].trim();
      String version = requirementArray[1].trim();
      Packages onePackage = this.packagesMap.get(name);

      if (onePackage == null) {
        // It is likely plugin information is not available in the local repository
        log.info("Packages information missing in local repo: [ {} ]", name);
        // Save to mongo repository
        mongoRepoService.savePackage(name, version);
      }

      String latestVersion = "";
      if (onePackage != null && !onePackage.isSkipVersion()) {
        latestVersion = onePackage.getVersion();
      }

      if (isRequiresUpdate(version, latestVersion)) {
        updatedLine = updatedLine.replace(version, latestVersion);
      }
    } else {
      log.error("Python Requirement Array Size Incorrect: [ {} ]", requirement);
    }
    return updatedLine;
  }

  /*
   * PYPROJECT.TOML UPDATE
   */

  private void executePyProjectTomlUpdate() {
    Path pyProjectTomlPath =
        Path.of(
            this.repository.getRepoPath().toString().concat(PATH_DELIMITER).concat(PYPROJECT_TOML));
    List<String> pyProjectContent = readFromFile(pyProjectTomlPath);

    if (pyProjectContent.isEmpty()) {
      log.error("PyProject Toml Content is empty: [ {} ]", this.repository.getRepoName());
    } else {
      modifyPyProjectToml(pyProjectTomlPath, pyProjectContent);
    }
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
      } else if (isRequiresPython(s)) {
        String updatedS = updateRequiresPython(s);
        if (!updatedS.equals(s)) {
          isUpdated = true;
        }
        updatedPyProjectContent.add(updatedS);
      } else if (isBlackTargetVersion(s)) {
        String updatedS = updateBlackTargetVersion(s);
        if (!updatedS.equals(s)) {
          isUpdated = true;
        }
        updatedPyProjectContent.add(updatedS);
      } else {
        updatedPyProjectContent.add(s);
      }
    }

    if (isUpdated) { // NOSONAR
      writeToFile(pyProjectTomlPath, updatedPyProjectContent);
    }
  }

  private boolean isRequiresBuildTools(final String line) {
    return line.contains("requires") && line.contains("setuptools") && line.contains("wheel");
  }

  private boolean isRequiresPython(final String line) {
    return line.contains("requires-python");
  }

  private boolean isBlackTargetVersion(final String line) {
    return line.contains("target-version");
  }

  private String updateBuildTools(final String line) {
    List<String> buildTools = new ArrayList<>();
    Pattern pattern = Pattern.compile(PYTHON_PYPROJECT_TOML_BUILDTOOLS_REGEX);
    Matcher matcher = pattern.matcher(line);

    while (matcher.find()) {
      buildTools.add(matcher.group(1));
    }

    return updateBuildTools(line, buildTools);
  }

  private String updateRequiresPython(final String line) {
    final String currentVersion = line.replaceAll("[^\\d.]", "").trim();
    final String latestVersion =
        getVersionMajorMinor(
            this.latestVersionsModel.getLatestVersionLanguages().getPython().getVersionFull(),
            true);

    if (currentVersion.equals(latestVersion)) {
      return line;
    }

    return line.replace(currentVersion, latestVersion);
  }

  private String updateBlackTargetVersion(final String line) {
    final String currentVersion = getCurrentBlackTargetVersion(line);
    final String latestVersion =
        "py"
            + getVersionMajorMinor(
                this.latestVersionsModel.getLatestVersionLanguages().getPython().getVersionFull(),
                false);

    if (currentVersion.equals(latestVersion)) {
      return line;
    }

    return line.replace(currentVersion, latestVersion);
  }

  public String getCurrentBlackTargetVersion(final String line) {
    String[] parts = line.split("=");
    String versionPart = parts[1].trim();
    versionPart = versionPart.replaceAll("[\\[\\]' ]", "");
    return versionPart;
  }

  // suppressing sonarlint rule for cognitive complexity of method too high
  @SuppressWarnings({"java:S3776"})
  private String updateBuildTools(final String line, final List<String> buildTools) {
    String updatedLine = line;
    for (String buildTool : buildTools) {
      String[] buildToolArray;
      if (buildTool.contains(">=")) {
        buildToolArray = buildTool.split(">=");
      } else {
        buildToolArray = buildTool.split("==");
      }

      if (buildToolArray.length == 2) {
        String name = buildToolArray[0].trim();
        String version = buildToolArray[1].trim();
        Packages onePackage = this.packagesMap.get(name);

        if (onePackage == null) {
          // It is likely plugin information is not available in the local repository
          log.info("Packages information missing in local repo: [ {} ]", name);
          // Save to mongo repository
          mongoRepoService.savePackage(name, version);
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
