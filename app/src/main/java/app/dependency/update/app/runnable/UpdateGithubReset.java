package app.dependency.update.app.runnable;

import static app.dependency.update.app.util.CommonUtils.*;
import static app.dependency.update.app.util.ConstantUtils.PARAM_REPO_HOME;

import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import app.dependency.update.app.model.AppInitData;
import app.dependency.update.app.model.ScriptFile;
import java.util.LinkedList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdateGithubReset {
  private final String repoHome;
  private final ScriptFile scriptFile;
  private final boolean isWindows;

  public UpdateGithubReset(final AppInitData appInitData) {
    this.repoHome = appInitData.getArgsMap().get(PARAM_REPO_HOME);
    this.scriptFile =
        appInitData.getScriptFiles().stream()
            .filter(sf -> sf.getType().equals(UpdateType.GITHUB_RESET))
            .findFirst()
            .orElseThrow(
                () -> new AppDependencyUpdateRuntimeException("Github Reset Script Not Found..."));
    this.isWindows = appInitData.isWindows();
  }

  public void updateGithubReset() {
    log.info("Execute Github Reset on: [ {} ]", this.repoHome);
    List<String> arguments = new LinkedList<>();
    arguments.add(this.repoHome);
    Thread executeThread =
        new ExecuteScriptFile(
                threadName(this.getClass().getSimpleName()),
                this.scriptFile,
                arguments,
                this.isWindows)
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
