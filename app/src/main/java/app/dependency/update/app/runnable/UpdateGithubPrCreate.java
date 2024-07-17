package app.dependency.update.app.runnable;

import static app.dependency.update.app.util.CommonUtils.*;

import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import app.dependency.update.app.model.AppInitData;
import app.dependency.update.app.model.Repository;
import app.dependency.update.app.model.ScriptFile;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdateGithubPrCreate {
  private final List<Repository> repositories;
  private final ScriptFile scriptFile;
  private final String branchName;

  public UpdateGithubPrCreate(
      final List<Repository> repositories, final AppInitData appInitData, final String branchName) {
    this.repositories = repositories;
    this.scriptFile =
        appInitData.getScriptFiles().stream()
            .filter(sf -> sf.getType().equals(UpdateType.GITHUB_PR_CREATE))
            .findFirst()
            .orElseThrow(
                () ->
                    new AppDependencyUpdateRuntimeException(
                        "Github PR Create Script Not Found..."));
    this.branchName = branchName;
  }

  public void updateGithubPrCreate() {
    List<Thread> threads = new ArrayList<>();
    for (Repository repository : this.repositories) {
      threads.add(executeUpdate(repository));
    }
    threads.forEach(this::join);
  }

  private Thread executeUpdate(final Repository repository) {
    log.debug("Execute Github PR Create on: [ {} ]", repository);
    List<String> arguments = new LinkedList<>();
    arguments.add(repository.getRepoPath().toString());
    arguments.add(branchName);
    return new ExecuteScriptFile(
            threadName(repository, this.getClass().getSimpleName()),
            this.scriptFile,
            arguments,
            repository)
        .start();
  }

  // suppressing sonarlint rule for interrupting thread
  @SuppressWarnings("java:S2142")
  private void join(final Thread thread) {
    try {
      thread.join();
    } catch (InterruptedException ex) {
      log.error("Exception Join Thread", ex);
    }
  }
}
