package app.dependency.update.app.runnable;

import static app.dependency.update.app.util.CommonUtils.getVersionMajorMinor;

import app.dependency.update.app.model.LatestVersionsModel;
import app.dependency.update.app.model.Repository;
import app.dependency.update.app.util.CommonUtils;
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
  private final LatestVersionsModel latestVersionsModel;
  private final Path githubWorkflowsFolderPath;

  public ExecuteGithubWorkflowsUpdate(
      final Repository repository, final LatestVersionsModel latestVersionsModel) {
    this.repository = repository;
    this.latestVersionsModel = latestVersionsModel;
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
      log.error("Find Github Actions Files: [{}]", this.githubWorkflowsFolderPath, ex);
      return Collections.emptyList();
    }
  }

  private List<String> readGithubWorkflowFile(final Path githubWorkflowPath) {
    try {
      return Files.readAllLines(githubWorkflowPath);
    } catch (IOException ex) {
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

    for (int i = 0; i < githubWorkflowContent.size(); i++) {
      String line = githubWorkflowContent.get(i);
      if (line.trim().startsWith("#")) {
        updatedGithubWorkflowContent.add(line);
        continue;
      }

      String updatedLine = updateGithubActions(line);
      if (line.equals(updatedLine)) {
        updatedLine = updateLanguageVersion(updatedLine);
      }
      if (line.equals(updatedLine) && i > 0) {
        final String lineMinusOne = githubWorkflowContent.get(i - 1);
        List<String> updatedLines = updateToolVersionFlyway(updatedLine, lineMinusOne);
        updatedLine = updatedLines.getFirst();

        final String updatedLineMinusOne = updatedLines.get(1);
        if (!lineMinusOne.equals(updatedLineMinusOne)) {
          updatedGithubWorkflowContent.set(i - 1, updatedLineMinusOne);
        }
      }

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
          this.latestVersionsModel.getLatestVersionGithubActions().getCheckout().getVersionMajor();
    } else if (githubActionLine.contains("actions/setup-java")) {
      latestVersion =
          this.latestVersionsModel.getLatestVersionGithubActions().getSetupJava().getVersionMajor();
    } else if (githubActionLine.contains("gradle/actions/setup-gradle")) {
      latestVersion =
          this.latestVersionsModel
              .getLatestVersionGithubActions()
              .getSetupGradle()
              .getVersionMajor();
    } else if (githubActionLine.contains("actions/setup-node")) {
      latestVersion =
          this.latestVersionsModel.getLatestVersionGithubActions().getSetupNode().getVersionMajor();
    } else if (githubActionLine.contains("actions/setup-python")) {
      latestVersion =
          this.latestVersionsModel
              .getLatestVersionGithubActions()
              .getSetupPython()
              .getVersionMajor();
    } else if (githubActionLine.contains("github/codeql-action")) {
      latestVersion =
          this.latestVersionsModel.getLatestVersionGithubActions().getCodeql().getVersionMajor();
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
    String latestVersion = getLatestLanguageVersion(versionLine);

    if (StringUtils.hasText(latestVersion)) {
      currentVersion = getCurrentLanguageVersion(versionLine);

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

  private List<String> updateToolVersionFlyway(
      final String versionLine, final String versionLineMinusOne) {
    String currentVersion = "";
    String latestVersion = getLatestToolVersionFlyway(versionLine);

    if (StringUtils.hasText(latestVersion)) {
      currentVersion = getCurrentToolVersionFlyway(versionLine);

      if (!StringUtils.hasText(currentVersion)) {
        // set it as latest version so that the line doesn't get updated
        currentVersion = latestVersion;
      }
    }

    if (currentVersion.equals(latestVersion)) {
      return List.of(versionLine, versionLineMinusOne);
    }

    final String updatedVersionLine = versionLine.replace(currentVersion, latestVersion);
    final String updatedVersionLineMinusOne =
        versionLineMinusOne.replace(currentVersion, latestVersion);
    return List.of(updatedVersionLine, updatedVersionLineMinusOne);
  }

  private String getLatestLanguageVersion(final String versionLine) {
    String latestVersion = "";

    if (versionLine.contains("node-version") && !versionLine.contains("matrix.node-version")) {
      latestVersion =
          this.latestVersionsModel.getLatestVersionLanguages().getNode().getVersionMajor();
    } else if (versionLine.contains("python-version")
        && !versionLine.contains("matrix.python-version")) {
      latestVersion =
          getVersionMajorMinor(
              this.latestVersionsModel.getLatestVersionLanguages().getPython().getVersionFull(),
              true);
    } else if (versionLine.contains("java-version")
        && !versionLine.contains("matrix.java-version")) {
      latestVersion =
          this.latestVersionsModel.getLatestVersionLanguages().getJava().getVersionMajor();
    }

    return latestVersion;
  }

  private String getLatestToolVersionFlyway(final String versionLine) {
    String latestVersion = "";
    if (versionLine.contains("sudo mv flyway-")) {
      latestVersion = this.latestVersionsModel.getLatestVersionTools().getFlyway().getVersionFull();
    }
    return latestVersion;
  }

  private String getCurrentLanguageVersion(final String versionLine) {
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

  private String getCurrentToolVersionFlyway(final String versionLine) {
    return versionLine.replaceAll("[^0-9.]", "");
  }

  private String getLowestCurrentVersionFromMatrix(final String versionLine) {
    Pattern pattern = Pattern.compile("\\[(.*?)]");
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
      log.error(
          "Error Writing Updated Github Workflow [{}] of repository: [{}]",
          githubWorkflowPath,
          this.repository.getRepoName());
      return false;
    }
  }
}
