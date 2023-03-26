package app.dependency.update.app.schedule;

import app.dependency.update.app.execute.CreateTempScriptFiles;
import app.dependency.update.app.model.AppInitData;
import app.dependency.update.app.util.CommonUtil;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@Slf4j
public class SchedulerJobCreateTempScriptFiles implements Job {

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    log.info("Start SchedulerJobDeleteTempScriptFiles...");

    final AppInitData appInitData =
        (AppInitData) context.getJobDetail().getJobDataMap().get(CommonUtil.APP_INIT_DATA_MAP);
    new CreateTempScriptFiles(appInitData.getScriptFiles()).createTempScriptFiles();
    log.info("Finish SchedulerJobDeleteTempScriptFiles...");
  }
}
