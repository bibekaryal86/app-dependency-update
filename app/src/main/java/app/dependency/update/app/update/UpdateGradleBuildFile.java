package app.dependency.update.app.update;

import app.dependency.update.app.helper.ExecuteScriptFile;
import app.dependency.update.app.helper.ModifyBuildGradle;
import app.dependency.update.app.helper.ReadBuildGradle;
import app.dependency.update.app.model.BuildGradleConfigs;
import app.dependency.update.app.model.Repository;
import app.dependency.update.app.model.ScriptFile;
import app.dependency.update.app.util.CommonUtil;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdateGradleBuildFile implements Runnable {
  private final String threadName;
  private final Repository repository;
  private final ScriptFile scriptFile;
  private final List<String> arguments;
  private Thread thread;

  public UpdateGradleBuildFile(
      final Repository repository, final ScriptFile scriptFile, final List<String> arguments) {
    this.repository = repository;
    this.scriptFile = scriptFile;
    this.threadName = repository.getRepoName();
    this.arguments = arguments;
  }

  @Override
  public void run() {
    executeBuildGradleUpdate();
  }

  public void start() {
    if (thread == null) {
      thread = new Thread(this, threadName);
      thread.start();
    }
  }

  private void executeBuildGradleUpdate() {
    try {
      List<String> gradleModules = this.repository.getGradleModules();
      for (String gradleModule : gradleModules) {
        log.info(
            "Update Gradle Build File for Module: [ {} ] [ {} ]",
            this.repository.getRepoName(),
            gradleModule);
        BuildGradleConfigs buildGradleConfigs =
            new ReadBuildGradle(this.repository, gradleModule).readBuildGradle();
        if (buildGradleConfigs == null) {
          log.error("Build Gradle Configs is null: [ {} ]", this.repository.getRepoPath());
        } else {
          List<String> buildGradleContent =
              new ModifyBuildGradle(buildGradleConfigs).modifyBuildGradle();

          if (CommonUtil.isEmpty(buildGradleContent)) {
            log.info("Build Gradle Configs not updated: [ {} ]", this.repository.getRepoPath());
          } else {
            boolean isWriteToFile =
                writeToFile(buildGradleConfigs.getBuildGradlePath(), buildGradleContent);

            if (isWriteToFile) {
              new ExecuteScriptFile(threadName + "_", this.scriptFile, this.arguments).start();
            } else {
              log.info(
                  "Build Gradle Changes Not Written to File: [ {} ]",
                  this.repository.getRepoPath());
            }
          }
        }
      }

    } catch (Exception ex) {
      log.error("Error in Execute Build Gradle Update: ", ex);
    }
  }

  private boolean writeToFile(final Path buildGradlePath, final List<String> buildGradleContent) {
    try {
      log.info("Writing to build.gradle file: [ {} ]", buildGradlePath);
      Files.write(buildGradlePath, buildGradleContent, StandardCharsets.UTF_8);
      return true;
    } catch (IOException ex) {
      log.error("Error Saving Updated Build Gradle File: [ {} ]", buildGradlePath, ex);
      return false;
    }
  }
}
