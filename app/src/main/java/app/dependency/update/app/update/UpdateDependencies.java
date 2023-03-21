package app.dependency.update.app.update;

import app.dependency.update.app.model.AppInitData;
import app.dependency.update.app.model.Repository;
import app.dependency.update.app.model.ScriptFile;
import app.dependency.update.app.util.CommonUtil;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdateDependencies {

  private final AppInitData appInitData;

  public UpdateDependencies(AppInitData appInitData) {
    this.appInitData = appInitData;
  }

  public void updateDependencies() {
    CompletableFuture.runAsync(this::updateNpmDependencies);
    CompletableFuture.runAsync(this::updateGradleDependencies);
  }

  private void updateNpmDependencies() {
    // check and update NPM repositories
    List<Repository> npmRepositories =
        this.appInitData.getRepositories().stream()
            .filter(repository -> CommonUtil.NPM.equals(repository.getType()))
            .toList();
    List<ScriptFile> npmScripts =
        this.appInitData.getScriptFiles().stream()
            .filter(scriptFile -> CommonUtil.NPM.equals(scriptFile.getType()))
            .sorted(Comparator.comparingInt(ScriptFile::getStep))
            .toList();

    if (npmRepositories.isEmpty() || npmScripts.isEmpty()) {
      log.info(
          "NPM Repositories [{}] and/or NPM Scripts [{}] are empty!",
          npmRepositories.isEmpty(),
          npmScripts.isEmpty());
    } else {
      log.info("Updating NPM repositories: {}", npmRepositories);
      new UpdateDependenciesNpm(npmRepositories, npmScripts, this.appInitData.getArgsMap())
          .updateDependenciesNpm();
    }
  }

  private void updateGradleDependencies() {
    // check and update Gradle repositories
    // first update gradle wrapper, then update dependencies
    List<Repository> gradleRepositories =
        this.appInitData.getRepositories().stream()
            .filter(repository -> CommonUtil.GRADLE.equals(repository.getType()))
            .toList();
    List<ScriptFile> gradleScripts =
        this.appInitData.getScriptFiles().stream()
            .filter(scriptFile -> CommonUtil.GRADLE.equals(scriptFile.getType()))
            .sorted(Comparator.comparingInt(ScriptFile::getStep))
            .toList();

    if (gradleRepositories.isEmpty() || gradleScripts.isEmpty()) {
      log.info(
          "Gradle Repositories [{}] and/or Gradle Scripts [{}] are empty!",
          gradleRepositories.isEmpty(),
          gradleScripts.isEmpty());
    } else {
      log.info("Updating Gradle repositories: {}", gradleRepositories);
      new UpdateDependenciesGradle(gradleRepositories, gradleScripts, this.appInitData.getArgsMap())
          .updateDependenciesGradle();
    }
  }
}
