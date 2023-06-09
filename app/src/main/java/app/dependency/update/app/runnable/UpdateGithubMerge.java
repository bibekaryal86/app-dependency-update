package app.dependency.update.app.runnable;

import static app.dependency.update.app.util.CommonUtils.*;
import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import app.dependency.update.app.model.AppInitData;
import app.dependency.update.app.model.ScriptFile;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdateGithubMerge {
  private final String repoHome;
  private final ScriptFile scriptFile;
  private final boolean isWindows;

  public UpdateGithubMerge(final AppInitData appInitData) {
    this.repoHome = appInitData.getArgsMap().get(PARAM_REPO_HOME);
    this.scriptFile =
        appInitData.getScriptFiles().stream()
            .filter(sf -> sf.getType().equals(UpdateType.GITHUB_MERGE))
            .findFirst()
            .orElseThrow(
                () -> new AppDependencyUpdateRuntimeException("Github Merge Script Not Found..."));
    this.isWindows = appInitData.isWindows();
  }

  public void updateGithubMerge() {
    // updating GitHub pull requests is fairly straightforward because everything is done by the
    // GitHub script, we just need to execute it for repository home
    log.info("Execute Github Merge on: [ {} ]", this.repoHome);
    List<String> arguments = new LinkedList<>();
    arguments.add(this.repoHome);
    arguments.add(String.format(BRANCH_UPDATE_DEPENDENCIES, LocalDate.now()));
    new ExecuteScriptFile(
            threadName(this.getClass().getSimpleName()), this.scriptFile, arguments, this.isWindows)
        .start();
  }
}
