package app.dependency.update.app.runnable;

import static app.dependency.update.app.util.CommonUtils.*;
import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import app.dependency.update.app.model.AppInitData;
import app.dependency.update.app.model.Repository;
import app.dependency.update.app.model.ScriptFile;
import app.dependency.update.app.model.dto.Dependencies;
import app.dependency.update.app.model.dto.Plugins;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdateGradleDependencies {
  private final List<Repository> repositories;
  private final ScriptFile scriptFile;
  private final Map<String, Plugins> pluginsMap;
  private final Map<String, Dependencies> dependenciesMap;

  public UpdateGradleDependencies(
      final AppInitData appInitData,
      final Map<String, Plugins> pluginsMap,
      final Map<String, Dependencies> dependenciesMap) {
    this.repositories =
        appInitData.getRepositories().stream()
            .filter(repository -> repository.getType().equals(UpdateType.GRADLE_DEPENDENCIES))
            .toList();
    this.scriptFile =
        appInitData.getScriptFiles().stream()
            .filter(sf -> sf.getType().equals(UpdateType.GRADLE_DEPENDENCIES))
            .findFirst()
            .orElseThrow(
                () ->
                    new AppDependencyUpdateRuntimeException(
                        "Gradle Dependencies Script Not Found..."));
    this.pluginsMap = pluginsMap;
    this.dependenciesMap = dependenciesMap;
  }

  public void updateGradleDependencies() {
    List<Thread> threads = new ArrayList<>();
    for (Repository repository : this.repositories) {
      threads.add(executeUpdate(repository));
    }
    threads.forEach(this::join);
  }

  private Thread executeUpdate(final Repository repository) {
    log.debug("Execute Gradle Dependencies Update on: [ {} ]", repository);
    List<String> arguments = new LinkedList<>();
    arguments.add(repository.getRepoPath().toString());
    arguments.add(String.format(BRANCH_UPDATE_DEPENDENCIES, LocalDate.now()));

    return new ExecuteGradleUpdate(
            repository, this.scriptFile, arguments, pluginsMap, dependenciesMap)
        .start();
  }

  // suppressing sonarlint rule for interrupting thread
  @SuppressWarnings("java:S2142")
  private void join(Thread executeThread) {
    try {
      executeThread.join();
    } catch (InterruptedException ex) {
      log.error("Exception Join Thread", ex);
    }
  }
}
