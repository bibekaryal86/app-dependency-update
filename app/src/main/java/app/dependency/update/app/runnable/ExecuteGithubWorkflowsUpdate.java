package app.dependency.update.app.runnable;

import static app.dependency.update.app.util.CommonUtils.getVersionMajorMinor;

import app.dependency.update.app.model.LatestVersions;
import app.dependency.update.app.model.Repository;
import app.dependency.update.app.util.CommonUtils;
import app.dependency.update.app.util.ProcessUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Slf4j
public class ExecuteGithubWorkflowsUpdate {
  private final Repository repository;
  private final LatestVersions latestVersions;
  private final Path githubWorkflowsFolderPath;

  public ExecuteGithubWorkflowsUpdate(
      final Repository repository, final LatestVersions latestVersions) {
    this.repository = repository;
    this.latestVersions = latestVersions;
    githubWorkflowsFolderPath = this.repository.getRepoPath().resolve(".github");
  }

  public boolean executeGithubWorkflowsUpdate() {
    boolean isUpdated = false;
    if (Files.exists(this.githubWorkflowsFolderPath)) {
      List<Path> githubWorkflowPaths = findGithubWorkflows();
      for (Path githubWorkflowPath : githubWorkflowPaths) {
        List<String> githubWorkflowContent = readGithubWorkflowFile(githubWorkflowPath);
        githubWorkflowContent = updateGithubWorkflowFile(githubWorkflowContent);
        boolean isWrittenToFile =
            writeGithubWorkflowFile(githubWorkflowPath, githubWorkflowContent);

        if (isWrittenToFile && !isUpdated) {
          isUpdated = true;
        }
      }
    }
    return isUpdated;
  }

