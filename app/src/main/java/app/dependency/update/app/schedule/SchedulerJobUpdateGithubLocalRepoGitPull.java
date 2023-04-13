package app.dependency.update.app.schedule;

import app.dependency.update.app.model.AppInitData;
import app.dependency.update.app.model.ScriptFile;
import app.dependency.update.app.update.UpdateGithubLocalRepoGitPull;
import app.dependency.update.app.util.CommonUtil;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@Slf4j
public class SchedulerJobUpdateGithubLocalRepoGitPull implements Job {

  @Override
  public void execute(final JobExecutionContext context) throws JobExecutionException {
    log.info("Start SchedulerJobUpdateGithubLocalRepoGitPull...");
    final AppInitData appInitData =
        (AppInitData) context.getJobDetail().getJobDataMap().get(CommonUtil.APP_INIT_DATA_MAP);
    CompletableFuture.runAsync(() -> updateGithubLocalRepoPullRequests(appInitData));
  }

  private void updateGithubLocalRepoPullRequests(final AppInitData appInitData) {
    Optional<ScriptFile> githubScriptFile =
        appInitData.getScriptFiles().stream()
            .filter(
                scriptFile ->
                    CommonUtil.GITHUB.equals(scriptFile.getType()) & scriptFile.getStep() == 1)
            .findFirst();

    if (appInitData.getRepositories().isEmpty() || githubScriptFile.isEmpty()) {
      log.info(
          "All Repositories [ {} ] and/or Github Local Pull Script [ {} ] is empty!",
          appInitData.getRepositories().isEmpty(),
          githubScriptFile.isEmpty());
    } else {
      log.info("Updating All Repositories for Github Local Pull...");
      new UpdateGithubLocalRepoGitPull(appInitData.getRepositories(), githubScriptFile.get())
          .updateGithubPullRepo();
    }
    log.info("Finish SchedulerJobUpdateGithubLocalRepoGitPull...");
  }
}
