package app.dependency.update.app.execute;

import app.dependency.update.app.util.Util;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Slf4j
public class DeleteTempScriptFiles implements Runnable {
  private final String threadName;
  private final String tempScriptsDirectory;
  private Thread thread;

  public DeleteTempScriptFiles(String threadName) {
    this.threadName = threadName;
    this.tempScriptsDirectory = Util.JAVA_SYSTEM_TMPDIR + "/" + Util.SCRIPTS_DIRECTORY;
  }

  @Override
  public void run() {
      quietCleanup();
  }

  private void quietCleanup() {
    try {
      try (Stream<Path> paths = Files.walk(Path.of(this.tempScriptsDirectory), 1)) {
          paths.forEach(path -> {
            try {
              Files.deleteIfExists(path);
            } catch (IOException ex) {
              log.error("Quiet cleanup error file: {} | {}", path, ex.getMessage());
            }
          });
      }
      Files.deleteIfExists(Path.of(this.tempScriptsDirectory));
    } catch (IOException ex) {
      log.error("Quiet cleanup error: {}", ex.getMessage());
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
