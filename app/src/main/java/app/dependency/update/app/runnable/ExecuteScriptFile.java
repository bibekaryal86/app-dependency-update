package app.dependency.update.app.runnable;

import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.exception.AppDependencyUpdateIOException;
import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import app.dependency.update.app.model.ScriptFile;
import app.dependency.update.app.util.CommonUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExecuteScriptFile implements Runnable {
  private final String threadName;
  private final String scriptPath;
  private final List<String> arguments;
  private Thread thread;

  public ExecuteScriptFile(
      final String threadName, final ScriptFile scriptFile, final List<String> arguments) {
    this.threadName = threadName;
    this.arguments = arguments;
    this.scriptPath =
        JAVA_SYSTEM_TMPDIR
            + PATH_DELIMITER
            + SCRIPTS_DIRECTORY
            + PATH_DELIMITER
            + scriptFile.getScriptFileName();
  }

  @Override
  public void run() {
    executeScript();
  }

  public Thread start() {
    if (this.thread == null) {
      this.thread = new Thread(this, this.threadName);
      this.thread.start();
    }
    return this.thread;
  }

  private void executeScript() {
    try {
      Process process = startProcess();
      processOutput(process);
    } catch (Exception ex) {
      log.error("Error in Execute Script: ", ex);
    }
  }

  private Process startProcess()
      throws AppDependencyUpdateIOException, AppDependencyUpdateRuntimeException {
    try {
      List<String> command = new LinkedList<>();
      command.add(COMMAND_PATH);
      command.add(this.scriptPath);
      command.addAll(this.arguments);
      Process process = new ProcessBuilder(command).start();
      process.waitFor();
      return process;
    } catch (IOException ex) {
      throw new AppDependencyUpdateIOException("Error in Start Process", ex.getCause());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new AppDependencyUpdateRuntimeException("Error in Start Process", ex.getCause());
    }
  }

  private void processOutput(final Process process) throws AppDependencyUpdateIOException {
    StringBuilder stringBuilder = new StringBuilder();
    String line;

    try (BufferedReader readerError =
        new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
      while ((line = readerError.readLine()) != null) {
        stringBuilder.append("ERROR-- ").append(line).append("\n");
      }

      try (BufferedReader readerInput =
          new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        while ((line = readerInput.readLine()) != null) {
          stringBuilder.append(line).append("\n");
        }
      }

      log.info("Process output: [ {} ]\n{}", this.scriptPath, stringBuilder);
    } catch (IOException ex) {
      throw new AppDependencyUpdateIOException(
          "Error in Display Stream Output: " + ", " + this.scriptPath, ex.getCause());
    }

    checkPrCreationError(stringBuilder);
  }

  private void checkPrCreationError(StringBuilder stringBuilder) {
    if ((this.scriptPath.contains("NPM_DEPENDENCIES")
            || this.scriptPath.contains("GRADLE_DEPENDENCIES"))
        && (stringBuilder.toString().contains("pull request create failed"))) {
      CommonUtils.addRepositoriesWithPrError(this.threadName.split("--")[0]);
    } else if (this.scriptPath.contains("GITHUB_PR_CREATE")) {
      CommonUtils.removeRepositoriesWithPrError(this.threadName.split("--")[0]);
    }
  }
}
