package app.dependency.update.app.execute;

import app.dependency.update.app.exception.AppDependencyUpdateIOException;
import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import app.dependency.update.app.model.ScriptFile;
import app.dependency.update.app.util.Util;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExecuteScriptFile implements Runnable {
  private final String threadName;
  private final String commandPath;
  private final String scriptPath;
  private final String arguments;
  private final ScriptFile scriptFile;
  private Thread thread;

  public ExecuteScriptFile(String threadName, ScriptFile scriptFile, String arguments) {
    this.threadName = threadName;
    this.scriptFile = scriptFile;
    this.arguments = arguments;
    this.commandPath = Util.COMMAND_PATH;
    this.scriptPath =
        Util.JAVA_SYSTEM_TMPDIR
            + "/"
            + Util.SCRIPTS_DIRECTORY
            + "/"
            + this.scriptFile.getScriptFileName();
  }

  @Override
  public void run() {
    executeScript();
  }

  private void executeScript() {
    try {
      Process processChmod = startProcess(Util.CHMOD_COMMAND + this.scriptPath);
      try (InputStream errorStream = processChmod.getErrorStream()) {
        displayStreamOutput(Util.CHMOD_COMMAND, errorStream, true);
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

    log.info("End execute script...");
  }

  private Process startProcess(String script)
      throws AppDependencyUpdateIOException, AppDependencyUpdateRuntimeException {
    log.info("Starting process: {}", script == null ? this.scriptPath : script);
    try {
      Process process;
      if (script == null) {

        process = new ProcessBuilder(this.commandPath, scriptPath, arguments).start();
      } else {
        process = new ProcessBuilder(this.commandPath, script).start();
      }
      log.info("Waiting process: {}", script == null ? this.scriptPath : script);
      process.waitFor();
      log.info("Finished process: {}", script == null ? this.scriptPath : script);
      return process;
    } catch (IOException | InterruptedException ex) {
      if (ex instanceof IOException) {
        throw new AppDependencyUpdateIOException("Error in Start Process", ex.getCause());
      } else {
        throw new AppDependencyUpdateRuntimeException("Error in Start Process", ex.getCause());
      }
    }
  }

  private void displayStreamOutput(
      String script, final InputStream inputStream, boolean isErrorStream)
      throws AppDependencyUpdateIOException {
    log.info("Display stream output: {}", script);
    String line;
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
      while ((line = reader.readLine()) != null) {
        if (isErrorStream) {
          log.error(line);
        } else {
          log.info(line);
        }
      }
    } catch (IOException ex) {
      throw new AppDependencyUpdateIOException(
          "Error in Display Stream Output: " + script, ex.getCause());
    }
  }

  public Thread start() {
    if (thread == null) {
      thread = new Thread(this, threadName);
      thread.start();
    }
    return thread;
  }
}
