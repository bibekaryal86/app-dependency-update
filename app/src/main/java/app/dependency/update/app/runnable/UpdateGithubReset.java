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
public class UpdateGithubReset {
  private final List<Repository> repositories;
  private final ScriptFile scriptFile;

  public UpdateGithubReset(final AppInitData appInitData) {
    this.repositories = appInitData.getRepositories();
    this.scriptFile =
        appInitData.getScriptFiles().stream()
            .filter(sf -> sf.getType().equals(UpdateType.GITHUB_RESET))
            .findFirst()
            .orElseThrow(
                () -> new AppDependencyUpdateRuntimeException("Github Reset Script Not Found..."));
  }

  public void updateGithubReset() {
    // updating github reset is fairly straightforward because everything is done by the
    // github script, we just need to execute it for each repository
    this.repositories.forEach(this::executeUpdate);
  }

  private void executeUpdate(final Repository repository) {
    log.info("Execute Github Reset Update on: [ {} ]", repository);
    List<String> arguments = new LinkedList<>();
    arguments.add(repository.getRepoPath().toString());
    new ExecuteScriptFile(
            threadName(repository, this.getClass().getSimpleName()), this.scriptFile, arguments)
        .start();
  }
}
