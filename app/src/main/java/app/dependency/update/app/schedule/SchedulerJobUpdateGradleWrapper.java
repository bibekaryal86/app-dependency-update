package app.dependency.update.app.schedule;

import app.dependency.update.app.execute.UpdateGradleWrapper;
import app.dependency.update.app.model.AppInitData;
import app.dependency.update.app.model.Repository;
import app.dependency.update.app.model.ScriptFile;
import app.dependency.update.app.util.CommonUtil;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@Slf4j
public class SchedulerJobUpdateGradleWrapper implements Job {

  @Override
  public void execute(final JobExecutionContext context) throws JobExecutionException {
    log.info("Start SchedulerJobUpdateGradleWrapper...");
    final AppInitData appInitData =
        (AppInitData) context.getJobDetail().getJobDataMap().get(CommonUtil.APP_INIT_DATA_MAP);
    CompletableFuture.runAsync(() -> updateGradleWrapper(appInitData));
  }

  private void updateGradleWrapper(final AppInitData appInitData) {
    List<Repository> gradleRepositories =
        appInitData.getRepositories().stream()
            .filter(repository -> CommonUtil.GRADLE.equals(repository.getType()))
            .toList();
    Optional<ScriptFile> gradleWrapperScriptFile =
        appInitData.getScriptFiles().stream()
            .filter(
                scriptFile ->
                    CommonUtil.GRADLE.equals(scriptFile.getType()) && scriptFile.getStep() == 1)
            .findFirst();

    if (gradleRepositories.isEmpty() || gradleWrapperScriptFile.isEmpty()) {
      log.info(
          "Gradle Repositories [ {} ] and/or Gradle Wrapper Script [ {} ] is empty!",
          gradleRepositories.isEmpty(),
          gradleWrapperScriptFile.isEmpty());
    } else {
      log.info("Updating Gradle repositories for Wrapper: {}", gradleRepositories);
      new UpdateGradleWrapper(gradleRepositories, gradleWrapperScriptFile.get())
          .updateGradleWrapper();
    }
    log.info("Finish SchedulerJobUpdateGradleWrapper...");
  }
}
