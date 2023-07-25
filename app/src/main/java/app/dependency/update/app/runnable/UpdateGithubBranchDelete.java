package app.dependency.update.app.runnable;

import static app.dependency.update.app.util.CommonUtils.*;
import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import app.dependency.update.app.model.AppInitData;
import app.dependency.update.app.model.ScriptFile;
import java.util.LinkedList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdateGithubBranchDelete {
  private final String repoHome;
  private final ScriptFile scriptFile;
  private final boolean isDeleteUpdateDependenciesOnly;

  public UpdateGithubBranchDelete(
      final AppInitData appInitData, final boolean isDeleteUpdateDependenciesOnly) {
    this.repoHome = appInitData.getArgsMap().get(PARAM_REPO_HOME);
    this.scriptFile =
        appInitData.getScriptFiles().stream()
            .filter(sf -> sf.getType().equals(UpdateType.GITHUB_BRANCH_DELETE))
            .findFirst()
            .orElseThrow(
                () ->
                    new AppDependencyUpdateRuntimeException(
                        "Github Branch Delete Script Not Found..."));
    this.isDeleteUpdateDependenciesOnly = isDeleteUpdateDependenciesOnly;
  }

  public void updateGithubBranchDelete() {
    log.info("Execute Github Branch Delete on: [ {} ]", this.repoHome);
    List<String> arguments = new LinkedList<>();
    arguments.add(this.repoHome);
    arguments.add(String.valueOf(this.isDeleteUpdateDependenciesOnly));
    Thread executeThread =
        new ExecuteScriptFile(
                threadName(this.getClass().getSimpleName()), this.scriptFile, arguments)
            .start();
    join(executeThread);
  }

  // suppressing sonarlint rule for interrupting thread
  @SuppressWarnings("java:S2142")
  private void join(Thread executeThread) {
    try {
      executeThread.join();
    } catch (InterruptedException ex) {
      log.error("Exception Join Thread", ex);
    }
  }
}
