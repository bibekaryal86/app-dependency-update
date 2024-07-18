package app.dependency.update.app.runnable;

import static app.dependency.update.app.util.CommonUtils.*;
import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.exception.AppDependencyUpdateIOException;
import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import app.dependency.update.app.model.ScriptFile;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExecuteScriptFile implements Runnable {
  private final String threadName;
  private final String scriptPath;
  private final List<String> arguments;
  private Thread thread;

  public ExecuteScriptFile(
      final String threadName, final ScriptFile scriptFile, final List<String> arguments) {
    this.threadName = threadName;
    this.arguments = arguments;
    this.scriptPath =
        JAVA_SYSTEM_TMPDIR
            + PATH_DELIMITER
            + SCRIPTS_DIRECTORY
            + PATH_DELIMITER
            + scriptFile.getScriptFileName();
  }

  @Override
  public void run() {
    executeScript();
  }

  public Thread start() {
    if (this.thread == null) {
      this.thread = new Thread(this, this.threadName);
      this.thread.start();
    }
    return this.thread;
  }

  private void executeScript() {
    try {
      Process process = startProcess();
      processOutput(process);
    } catch (Exception ex) {
      log.error("Error in Execute Script: ", ex);
    }
  }

  private Process startProcess()
      throws AppDependencyUpdateIOException, AppDependencyUpdateRuntimeException {
    try {
      List<String> command = new LinkedList<>();
      command.add(COMMAND_PATH);
      command.add(this.scriptPath);
      command.addAll(this.arguments);
      Process process = new ProcessBuilder(command).start();
      process.waitFor();
      return process;
    } catch (IOException ex) {
      throw new AppDependencyUpdateIOException("Error in Start Process", ex.getCause());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new AppDependencyUpdateRuntimeException("Error in Start Process", ex.getCause());
    }
  }

  private void processOutput(final Process process) throws AppDependencyUpdateIOException {
    StringBuilder stringBuilder = new StringBuilder();
    String line;

    try (BufferedReader readerError =
        new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
      while ((line = readerError.readLine()) != null) {
        stringBuilder.append("ERROR-- ").append(line).append("\n");
      }

      try (BufferedReader readerInput =
          new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        while ((line = readerInput.readLine()) != null) {
          stringBuilder.append(line).append("\n");
        }
      }

      log.debug("Process output: [ {} ]\n{}", this.scriptPath, stringBuilder);
    } catch (IOException ex) {
      throw new AppDependencyUpdateIOException(
          "Error in Process Stream Output: " + ", " + this.scriptPath, ex.getCause());
    }

    boolean isCheckPrCreateRelatedRequired = isCheckPrCreateRequired();
    boolean isCheckPrMergeRelatedRequired = isCheckPrMergeRequired();

    if (isCheckPrCreateRelatedRequired) {
      checkRepositoryPrCreateRelated(stringBuilder.toString());
    } else if (isCheckPrMergeRelatedRequired) {
      checkRepositoryPrMergeRelated(stringBuilder.toString());
    }
  }

  private void checkRepositoryPrCreateRelated(final String output) {
    String repoName = this.threadName.split("--")[0];
    boolean isPrCreateAttempted = checkPrCreateAttempted(output);
    boolean isPrCreateError = checkPrCreationError(output, repoName);
    addProcessedRepositories(repoName, isPrCreateAttempted, isPrCreateError);
  }

  private void checkRepositoryPrMergeRelated(final String output) {
    boolean isPrMerged = checkPrMerged(output);
    if (isPrMerged) {
      // GITHUB_MERGE does not include repository name in threadName
      String repoName = findRepoNameForPrMerge(output);
      if (!isEmpty(repoName)) {
        updateProcessedRepositoriesToPrMerged(repoName);
      }
    }
  }

  private boolean isCheckPrCreateRequired() {
    return this.scriptPath.contains(UpdateType.NPM_DEPENDENCIES.toString())
        || this.scriptPath.contains(UpdateType.GRADLE_DEPENDENCIES.toString())
        || this.scriptPath.contains(UpdateType.PYTHON_DEPENDENCIES.toString())
        || this.scriptPath.contains(UpdateType.GITHUB_PR_CREATE.toString());
  }

  private boolean isCheckPrMergeRequired() {
    return this.scriptPath.contains(UpdateType.GITHUB_MERGE.toString());
  }

  private boolean checkPrCreateAttempted(final String output) {
    return output.contains("Creating PR");
  }

  private boolean checkPrCreationError(final String output, final String repoName) {
    if (output.contains("pull request create failed")) {
      log.info("Pull Request Create Failed: [ {} ]", this.threadName);
      addRepositoriesWithPrError(repoName);
      return true;
    } else {
      removeRepositoriesWithPrError(repoName);
    }
    return false;
  }

  private boolean checkPrMerged(final String output) {
    return output.contains("Merged PR");
  }

  private String findRepoNameForPrMerge(final String output) {
    String regex = "Merging PR: .*/([^/\\s]+)";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(output);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return null;
  }
}
