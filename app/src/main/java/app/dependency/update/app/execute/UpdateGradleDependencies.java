package app.dependency.update.app.execute;

import app.dependency.update.app.model.Repository;
import app.dependency.update.app.model.ScriptFile;
import app.dependency.update.app.util.CommonUtil;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdateGradleDependencies {

  private final List<Repository> repositories;
  private final ScriptFile scriptFile;

  public UpdateGradleDependencies(
      final List<Repository> repositories, final ScriptFile scriptFile) {
    this.repositories = repositories;
    this.scriptFile = scriptFile;
  }

  public void updateDependenciesGradle() {
    this.repositories.forEach(this::executeUpdate);
  }

  private void executeUpdate(final Repository repository) {
    log.info("Execute build.gradle Update on: [ {} ]", repository);
    List<String> arguments = new LinkedList<>();
    arguments.add(repository.getRepoPath().toString());
    arguments.add(String.format(CommonUtil.BRANCH_UPDATE_DEPENDENCIES, LocalDate.now()));
    new UpdateGradleBuildFile(repository, this.scriptFile, arguments).start();
  }
}
