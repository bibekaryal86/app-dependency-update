package app.dependency.update.app.execute;

import app.dependency.update.app.exception.AppDependencyUpdateIOException;
import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import app.dependency.update.app.model.ScriptFile;
import app.dependency.update.app.util.Util;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

@Slf4j
public class CreateTempScriptFiles implements Runnable {
  private final String threadName;
  private final String scriptsDirectory;
  private final String tmpdir;
  private final List<ScriptFile> scriptFiles;
  private Thread thread;

  public CreateTempScriptFiles(String threadName, List<ScriptFile> scriptFiles) {
    this.threadName = threadName;
    this.scriptFiles = scriptFiles;
    this.scriptsDirectory = Util.SCRIPTS_DIRECTORY;
    this.tmpdir = Util.JAVA_SYSTEM_TMPDIR;
  }

  @Override
  public void run() {
    try {
      createTempScriptFiles();
    } catch (AppDependencyUpdateIOException e) {
      throw new AppDependencyUpdateRuntimeException(e.getMessage());
    }
  }

  private void createTempScriptFiles() throws AppDependencyUpdateIOException {
    boolean isError = createTempScriptsDirectory();
    if (isError) {
      throw new AppDependencyUpdateIOException("Unable to create temp directory to store scripts...");
    }

    for (ScriptFile scriptFile : this.scriptFiles) {
      isError = createTempScriptFile(scriptFile);
      if (isError) {
        throw new AppDependencyUpdateIOException(String.format("Unable to create and temp script file %s...", scriptFile.getScriptFileName()));
      }
    }
  }

  private boolean createTempScriptsDirectory() {
    Path path = Path.of(this.tmpdir + "/" + this.scriptsDirectory);

    try {
      if (!Files.exists(path)) {
        log.info("Creating temp script directory: {}", path);
        Files.createDirectory(path);
      }
      return false;
    } catch (IOException ex) {
      log.error("Error creating temp script directory", ex);
      return true;
    }
  }

  private boolean createTempScriptFile(ScriptFile scriptFile) {
    try {
      Path filePath = Files.createFile(Path.of(this.tmpdir + "/" + this.scriptsDirectory + "/" + scriptFile.getScriptFileName()));
      try (InputStream inputStream =
                   getClass()
                           .getClassLoader()
                           .getResourceAsStream(
                                   this.scriptsDirectory + "/" + scriptFile.getScriptFileName())) {
        assert inputStream != null;
        Files.write(filePath, inputStream.readAllBytes(), StandardOpenOption.WRITE);
        log.info("Written to file: {}", filePath);
        return false;
      }
    } catch (IOException | NullPointerException ex) {
      log.error("Error creating temp script file: {}", scriptFile, ex);
      return true;
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
