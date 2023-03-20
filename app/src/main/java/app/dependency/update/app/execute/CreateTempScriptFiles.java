package app.dependency.update.app.execute;

import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import app.dependency.update.app.model.ScriptFile;
import app.dependency.update.app.util.Util;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateTempScriptFiles {
  private final String scriptsDirectory;
  private final String tmpdir;
  private final List<ScriptFile> scriptFiles;

  public CreateTempScriptFiles(List<ScriptFile> scriptFiles) {
    this.scriptFiles = scriptFiles;
    this.scriptsDirectory = Util.SCRIPTS_DIRECTORY;
    this.tmpdir = Util.JAVA_SYSTEM_TMPDIR;
  }

  public void createTempScriptFiles() {
    boolean isError = createTempScriptsDirectory();
    if (isError) {
      throw new AppDependencyUpdateRuntimeException(
          "Unable to create temp directory to store scripts...");
    }

    for (ScriptFile scriptFile : this.scriptFiles) {
      isError = createTempScriptFile(scriptFile);
      if (isError) {
        throw new AppDependencyUpdateRuntimeException(
            String.format(
                "Unable to create and temp script file %s...", scriptFile.getScriptFileName()));
      }
    }
  }

  private boolean createTempScriptsDirectory() {
    Path path = Path.of(this.tmpdir + Util.PATH_DELIMITER + this.scriptsDirectory);

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
      Path filePath =
          Files.createFile(
              Path.of(
                  this.tmpdir
                      + Util.PATH_DELIMITER
                      + this.scriptsDirectory
                      + Util.PATH_DELIMITER
                      + scriptFile.getScriptFileName()));
      try (InputStream inputStream =
          getClass()
              .getClassLoader()
              .getResourceAsStream(this.scriptsDirectory + "/" + scriptFile.getScriptFileName())) {
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
}
