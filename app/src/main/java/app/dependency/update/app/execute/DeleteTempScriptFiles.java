package app.dependency.update.app.execute;

import app.dependency.update.app.util.Util;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeleteTempScriptFiles {

  public DeleteTempScriptFiles() {
    log.info("Delete Temp Script Files Quiet Cleanup...");

    try {
      Path tempScriptsDirectory = Path.of(Util.JAVA_SYSTEM_TMPDIR + "/" + Util.SCRIPTS_DIRECTORY);
      if (Files.exists(tempScriptsDirectory)) {
        try (Stream<Path> paths = Files.walk(tempScriptsDirectory)) {
          paths.sorted(Comparator.reverseOrder()).forEach(this::delete);
        }
      }
    } catch (IOException ex) {
      log.error("Quiet cleanup error: {}", ex.getMessage(), ex);
    }
  }

  public void delete(Path path) {
    try {
      boolean isDeleted = Files.deleteIfExists(path);
      log.info("Delete: {} | {}", path, isDeleted);
    } catch (IOException ex) {
      log.info("Quiet cleanup delete error: {}", path, ex);
    }
  }
}
