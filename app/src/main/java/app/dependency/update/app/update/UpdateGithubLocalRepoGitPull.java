package app.dependency.update.app.update;

import app.dependency.update.app.helper.ExecuteScriptFile;
import app.dependency.update.app.model.Repository;
import app.dependency.update.app.model.ScriptFile;
import java.util.LinkedList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdateGithubLocalRepoGitPull {

  private final List<Repository> repositories;
  private final ScriptFile scriptFile;

  public UpdateGithubLocalRepoGitPull(
      final List<Repository> repositories, final ScriptFile scriptFile) {
    this.repositories = repositories;
    this.scriptFile = scriptFile;
  }

  public void updateGithubPullRepo() {
    // updating github pull repo is fairly straightforward because everything is done by the
    // github script, we just need to execute it for each repository
    this.repositories.forEach(this::executeUpdate);
  }

  private void executeUpdate(final Repository repository) {
    log.info("Execute Github Pull Repo Update on: [ {} ]", repository);
    List<String> arguments = new LinkedList<>();
    arguments.add(repository.getRepoPath().toString());
    new ExecuteScriptFile(repository.getRepoName(), this.scriptFile, arguments).start();
  }
}
