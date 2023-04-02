package app.dependency.update.app.execute;

import app.dependency.update.app.model.Repository;
import app.dependency.update.app.model.ScriptFile;
import app.dependency.update.app.util.CommonUtil;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdateGithubPullRequests {

  private final List<Repository> repositories;
  private final ScriptFile scriptFile;
  private final boolean isWrapperMerge;

  public UpdateGithubPullRequests(
      final List<Repository> repositories,
      final ScriptFile scriptFile,
      final boolean isWrapperMerge) {
    this.repositories = repositories;
    this.scriptFile = scriptFile;
    this.isWrapperMerge = isWrapperMerge;
  }

  public void updateGithubPullRequests() {
    // updating github pull requests is fairly straightforward because everything is done by the
    // github script, we just need to execute it for each repository
    // the script checks if builds passed and, if yes, merges the PR
    this.repositories.forEach(this::executeUpdate);
  }

  private void executeUpdate(final Repository repository) {
    log.info("Execute Github Pull Request Update on: [ {} ]", repository);
    List<String> arguments = new LinkedList<>();
    arguments.add(repository.getRepoPath().toString());

    if (!this.isWrapperMerge) {
      arguments.add(String.format(CommonUtil.BRANCH_UPDATE_DEPENDENCIES, LocalDate.now()));
      new ExecuteScriptFile(repository.getRepoName(), this.scriptFile, arguments).start();
    } else if (repository.getType().equals(CommonUtil.GRADLE)) {
      arguments.add(String.format(CommonUtil.BRANCH_UPDATE_WRAPPER, LocalDate.now()));
      new ExecuteScriptFile(repository.getRepoName(), this.scriptFile, arguments).start();
    }
  }
}
