package app.dependency.update.app.schedule;

import app.dependency.update.app.execute.UpdateGradleDependencies;
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
public class SchedulerJobUpdateGradleDependencies implements Job {

  @Override
  public void execute(final JobExecutionContext context) throws JobExecutionException {
    log.info("Start SchedulerJobUpdateGradleDependencies...");
    final AppInitData appInitData =
        (AppInitData) context.getJobDetail().getJobDataMap().get(CommonUtil.APP_INIT_DATA_MAP);
    CompletableFuture.runAsync(() -> updateGradleDependencies(appInitData));
  }

  private void updateGradleDependencies(final AppInitData appInitData) {
    List<Repository> gradleRepositories =
        appInitData.getRepositories().stream()
            .filter(repository -> CommonUtil.GRADLE.equals(repository.getType()))
            .toList();
    Optional<ScriptFile> gradleScriptFile =
        appInitData.getScriptFiles().stream()
            .filter(
                scriptFile ->
                    CommonUtil.GRADLE.equals(scriptFile.getType()) && scriptFile.getStep() == 2)
            .findFirst();

    if (gradleRepositories.isEmpty() || gradleScriptFile.isEmpty()) {
      log.info(
          "Gradle Repositories [ {} ] and/or Gradle Script [ {} ] is empty!",
          gradleRepositories.isEmpty(),
          gradleScriptFile.isEmpty());
    } else {
      log.info("Updating Gradle repositories: {}", gradleRepositories);
      new UpdateGradleDependencies(gradleRepositories, gradleScriptFile.get())
          .updateDependenciesGradle();
    }
    log.info("Finish SchedulerJobUpdateGradleDependencies...");
  }
}
