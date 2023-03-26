package app.dependency.update.app.schedule;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@Slf4j
public class SchedulerJobUpdateGradleDependencies implements Job {

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    log.info("Start SchedulerJobUpdateGradleDependencies...");
    // TODO
    log.info("Finish SchedulerJobUpdateGradleDependencies...");
  }
}
