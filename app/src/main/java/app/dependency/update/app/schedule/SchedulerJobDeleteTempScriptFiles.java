package app.dependency.update.app.schedule;

import app.dependency.update.app.execute.DeleteTempScriptFiles;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@Slf4j
public class SchedulerJobDeleteTempScriptFiles implements Job {

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    log.info("Start SchedulerJobDeleteTempScriptFiles...");
    CompletableFuture.runAsync(this::deleteTempScriptFiles);
  }

  private void deleteTempScriptFiles() {
    new DeleteTempScriptFiles();
    log.info("Finish SchedulerJobDeleteTempScriptFiles...");
  }
}
