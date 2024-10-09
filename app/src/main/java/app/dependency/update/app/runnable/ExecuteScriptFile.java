package app.dependency.update.app.runnable;

import static app.dependency.update.app.util.CommonUtils.*;
import static app.dependency.update.app.util.ConstantUtils.*;
import static app.dependency.update.app.util.ProcessUtils.*;

import app.dependency.update.app.exception.AppDependencyUpdateIOException;
import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import app.dependency.update.app.model.ScriptFile;
import app.dependency.update.app.util.ProcessUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
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
      ProcessUtils.setExceptionCaught(true);
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
      ProcessUtils.setExceptionCaught(true);
      throw new AppDependencyUpdateIOException("Error in Start Process", ex.getCause());
    } catch (InterruptedException ex) {
      ProcessUtils.setExceptionCaught(true);
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
      ProcessUtils.setExceptionCaught(true);
      throw new AppDependencyUpdateIOException(
          "Error in Process Stream Output: " + ", " + this.scriptPath, ex.getCause());
    }

    boolean isPrCreateCheckRequired = checkPrCreateRequired();
    boolean isPrMergeCheckRequired = checkPrMergeRequired();

    if (isPrCreateCheckRequired) {
      checkRepositoryPrCreateRelated(stringBuilder.toString());
    } else if (isPrMergeCheckRequired) {
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
      List<String> repoNames = findRepoNamesForPrMerge(output);
      if (!isEmpty(repoNames)) {
        for (String repoName : repoNames) {
          updateProcessedRepositoriesToPrMerged(repoName);
        }
      }
    }
  }

  private boolean checkPrCreateRequired() {
    return this.scriptPath.contains(UpdateType.NPM_DEPENDENCIES.toString())
        || this.scriptPath.contains(UpdateType.GRADLE_DEPENDENCIES.toString())
        || this.scriptPath.contains(UpdateType.PYTHON_DEPENDENCIES.toString())
        || this.scriptPath.contains(UpdateType.GITHUB_PR_CREATE.toString());
  }

  private boolean checkPrMergeRequired() {
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

  private List<String> findRepoNamesForPrMerge(final String output) {
    List<String> repoNames = new ArrayList<>();
    String regex = "^Merged PR: (?!.*already merged).*/([^/\\s]+)";
    Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
    Matcher matcher = pattern.matcher(output);
    while (matcher.find()) {
      String matcherGroup = matcher.group(1);
      repoNames.add(matcherGroup);
    }
    return repoNames;
  }
}
