package app.dependency.update.app.runnable;

import static app.dependency.update.app.util.CommonUtils.*;

import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import app.dependency.update.app.model.AppInitData;
import app.dependency.update.app.model.Repository;
import app.dependency.update.app.model.ScriptFile;
import java.util.LinkedList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdateNpmSnapshots {
  private final List<Repository> repositories;
  private final ScriptFile scriptFile;
  private final String branchName;

  public UpdateNpmSnapshots(final AppInitData appInitData, final String branchName) {
    this.repositories =
        appInitData.getRepositories().stream()
            .filter(repository -> repository.getType().equals(UpdateType.NODE_DEPENDENCIES))
            .toList();
    this.scriptFile =
        appInitData.getScriptFiles().stream()
            .filter(sf -> sf.getType().equals(UpdateType.NPM_SNAPSHOT))
            .findFirst()
            .orElseThrow(
                () -> new AppDependencyUpdateRuntimeException("NPM Snapshot Script Not Found..."));
    this.branchName = branchName;
  }

  public void updateNpmSnapshots() {
    // updating NPM snapshots is fairly straightforward because everything is done by the npm
    // script, we just need to execute it for each repository
    this.repositories.forEach(this::executeUpdate);
  }

  private void executeUpdate(final Repository repository) {
    log.debug("Execute NPM Snapshots Update on: [ {} ]", repository);
    List<String> arguments = new LinkedList<>();
    arguments.add(repository.getRepoPath().toString());
    arguments.add(branchName);
    new ExecuteScriptFile(
            threadName(repository, this.getClass().getSimpleName()), this.scriptFile, arguments)
        .start();
  }
}
