package app.dependency.update.app.runnable;

import static app.dependency.update.app.util.CommonUtils.*;
import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import app.dependency.update.app.model.AppInitData;
import app.dependency.update.app.model.LatestVersions;
import app.dependency.update.app.model.Repository;
import app.dependency.update.app.model.ScriptFile;
import app.dependency.update.app.service.MongoRepoService;
import app.dependency.update.app.util.ProcessUtils;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdatePythonDependencies {
  private final LatestVersions latestVersions;
  private final List<Repository> repositories;
  private final ScriptFile scriptFile;
  private final MongoRepoService mongoRepoService;

  public UpdatePythonDependencies(
      final AppInitData appInitData, final MongoRepoService mongoRepoService) {
    this.latestVersions = appInitData.getLatestVersions();
    this.repositories =
        appInitData.getRepositories().stream()
            .filter(repository -> repository.getType().equals(UpdateType.PYTHON_DEPENDENCIES))
            .toList();
    this.scriptFile =
        appInitData.getScriptFiles().stream()
            .filter(sf -> sf.getType().equals(UpdateType.PYTHON_DEPENDENCIES))
            .findFirst()
            .orElseThrow(
                () ->
                    new AppDependencyUpdateRuntimeException(
                        "Python Dependencies Script Not Found..."));
    this.mongoRepoService = mongoRepoService;
  }

  public void updatePythonDependencies() {
    List<Thread> threads = new ArrayList<>();
    for (Repository repository : this.repositories) {
      threads.add(executeUpdate(repository));
    }
    threads.forEach(this::join);
  }

  private Thread executeUpdate(final Repository repository) {
    log.debug("Execute Python Dependencies Update on: [ {} ]", repository);
    List<String> arguments = new LinkedList<>();
    arguments.add(repository.getRepoPath().toString());
    arguments.add(String.format(BRANCH_UPDATE_DEPENDENCIES, LocalDate.now()));

    return new ExecutePythonUpdate(
            this.latestVersions, repository, this.scriptFile, arguments, mongoRepoService)
        .start();
  }

  // suppressing sonarlint rule for interrupting thread
  @SuppressWarnings("java:S2142")
  private void join(final Thread thread) {
    try {
      thread.join();
    } catch (InterruptedException ex) {
      ProcessUtils.setExceptionCaught(true);
      log.error("Exception Join Thread", ex);
    }
  }
}
