package app.dependency.update.app.schedule;

import app.dependency.update.app.execute.SetAppInitData;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@Slf4j
public class SchedulerJobAppInitData implements Job {

  @Override
  public void execute(final JobExecutionContext context) throws JobExecutionException {
    log.info("Start SchedulerJobAppInitData...");
    CompletableFuture.runAsync(this::execute);
  }

  private void execute() {
    new SetAppInitData(new String[0]).setAppInitData();
    log.info("Finish SchedulerJobAppInitData...");
  }
}
