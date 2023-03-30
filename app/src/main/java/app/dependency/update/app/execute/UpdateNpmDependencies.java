package app.dependency.update.app.execute;

import app.dependency.update.app.model.Repository;
import app.dependency.update.app.model.ScriptFile;
import app.dependency.update.app.util.CommonUtil;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdateNpmDependencies {

  private final List<Repository> repositories;
  private final ScriptFile scriptFile;
  private final Map<String, String> argsMap;

  public UpdateNpmDependencies(
      final List<Repository> repositories,
      final ScriptFile scriptFile,
      final Map<String, String> argsMap) {
    this.repositories = repositories;
    this.scriptFile = scriptFile;
    this.argsMap = argsMap;
  }

  public void updateNpmDependencies() {
    // updating NPM dependencies is fairly straightforward because everything is done by the npm
    // script, we just need to execute it for each repository
    this.repositories.forEach(this::executeUpdate);
  }

  private void executeUpdate(final Repository repository) {
    log.info("Execute NPM Update on: {}", repository);
    List<String> arguments = new LinkedList<>();
    arguments.add(this.argsMap.get(CommonUtil.PARAM_REPO_HOME));
    arguments.add(repository.getRepoName());
    new ExecuteScriptFile(repository.getRepoName(), this.scriptFile, arguments).start();
  }
}
