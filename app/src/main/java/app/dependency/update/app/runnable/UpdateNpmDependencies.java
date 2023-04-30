package app.dependency.update.app.runnable;

import static app.dependency.update.app.util.CommonUtils.*;
import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import app.dependency.update.app.model.AppInitData;
import app.dependency.update.app.model.Repository;
import app.dependency.update.app.model.ScriptFile;
import java.time.LocalDate;
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
    // updating NPM dependencies is fairly straightforward because everything is done by the npm
    // script, we just need to execute it for each repository
    this.repositories.forEach(this::executeUpdate);
  }

  private void executeUpdate(final Repository repository) {
    log.info("Execute NPM Dependencies Update on: [ {} ]", repository);
    List<String> arguments = new LinkedList<>();
    arguments.add(repository.getRepoPath().toString());
    arguments.add(String.format(BRANCH_UPDATE_DEPENDENCIES, LocalDate.now()));
    new ExecuteScriptFile(
            threadName(repository, this.getClass().getSimpleName()), this.scriptFile, arguments)
        .start();
  }
}
