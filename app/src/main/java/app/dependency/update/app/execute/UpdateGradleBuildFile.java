package app.dependency.update.app.execute;

import app.dependency.update.app.model.BuildGradleConfigs;
import app.dependency.update.app.model.Repository;
import app.dependency.update.app.model.ScriptFile;
import app.dependency.update.app.util.CommonUtil;
import java.io.IOException;
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

  private void executeBuildGradleUpdate() {
    try {
      BuildGradleConfigs buildGradleConfigs =
          new ReadBuildGradle(this.repository).readBuildGradle();
      if (buildGradleConfigs == null) {
        log.error("Build Gradle Configs is null: [{}]", this.repository.getRepoPath());
      } else {
        List<String> buildGradleContent =
            new ModifyBuildGradle(buildGradleConfigs).modifyBuildGradle();

        if (CommonUtil.isEmpty(buildGradleContent)) {
          log.info("Build Gradle Configs not updated: [{}]", this.repository.getRepoPath());
        } else {
          boolean isWriteToFile =
              writeToFile(buildGradleConfigs.getBuildGradlePath(), buildGradleContent);

          if (isWriteToFile) {
            new ExecuteScriptFile(threadName + "_Execute", this.scriptFile, this.arguments).start();
          } else {
            log.info(
                "Build Gradle Changes Not Written to File: [{}]", this.repository.getRepoPath());
          }
        }
      }
    } catch (Exception e) {
      log.error("Error in Execute Build Gradle Update: ", e);
    }
  }

  private boolean writeToFile(Path buildGradlePath, List<String> buildGradleContent) {
    try {
      log.info("Writing to build.gradle file: [{}]", buildGradlePath);
      Files.write(buildGradlePath, buildGradleContent, java.nio.charset.StandardCharsets.UTF_8);
      return true;
    } catch (IOException ex) {
      log.error("Error Saving Updated Build Gradle File: [{}]", buildGradlePath, ex);
      return false;
    }
  }

  public void start() {
    if (thread == null) {
      thread = new Thread(this, threadName);
      thread.start();
    }
  }
}
