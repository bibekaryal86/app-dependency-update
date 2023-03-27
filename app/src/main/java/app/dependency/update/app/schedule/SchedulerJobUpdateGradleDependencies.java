package app.dependency.update.app.schedule;

import app.dependency.update.app.model.AppInitData;
import app.dependency.update.app.util.CommonUtil;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@Slf4j
public class SchedulerJobUpdateGradleDependencies implements Job {

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    log.info("Start SchedulerJobUpdateGradleDependencies...");
    final AppInitData appInitData =
        (AppInitData) context.getJobDetail().getJobDataMap().get(CommonUtil.APP_INIT_DATA_MAP);
    CompletableFuture.runAsync(() -> updateGradleDependencies(appInitData));
  }

  private void updateGradleDependencies(final AppInitData appInitData) {

    // TODO
    log.info("Finish SchedulerJobUpdateGradleDependencies...");
  }
}
