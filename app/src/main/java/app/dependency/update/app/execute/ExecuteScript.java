package app.dependency.update.app.execute;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import app.dependency.update.app.model.ScriptFile;
import app.dependency.update.app.util.Util;
import lombok.extern.slf4j.Slf4j;
import app.dependency.update.app.exception.AppDependencyUpdateIOException;
import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;

@Slf4j
public class ExecuteScript implements Runnable {
  private final String threadName;
  private final String commandPath;
  private final String scriptsFolder;
  private final String tmpdir;
  private final String scriptPath;
  private final Map<String, String> argsMap;
  private final ScriptFile scriptFile;
  private Thread thread;

  public ExecuteScript(String threadName, Map<String, String> argsMap, ScriptFile scriptFile) {
    this.threadName = threadName;
    this.argsMap = argsMap;
    this.scriptFile = scriptFile;
    this.commandPath = Util.COMMAND_PATH;
    this.scriptsFolder = Util.SCRIPTS_FOLDER;
    this.tmpdir = System.getProperty("java.io.tmpdir");
    this.scriptPath =
        this.tmpdir + "/" + this.scriptsFolder + "/" + this.scriptFile.getScriptFileName();
  }

  @Override
  public void run() {
    executeScript();
  }

  private void executeScript() {
    log.info("Begin execute script: {} | {}", threadName, scriptFile);

    try {
      quietCleanup();
      createTempScriptFile();
      startProcess(this.commandPath, "chmod +x " + this.scriptPath, null);
      try (InputStream inputStream = startProcess(this.commandPath, null, this.scriptPath)) {
        displayStreamOutput(inputStream);
      }
      quietCleanup();
    } catch (Exception e) {
      log.error("Error in Execute Script: ", e);
    }

    log.info("End execute script...");
  }

  private InputStream startProcess(String commandPath, String script, String scriptPath)
      throws AppDependencyUpdateIOException, AppDependencyUpdateRuntimeException {
    log.info("Starting process: {} | {} | {}", commandPath, script, scriptPath);
    try {
      Process process;
      if (script == null) {
        String repoHome = this.argsMap.get(Util.PARAM_REPO_HOME);
        process = new ProcessBuilder(commandPath, scriptPath, repoHome).start();
      } else {
        process = new ProcessBuilder(commandPath, script).start();
      }
      log.info("Wait for process: {} | {} | {}", commandPath, script, scriptPath);
      process.waitFor();
      log.info("Finished process: {} | {} | {}", commandPath, script, scriptPath);
      return process.getInputStream();
    } catch (IOException | InterruptedException ex) {
      if (ex instanceof IOException) {
        throw new AppDependencyUpdateIOException("Error in Start Process", ex.getCause());
      } else {
        throw new AppDependencyUpdateRuntimeException("Error in Start Process", ex.getCause());
      }
    }
  }

  private void createTempScriptFile() throws AppDependencyUpdateIOException {
    try {
      log.info("Creating temp script file...");
      Path scriptsFolderPath = Path.of(this.tmpdir + "/" + this.scriptsFolder);
      if (Files.exists(scriptsFolderPath)) {
        log.info("Existing directory: {}", scriptsFolderPath);
      } else {
        log.info("Creating temp script directory...");
        Path tempDirectory = Files.createDirectory(scriptsFolderPath);
        log.info("Created directory: {}", tempDirectory);
      }
      Path tempFile = Files.createFile(Path.of(this.scriptPath));
      log.info("Created file: {}", tempFile);
      InputStream inputStream =
          getClass()
              .getClassLoader()
              .getResourceAsStream(
                  this.scriptsFolder + "/" + this.scriptFile.getOriginalFileName());
      assert inputStream != null;
      Path pathFile = Files.write(tempFile, inputStream.readAllBytes(), StandardOpenOption.WRITE);
      log.info("Written to file: {}", pathFile);
    } catch (IOException ex) {
      throw new AppDependencyUpdateIOException("Error in Create Temp Script File", ex.getCause());
    }
  }

  private void quietCleanup() {
    try {
      log.info("Deleting temp script file if exists...");
      boolean isFileDeleted = Files.deleteIfExists(Path.of(this.scriptPath));
      log.info("Deleted temp script file if exists: {}", isFileDeleted);
      boolean isFolderDeleted =
          Files.deleteIfExists(Path.of(this.tmpdir + "/" + this.scriptsFolder));
      log.info("Deleted temp script folder if exists: {}", isFolderDeleted);
    } catch (Exception ex) {
      log.error("Quiet cleanup error: ", ex);
    }
  }

  private void displayStreamOutput(final InputStream stdoutInputStream)
      throws AppDependencyUpdateIOException {
    log.info("Display stream output...");
    String line;
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(stdoutInputStream))) {
      while ((line = reader.readLine()) != null) {
        log.info(line);
      }
    } catch (IOException ex) {
      throw new AppDependencyUpdateIOException("Error in Display Stream Output", ex.getCause());
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
