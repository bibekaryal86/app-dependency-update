package app.dependency.update.app.execute;

import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import app.dependency.update.app.model.ScriptFile;
import app.dependency.update.app.util.CommonUtil;
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

  public CreateTempScriptFiles(final List<ScriptFile> scriptFiles) {
    this.scriptFiles = scriptFiles;
    this.scriptsDirectory = CommonUtil.SCRIPTS_DIRECTORY;
    this.tmpdir = CommonUtil.JAVA_SYSTEM_TMPDIR;
  }

  public void createTempScriptFiles() {
    boolean isError = createTempScriptsDirectory();
    if (isError) {
      throw new AppDependencyUpdateRuntimeException(
          "Unable to create temp directory to store scripts...");
    }

    for (final ScriptFile scriptFile : this.scriptFiles) {
      isError = createTempScriptFile(scriptFile);
      if (isError) {
        throw new AppDependencyUpdateRuntimeException(
            String.format(
                "Unable to create and temp script file %s...", scriptFile.getScriptFileName()));
      }
    }
  }

  private boolean createTempScriptsDirectory() {
    Path path = Path.of(this.tmpdir + CommonUtil.PATH_DELIMITER + this.scriptsDirectory);

    try {
      if (!Files.exists(path)) {
        log.info("Creating temp script directory: [ {} ]", path);
        Files.createDirectory(path);
      }
      return false;
    } catch (IOException ex) {
      log.error("Error creating temp script directory", ex);
      return true;
    }
  }

  private boolean createTempScriptFile(final ScriptFile scriptFile) {
    try {
      Path filePath =
          Files.createFile(
              Path.of(
                  this.tmpdir
                      + CommonUtil.PATH_DELIMITER
                      + this.scriptsDirectory
                      + CommonUtil.PATH_DELIMITER
                      + scriptFile.getScriptFileName()));
      try (InputStream inputStream =
          getClass()
              .getClassLoader()
              .getResourceAsStream(this.scriptsDirectory + "/" + scriptFile.getScriptFileName())) {
        assert inputStream != null;
        Files.write(filePath, inputStream.readAllBytes(), StandardOpenOption.WRITE);
        log.info("Written to file: [ {} ]", filePath);
        return false;
      }
    } catch (IOException | NullPointerException ex) {
      log.error("Error creating temp script file: [ {} ]", scriptFile, ex);
      return true;
    }
  }
}
