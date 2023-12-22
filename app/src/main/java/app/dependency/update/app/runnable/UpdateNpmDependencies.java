package app.dependency.update.app.runnable;

import static app.dependency.update.app.util.CommonUtils.*;
import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import app.dependency.update.app.model.AppInitData;
import app.dependency.update.app.model.Repository;
import app.dependency.update.app.model.ScriptFile;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdateNpmDependencies {
  private final List<Repository> repositories;
  private final ScriptFile scriptFile;

  public UpdateNpmDependencies(final AppInitData appInitData) {
    this.repositories =
        appInitData.getRepositories().stream()
            .filter(repository -> repository.getType().equals(UpdateType.NPM_DEPENDENCIES))
            .toList();
    this.scriptFile =
        appInitData.getScriptFiles().stream()
            .filter(sf -> sf.getType().equals(UpdateType.NPM_DEPENDENCIES))
            .findFirst()
            .orElseThrow(
                () ->
                    new AppDependencyUpdateRuntimeException(
                        "NPM Dependencies Script Not Found..."));
  }

  public void updateNpmDependencies() {
    List<Thread> threads = new ArrayList<>();
    for (Repository repository : this.repositories) {
      threads.add(executeUpdate(repository));
    }
    threads.forEach(this::join);
  }

  private Thread executeUpdate(final Repository repository) {
    log.debug("Execute NPM Dependencies Update on: [ {} ]", repository);
    List<String> arguments = new LinkedList<>();
    arguments.add(repository.getRepoPath().toString());
    arguments.add(String.format(BRANCH_UPDATE_DEPENDENCIES, LocalDate.now()));
    return new ExecuteScriptFile(
            threadName(repository, this.getClass().getSimpleName()), this.scriptFile, arguments)
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
