package app.dependency.update.app.schedule;

import app.dependency.update.app.update.UpdateGithubPullRequests;
import app.dependency.update.app.model.AppInitData;
import app.dependency.update.app.model.ScriptFile;
import app.dependency.update.app.util.CommonUtil;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@Slf4j
public class SchedulerJobUpdateGithubPullRequests implements Job {

  @Override
  public void execute(final JobExecutionContext context) throws JobExecutionException {
    log.info("Start SchedulerJobUpdateGithubPullRequests...");
    final AppInitData appInitData =
        (AppInitData) context.getJobDetail().getJobDataMap().get(CommonUtil.APP_INIT_DATA_MAP);
    final boolean isWrapperMerge =
        (boolean) context.getJobDetail().getJobDataMap().get(CommonUtil.WRAPPER);
    CompletableFuture.runAsync(() -> updateGithubPullRequests(appInitData, isWrapperMerge));
  }

  private void updateGithubPullRequests(
      final AppInitData appInitData, final boolean isWrapperMerge) {
    Optional<ScriptFile> githubScriptFile =
        appInitData.getScriptFiles().stream()
            .filter(scriptFile -> CommonUtil.GITHUB.equals(scriptFile.getType()))
            .findFirst();

    if (appInitData.getRepositories().isEmpty() || githubScriptFile.isEmpty()) {
      log.info(
          "All Repositories [ {} ] and/or Github Script [ {} ] is empty!",
          appInitData.getRepositories().isEmpty(),
          githubScriptFile.isEmpty());
    } else {
      log.info("Updating All Repositories for Github Merge...");
      new UpdateGithubPullRequests(
              appInitData.getRepositories(), githubScriptFile.get(), isWrapperMerge)
          .updateGithubPullRequests();
    }
    log.info("Finish SchedulerJobUpdateGithubPullRequests...");
  }
}
