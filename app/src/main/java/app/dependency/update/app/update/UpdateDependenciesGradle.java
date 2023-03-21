package app.dependency.update.app.update;

import app.dependency.update.app.execute.ExecuteScriptFile;
import app.dependency.update.app.execute.GradleWrapperStatus;
import app.dependency.update.app.model.Repository;
import app.dependency.update.app.model.ScriptFile;
import app.dependency.update.app.util.CommonUtil;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdateDependenciesGradle {

  private final List<Repository> repositories;
  private final List<ScriptFile> scriptFiles;
  private final Map<String, String> argsMap;

  public UpdateDependenciesGradle(
      List<Repository> repositories, List<ScriptFile> scriptFiles, Map<String, String> argsMap) {
    this.repositories = repositories;
    this.scriptFiles = scriptFiles;
    this.argsMap = argsMap;
  }

  public void updateDependenciesGradle() {
    List<Repository> gradleRepositories =
        new GradleWrapperStatus(repositories).getGradleWrapperStatus();
    log.info("Gradle Repositories with Gradle Wrapper Status: {}", gradleRepositories);

    gradleRepositories.forEach(repository -> executeUpdate(repository, this.scriptFiles.get(0)));
  }

  private void executeUpdate(Repository repository, ScriptFile scriptFile) {
    log.info("Execute Gradle Update on: {}", repository);
    List<String> arguments = new LinkedList<>();
    arguments.add(this.argsMap.get(CommonUtil.PARAM_REPO_HOME));
    arguments.add(repository.getRepoName());
    arguments.add(repository.getGradleVersion());
    new ExecuteScriptFile(repository.getRepoName(), scriptFile, arguments).start();
  }
}
