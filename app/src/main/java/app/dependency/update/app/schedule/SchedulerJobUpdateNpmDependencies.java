package app.dependency.update.app.schedule;

import app.dependency.update.app.execute.UpdateNpmDependencies;
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
public class SchedulerJobUpdateNpmDependencies implements Job {

  @Override
  public void execute(final JobExecutionContext context) throws JobExecutionException {
    log.info("Start SchedulerJobUpdateNpmDependencies...");
    final AppInitData appInitData =
        (AppInitData) context.getJobDetail().getJobDataMap().get(CommonUtil.APP_INIT_DATA_MAP);
    CompletableFuture.runAsync(() -> updateNpmDependencies(appInitData));
  }

  private void updateNpmDependencies(final AppInitData appInitData) {
    // check and update NPM repositories
    List<Repository> npmRepositories =
        appInitData.getRepositories().stream()
            .filter(repository -> CommonUtil.NPM.equals(repository.getType()))
            .toList();
    Optional<ScriptFile> npmScriptFile =
        appInitData.getScriptFiles().stream()
            .filter(scriptFile -> CommonUtil.NPM.equals(scriptFile.getType()))
            .findFirst();

    if (npmRepositories.isEmpty() || npmScriptFile.isEmpty()) {
      log.info(
          "NPM Repositories [ {} ] and/or NPM Script [ {} ] is empty!",
          npmRepositories.isEmpty(),
          npmScriptFile.isEmpty());
    } else {
      log.info("Updating NPM repositories: {}", npmRepositories);
      new UpdateNpmDependencies(npmRepositories, npmScriptFile.get(), appInitData.getArgsMap())
          .updateNpmDependencies();
    }
    log.info("Finish SchedulerJobUpdateNpmDependencies...");
  }
}
