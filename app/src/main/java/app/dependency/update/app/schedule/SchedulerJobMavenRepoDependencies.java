package app.dependency.update.app.schedule;

import app.dependency.update.app.execute.MavenRepo;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@Slf4j
public class SchedulerJobMavenRepoDependencies implements Job {

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    log.info("Start SchedulerJobMavenRepoDependencies...");
    new MavenRepo().setDependenciesMap();
    log.info("Finish SchedulerJobMavenRepoDependencies...");
  }
}
