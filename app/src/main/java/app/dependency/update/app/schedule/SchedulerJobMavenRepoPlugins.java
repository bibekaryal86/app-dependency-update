package app.dependency.update.app.schedule;

import app.dependency.update.app.execute.MavenRepo;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@Slf4j
public class SchedulerJobMavenRepoPlugins implements Job {

  @Override
  public void execute(final JobExecutionContext context) throws JobExecutionException {
    log.info("Start SchedulerJobMavenRepoPlugins...");
    CompletableFuture.runAsync(this::execute);
  }

  private void execute() {
    new MavenRepo().setPluginsMap();
    log.info("Finish SchedulerJobMavenRepoPlugins...");
  }
}
