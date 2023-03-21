package app.dependency.update.app.execute;

import app.dependency.update.app.exception.AppDependencyUpdateIOException;
import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import app.dependency.update.app.model.ScriptFile;
import app.dependency.update.app.util.CommonUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExecuteScriptFile implements Runnable {
  private final String threadName;
  private final String commandPath;
  private final String scriptPath;
  private final List<String> arguments;
  private Thread thread;

  public ExecuteScriptFile(String threadName, ScriptFile scriptFile, List<String> arguments) {
    this.threadName = threadName;
    this.arguments = arguments;
    this.commandPath = CommonUtil.COMMAND_PATH;
    this.scriptPath =
        CommonUtil.JAVA_SYSTEM_TMPDIR
            + CommonUtil.PATH_DELIMITER
            + CommonUtil.SCRIPTS_DIRECTORY
            + CommonUtil.PATH_DELIMITER
            + scriptFile.getScriptFileName();
  }

  @Override
  public void run() {
    executeScript();
  }

  private void executeScript() {
    try {
      Process processChmod = startProcess(CommonUtil.CHMOD_COMMAND + this.scriptPath);
      try (InputStream errorStream = processChmod.getErrorStream()) {
        displayStreamOutput(CommonUtil.CHMOD_COMMAND, errorStream, true);
      }

      Process processExecuteScript = startProcess(null);
      try (InputStream errorStream = processExecuteScript.getErrorStream()) {
        displayStreamOutput(this.scriptPath, errorStream, true);
      }
      try (InputStream inputStream = processExecuteScript.getInputStream()) {
        displayStreamOutput(this.scriptPath, inputStream, false);
      }
    } catch (Exception e) {
      log.error("Error in Execute Script: ", e);
    }
  }

  private Process startProcess(String script)
      throws AppDependencyUpdateIOException, AppDependencyUpdateRuntimeException {
    log.info("Starting process: {}", script == null ? this.scriptPath : script);
    try {
      Process process;
      if (script == null) {
        List<String> command = new LinkedList<>();
        command.add(this.commandPath);
        command.add(this.scriptPath);
        command.addAll(this.arguments);
        process = new ProcessBuilder(command).start();
      } else {
        process = new ProcessBuilder(this.commandPath, script).start();
      }
      log.info("Waiting process: {}", script == null ? this.scriptPath : script);
      process.waitFor();
      log.info("Finished process: {}", script == null ? this.scriptPath : script);
      return process;
    } catch (IOException ex) {
      throw new AppDependencyUpdateIOException("Error in Start Process", ex.getCause());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new AppDependencyUpdateRuntimeException("Error in Start Process", ex.getCause());
    }
  }

  private void displayStreamOutput(
      String script, final InputStream inputStream, boolean isErrorStream)
      throws AppDependencyUpdateIOException {
    log.info("Display stream output: {}", script);
    StringBuilder stringBuilder = new StringBuilder();
    String line;
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
      while ((line = reader.readLine()) != null) {
        stringBuilder.append(line).append("\n");
      }

      if (isErrorStream) {
        log.error("Display stream output: {}\n{}\n", script, stringBuilder);
      } else {
        log.info("Display stream output: {}\n{}\n", script, stringBuilder);
      }
    } catch (IOException ex) {
      throw new AppDependencyUpdateIOException(
          "Error in Display Stream Output: " + script, ex.getCause());
    }
  }

  public void start() {
    if (thread == null) {
      thread = new Thread(this, threadName);
      thread.start();
    }
  }
}
