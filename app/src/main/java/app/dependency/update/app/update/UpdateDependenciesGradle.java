package app.dependency.update.app.update;

import app.dependency.update.app.execute.GradleWrapperStatus;
import app.dependency.update.app.model.Repository;
import app.dependency.update.app.model.ScriptFile;
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
  }
}
