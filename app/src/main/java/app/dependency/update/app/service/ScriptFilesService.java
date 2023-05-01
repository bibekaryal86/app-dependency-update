package app.dependency.update.app.service;

import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import app.dependency.update.app.model.ScriptFile;
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
  private final List<ScriptFile> scriptFiles;

  public ScriptFilesService(final AppInitDataService appInitDataService) {
    this.scriptFiles = appInitDataService.appInitData().getScriptFiles();
  }

  public void deleteTempScriptFilesBegin() {
    deleteTempScriptFiles("begin");
  }

  public void deleteTempScriptFilesEnd() {
    deleteTempScriptFiles("end");
  }

  public void deleteTempScriptFiles(String beginEnd) {
    log.info("Delete Temp Script Files: [ {} ]", beginEnd);

    try {
      Path tempScriptsDirectory = Path.of(JAVA_SYSTEM_TMPDIR + PATH_DELIMITER + SCRIPTS_DIRECTORY);
      if (Files.exists(tempScriptsDirectory)) {
        try (Stream<Path> paths = Files.walk(tempScriptsDirectory)) {
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

    for (final ScriptFile scriptFile : this.scriptFiles) {
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

  private void delete(final Path path) {
    try {
      boolean isDeleted = Files.deleteIfExists(path);
      log.info("Delete: [ {} ] | [ {} ]", path, isDeleted);
    } catch (IOException ex) {
      log.info("ERROR Delete: [ {} ]", path, ex);
    }
  }

  private boolean createTempScriptsDirectory() {
    Path path = Path.of(JAVA_SYSTEM_TMPDIR + PATH_DELIMITER + SCRIPTS_DIRECTORY);

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
                  JAVA_SYSTEM_TMPDIR
                      + PATH_DELIMITER
                      + SCRIPTS_DIRECTORY
                      + PATH_DELIMITER
                      + scriptFile.getScriptFileName()));
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
