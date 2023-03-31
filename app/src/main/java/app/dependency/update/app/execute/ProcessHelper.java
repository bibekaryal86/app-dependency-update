package app.dependency.update.app.execute;

import app.dependency.update.app.exception.AppDependencyUpdateIOException;
import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessHelper {

  private final String scriptPath;
  private final String script;
  private final InputStream inputStream;
  private final String commandPath;
  private final List<String> arguments;

  public ProcessHelper(
      final String scriptPath,
      final String script,
      final String commandPath,
      final List<String> arguments) {
    this.scriptPath = scriptPath;
    this.script = script;
    this.inputStream = null;
    this.commandPath = commandPath;
    this.arguments = arguments;
  }

  public ProcessHelper(final String script, final InputStream inputStream) {
    this.script = script;
    this.inputStream = inputStream;
    this.scriptPath = null;
    this.commandPath = null;
    this.arguments = null;
  }

  public Process startProcess()
      throws AppDependencyUpdateIOException, AppDependencyUpdateRuntimeException {
    log.info("Starting process: {}", this.script == null ? this.scriptPath : this.script);
    try {
      Process process;
      if (this.script == null) {
        assert this.arguments != null;
        List<String> command = new LinkedList<>();
        command.add(this.commandPath);
        command.add(this.scriptPath);
        command.addAll(this.arguments);
        process = new ProcessBuilder(command).start();
      } else {
        process = new ProcessBuilder(this.commandPath, this.script).start();
      }
      log.info("Waiting process: {}", this.script == null ? this.scriptPath : this.script);
      process.waitFor();
      log.info("Finished process: {}", this.script == null ? this.scriptPath : this.script);
      try (InputStream errorStream = process.getErrorStream()) {
        displayStreamOutput(this.scriptPath, errorStream);
      }
      try (InputStream inputStream = process.getInputStream()) {
        displayStreamOutput(this.scriptPath, inputStream);
      }



      return process;
    } catch (IOException ex) {
      throw new AppDependencyUpdateIOException("Error in Start Process", ex.getCause());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new AppDependencyUpdateRuntimeException("Error in Start Process", ex.getCause());
    }
  }

  public void displayStreamOutput(final String script, final InputStream inputStream) throws AppDependencyUpdateIOException {
    log.info("Display stream output: {}", script);
    StringBuilder stringBuilder = new StringBuilder();
    String line;
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
      while ((line = reader.readLine()) != null) {
        stringBuilder.append(line).append("\n");
      }

      log.info("Display stream output: {}\n{}\n", script, stringBuilder);
    } catch (IOException ex) {
      throw new AppDependencyUpdateIOException(
          "Error in Display Stream Output: " + this.script, ex.getCause());
    }
  }
}
