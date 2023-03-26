package app.dependency.update.app.schedule;

import app.dependency.update.app.execute.CreateTempScriptFiles;
import app.dependency.update.app.model.AppInitData;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@Slf4j
public class SchedulerJobCreateTempScriptFiles implements Job {

  public static final String APP_INIT_DATA_MAP = "APP_INIT_DATA";

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    log.info("Start SchedulerJobDeleteTempScriptFiles...");

    AppInitData appInitData =
        (AppInitData) context.getJobDetail().getJobDataMap().get(APP_INIT_DATA_MAP);
    new CreateTempScriptFiles(appInitData.getScriptFiles()).createTempScriptFiles();
    log.info("Finish SchedulerJobDeleteTempScriptFiles...");
  }
}
