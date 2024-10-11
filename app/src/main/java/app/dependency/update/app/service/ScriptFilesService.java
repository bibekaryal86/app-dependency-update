package app.dependency.update.app.service;

import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import app.dependency.update.app.model.ScriptFile;
import app.dependency.update.app.util.AppInitDataUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ScriptFilesService {
  private static final String TEMP_SCRIPTS_DIRECTORY =
      JAVA_SYSTEM_TMPDIR + PATH_DELIMITER + SCRIPTS_DIRECTORY;
  private final Path tempScriptsDirectoryPath = Path.of(TEMP_SCRIPTS_DIRECTORY);

  public void deleteTempScriptFiles() {
    log.info("Delete Temp Script Files...");

    try {
      if (Files.exists(this.tempScriptsDirectoryPath)) {
        try (Stream<Path> paths = Files.walk(tempScriptsDirectoryPath)) {
          paths.sorted(Comparator.reverseOrder()).forEach(this::delete);
        }
      }
    } catch (IOException ex) {
      log.error("ERROR Delete Temp Script Files: [ {} ]", ex.getMessage(), ex);
    }
  }

  public void createTempScriptFiles() {
    boolean isError = createTempScriptsDirectory();
    if (isError) {
      throw new AppDependencyUpdateRuntimeException(
          "Unable to create temp directory to store scripts...");
    }

    List<ScriptFile> scriptFiles = AppInitDataUtils.appInitData().getScriptFiles();
    for (final ScriptFile scriptFile : scriptFiles) {
      isError = createTempScriptFile(scriptFile);
      if (isError) {
        throw new AppDependencyUpdateRuntimeException(
            String.format(
                "Unable to create and temp script file %s...", scriptFile.getScriptFileName()));
      } else {
        giveExecutePermissionToFile(scriptFile);
      }
    }
  }

  public boolean isScriptFilesMissingInFileSystem() {
    try {
      if (!Files.exists(this.tempScriptsDirectoryPath)) {
        return true;
      }

      List<ScriptFile> scriptFiles = AppInitDataUtils.appInitData().getScriptFiles();
      for (final ScriptFile scriptFile : scriptFiles) {
        Path filePath =
            Path.of(TEMP_SCRIPTS_DIRECTORY + PATH_DELIMITER + scriptFile.getScriptFileName());
        if (!Files.exists(filePath)) {
          return true;
        }
      }
    } catch (AppDependencyUpdateRuntimeException ex) {
      log.error("Error checking if Script files exist in Directory", ex);
      return true;
    }
    return false;
  }

  private void delete(final Path path) {
    try {
      boolean isDeleted = Files.deleteIfExists(path);
      log.info("Delete: [ {} ] | [ {} ]", path, isDeleted);
    } catch (IOException ex) {
      log.info("ERROR Delete: [ {} ]", path, ex);
    }
  }

  private boolean createTempScriptsDirectory() {
    try {
      if (!Files.exists(this.tempScriptsDirectoryPath)) {
        log.debug("Creating temp script directory: [ {} ]", this.tempScriptsDirectoryPath);
        Files.createDirectory(this.tempScriptsDirectoryPath);
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
              Path.of(TEMP_SCRIPTS_DIRECTORY + PATH_DELIMITER + scriptFile.getScriptFileName()));
      try (InputStream inputStream =
          getClass()
              .getClassLoader()
              .getResourceAsStream(
                  SCRIPTS_DIRECTORY + PATH_DELIMITER + scriptFile.getScriptFileName())) {
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

  private void giveExecutePermissionToFile(final ScriptFile scriptFile) {
    try {
      String scriptPath =
          JAVA_SYSTEM_TMPDIR
              + PATH_DELIMITER
              + SCRIPTS_DIRECTORY
              + PATH_DELIMITER
              + scriptFile.getScriptFileName();
      new ProcessBuilder(COMMAND_PATH, CHMOD_COMMAND + scriptPath).start();
    } catch (IOException ex) {
      log.error(
          "Error on Give Execute Permission to File: [ {} ]", scriptFile.getScriptFileName(), ex);
    }
  }
}