  private List<Path> findGithubWorkflows() {
    try (Stream<Path> stream = Files.walk(this.githubWorkflowsFolderPath, 2)) {
      return stream
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".yml"))
          .collect(Collectors.toList());
    } catch (IOException ex) {
      ProcessUtils.setExceptionCaught(true);
      log.error("Find Github Actions Files: [{}]", this.githubWorkflowsFolderPath, ex);
      return Collections.emptyList();
    }
  }

  private List<String> readGithubWorkflowFile(final Path githubWorkflowPath) {
    try {
      return Files.readAllLines(githubWorkflowPath);
    } catch (IOException ex) {
      ProcessUtils.setExceptionCaught(true);
      log.error(
          "Error Reading Github Workflow [{}] of Repository [{}]",
          githubWorkflowPath,
          this.repository.getRepoName());
      return Collections.emptyList();
    }
  }

  private List<String> updateGithubWorkflowFile(final List<String> githubWorkflowContent) {
    if (CollectionUtils.isEmpty(githubWorkflowContent)) {
      return githubWorkflowContent;
    }

    boolean isUpdated = false;
    List<String> updatedGithubWorkflowContent = new ArrayList<>();

    for (String line : githubWorkflowContent) {
      if (line.trim().startsWith("#")) {
        updatedGithubWorkflowContent.add(line);
        continue;
      }

      String updatedLine = updateGithubActions(line);
      updatedLine = updateLanguageVersion(updatedLine);
      updatedGithubWorkflowContent.add(updatedLine);

      if (!line.equals(updatedLine)) {
        isUpdated = true;
      }
    }

    return isUpdated ? updatedGithubWorkflowContent : Collections.emptyList();
  }

  private String updateGithubActions(final String githubActionLine) {
    String currentVersion = "";
    String latestVersion = "";

    if (githubActionLine.contains("actions/checkout")) {
      latestVersion =
          this.latestVersions.getLatestVersionGithubActions().getCheckout().getVersionMajor();
    }
    if (githubActionLine.contains("actions/setup-java")) {
      latestVersion =
          this.latestVersions.getLatestVersionGithubActions().getSetupJava().getVersionMajor();
    }
    if (githubActionLine.contains("gradle/actions/setup-gradle")) {
      latestVersion =
          this.latestVersions.getLatestVersionGithubActions().getSetupGradle().getVersionMajor();
    }
    if (githubActionLine.contains("actions/setup-node")) {
      latestVersion =
          this.latestVersions.getLatestVersionGithubActions().getSetupNode().getVersionMajor();
    }
    if (githubActionLine.contains("actions/setup-python")) {
      latestVersion =
          this.latestVersions.getLatestVersionGithubActions().getSetupPython().getVersionMajor();
    }
    if (githubActionLine.contains("github/codeql-action")) {
      latestVersion =
          this.latestVersions.getLatestVersionGithubActions().getCodeql().getVersionMajor();
    }

    if (StringUtils.hasText(latestVersion)) {
      String[] lineArray = githubActionLine.split("@v");
      if (lineArray.length == 2) {
        currentVersion = lineArray[1];
      } else {
        // set it as latest version so that the line doesn't get updated
        currentVersion = latestVersion;
      }
    }

    if (currentVersion.equals(latestVersion)) {
      return githubActionLine;
    }

    return githubActionLine.replace(currentVersion, latestVersion);
  }

  private String updateLanguageVersion(final String versionLine) {
    String currentVersion = "";
    String latestVersion = getLatestVersion(versionLine);

    if (StringUtils.hasText(latestVersion)) {
      currentVersion = getCurrentVersion(versionLine);

      if (!StringUtils.hasText(currentVersion)) {
        // set it as latest version so that the line doesn't get updated
        currentVersion = latestVersion;
      }
    }

    if (currentVersion.equals(latestVersion)) {
      return versionLine;
    }

    return versionLine.replace(currentVersion, latestVersion);
  }

  private String getLatestVersion(String versionLine) {
    String latestVersion = "";

    if (versionLine.contains("node-version") && !versionLine.contains("matrix.node-version")) {
      latestVersion = this.latestVersions.getLatestVersionLanguages().getNode().getVersionMajor();
    } else if (versionLine.contains("python-version")
        && !versionLine.contains("matrix.python-version")) {
      latestVersion =
          getVersionMajorMinor(
              this.latestVersions.getLatestVersionLanguages().getPython().getVersionFull(), true);
    } else if (versionLine.contains("java-version")
        && !versionLine.contains("matrix.java-version")) {
      latestVersion = this.latestVersions.getLatestVersionLanguages().getJava().getVersionMajor();
    }

    return latestVersion;
  }

  private String getCurrentVersion(final String versionLine) {
    String currentVersion = "";

    if (versionLine.contains("[") && versionLine.contains("]")) {
      // this is a version matrix with multiple values, find out the lowest number among them
      return getLowestCurrentVersionFromMatrix(versionLine);
    } else {
      String[] versionLineArray = versionLine.split(":");

      if (versionLineArray.length == 2) {
        return versionLineArray[1].trim();
      }
    }

    return currentVersion;
  }

  private String getLowestCurrentVersionFromMatrix(final String versionLine) {
    Pattern pattern = Pattern.compile("\\[(.*?)\\]");
    Matcher matcher = pattern.matcher(versionLine);

    if (matcher.find()) {
      final String versions = matcher.group(1);
      if (StringUtils.hasText(versions)) {
        String lowestVersion =
            Arrays.stream(versions.split(","))
                .map(String::trim)
                .min(CommonUtils::compareVersions)
                .orElse(null);
        if (StringUtils.hasText(lowestVersion)) {
          return lowestVersion;
        }
      }
    }

    return "";
  }

  private boolean writeGithubWorkflowFile(
      final Path githubWorkflowPath, final List<String> githubWorkflowContent) {
    if (CollectionUtils.isEmpty(githubWorkflowContent)) {
      return false;
    }

    try {
      Files.write(githubWorkflowPath, githubWorkflowContent, StandardCharsets.UTF_8);
      return true;
    } catch (IOException ex) {
      ProcessUtils.setExceptionCaught(true);
      log.error(
          "Error Writing Updated Github Workflow [{}] of repository: [{}]",
          githubWorkflowPath,
          this.repository.getRepoName());
      return false;
    }
  }
}
