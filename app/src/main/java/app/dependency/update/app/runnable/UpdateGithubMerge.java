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
public class UpdateGithubMerge {
  private final List<Repository> repositories;
  private final ScriptFile scriptFile;
  private final boolean isWrapperMerge;

  public UpdateGithubMerge(final AppInitData appInitData, final boolean isWrapperMerge) {
    this.repositories = appInitData.getRepositories();
    this.scriptFile =
        appInitData.getScriptFiles().stream()
            .filter(sf -> sf.getType().equals(UpdateType.GITHUB_MERGE))
            .findFirst()
            .orElseThrow(
                () -> new AppDependencyUpdateRuntimeException("Github Merge Script Not Found..."));
    this.isWrapperMerge = isWrapperMerge;
  }

  public void updateGithubMerge() {
    // updating github pull requests is fairly straightforward because everything is done by the
    // github script, we just need to execute it for each repository
    // the script checks if builds passed and, if yes, merges the PR
    this.repositories.forEach(this::executeUpdate);
  }

  private void executeUpdate(final Repository repository) {
    log.info("Execute Github Merge Update on: [ {} ]", repository);
    List<String> arguments = new LinkedList<>();
    arguments.add(repository.getRepoPath().toString());

    if (!this.isWrapperMerge) {
      arguments.add(String.format(BRANCH_UPDATE_DEPENDENCIES, LocalDate.now()));
      new ExecuteScriptFile(
              threadName(repository, this.getClass().getSimpleName()), this.scriptFile, arguments)
          .start();
    } else if (repository.getType().equals(UpdateType.GRADLE_DEPENDENCIES)) {
      arguments.add(String.format(BRANCH_UPDATE_WRAPPER, LocalDate.now()));
      new ExecuteScriptFile(
              threadName(repository, this.getClass().getSimpleName()), this.scriptFile, arguments)
          .start();
    }
  }
}
