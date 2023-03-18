package app.dependency.update.app.update;

import app.dependency.update.app.model.AppInitData;
import app.dependency.update.app.model.Repository;
import app.dependency.update.app.model.ScriptFile;
import app.dependency.update.app.util.Util;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdateDependencies {

  private final AppInitData appInitData;

  public UpdateDependencies(AppInitData appInitData) {
    this.appInitData = appInitData;
  }

  public void updateDependencies() {
    // check and update NPM repositories
    List<Repository> npmRepositories =
        this.appInitData.getRepositories().stream()
            .filter(repository -> Util.NPM.equals(repository.getType()))
            .toList();
    List<ScriptFile> npmScripts =
        this.appInitData.getScriptFiles().stream()
            .filter(scriptFile -> Util.NPM.equals(scriptFile.getType()))
            .sorted(Comparator.comparingInt(ScriptFile::getStep))
            .toList();

    if (npmRepositories.isEmpty() || npmScripts.isEmpty()) {
      log.info(
          "NPM Repositories [{}] and/or NPM Scripts [{}] are empty!",
          npmRepositories.isEmpty(),
          npmScripts.isEmpty());
    } else {
      log.info("Updating NPM repositories: {}", npmRepositories);
      UpdateDependenciesNpm updateDependenciesNpm =
          new UpdateDependenciesNpm(npmRepositories, npmScripts, this.appInitData.getArgsMap());
      updateDependenciesNpm.updateDependenciesNpm();
    }
  }
}
