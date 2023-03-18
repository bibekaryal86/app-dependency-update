package app.dependency.update.app.update;

import app.dependency.update.app.execute.ExecuteScriptFile;
import app.dependency.update.app.model.Repository;
import app.dependency.update.app.model.ScriptFile;
import app.dependency.update.app.util.Util;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdateDependenciesNpm {

  private final List<Repository> repositories;
  private final List<ScriptFile> scriptFiles;
  private final Map<String, String> argsMap;

  public UpdateDependenciesNpm(
      List<Repository> repositories, List<ScriptFile> scriptFiles, Map<String, String> argsMap) {
    this.repositories = repositories;
    this.scriptFiles = scriptFiles;
    this.argsMap = argsMap;
  }

  public void updateDependenciesNpm() {
    // updating NPM dependencies is fairly straightforward because everything is done by the npm
    // script, we just need to execute it for each repository
    this.repositories.forEach(repository -> executeUpdate(repository, this.scriptFiles.get(0)));
  }

  private void executeUpdate(Repository repository, ScriptFile scriptFile) {
    log.info("Execute NPM Update on: {}", repository);
    String arguments = this.argsMap.get(Util.PARAM_REPO_HOME) + " " + repository.getRepoName();
    ExecuteScriptFile executeScriptFile =
        new ExecuteScriptFile(repository.getRepoName(), scriptFile, arguments);
    Thread thread = executeScriptFile.start();
    while (thread.isAlive()) {
      Util.threadSleep();
    }
  }
}
