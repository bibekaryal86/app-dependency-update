package app.dependency.update.app.schedule;

import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

@Slf4j
public class AppScheduler {

  public void startRepoScheduler() {
    log.info("Start Scheduler...");

    try {
      String schedulerName = "Update-Repo";
      Scheduler scheduler = new StdSchedulerFactory(getProperties(schedulerName)).getScheduler(schedulerName);
      scheduler.start();

      // schedule to get gradle plugins from local maven repo and set the Map in CommonUtil
      JobDetail jobDetailMavenRepoPlugins = getJobDetailMavenRepoPlugins();
      Trigger triggerMavenRepoPlugins =
          getTrigger(
              SchedulerJobMavenRepoPlugins.class.getSimpleName(),
              CronScheduleBuilder.dailyAtHourAndMinute(10, 10));
      scheduler.scheduleJob(jobDetailMavenRepoPlugins, triggerMavenRepoPlugins);

      // scheduler to get/manipulate/save dependencies for local maven repo and set the Map in CommonUtil
      JobDetail jobDetailMavenRepoDependencies = getJobDetailMavenRepoDependencies();
      Trigger triggerMavenRepoDependencies =
          getTrigger(
              SchedulerJobMavenRepoDependencies.class.getSimpleName(),
              CronScheduleBuilder.dailyAtHourAndMinute(10, 10));
      scheduler.scheduleJob(jobDetailMavenRepoDependencies, triggerMavenRepoDependencies);
    } catch (SchedulerException ex) {
      throw new AppDependencyUpdateRuntimeException("Scheduler Initialization Error", ex);
    }
  }

  private Trigger getTrigger(String identity, CronScheduleBuilder cronScheduleBuilder) {
    return TriggerBuilder.newTrigger()
        .withIdentity(identity)
        .withSchedule(cronScheduleBuilder)
        .startNow()
        .build();
  }

  private JobDetail getJobDetailMavenRepoPlugins() {
    return JobBuilder.newJob(SchedulerJobMavenRepoPlugins.class)
        .withIdentity(SchedulerJobMavenRepoPlugins.class.getSimpleName())
        .build();
  }

  private JobDetail getJobDetailMavenRepoDependencies() {
    return JobBuilder.newJob(SchedulerJobMavenRepoDependencies.class)
        .withIdentity(SchedulerJobMavenRepoDependencies.class.getSimpleName())
        .build();
  }

  private Properties getProperties(String schedulerName) {
    String fStr = "false";
    Properties properties = new Properties();
    // default properties from quartz.properties
    properties.setProperty("org.quartz.scheduler.instanceName", "Quartz-"+schedulerName);
    properties.setProperty("org.quartz.scheduler.rmi.export", fStr);
    properties.setProperty("org.quartz.scheduler.rmi.proxy", fStr);
    properties.setProperty("org.quartz.scheduler.rmi.wrapJobExecutionInUserTransaction", fStr);
    properties.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
    properties.setProperty("org.quartz.threadPool.threadCount", "5");
    properties.setProperty("org.quartz.threadPool.threadPriority", "5");
    properties.setProperty(
        "org.quartz.threadPool.threadsInheritContextClassLoaderOfInitializingThread", "true");
    properties.setProperty("org.quartz.jobStore.misfireThreshold", "60000");
    properties.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
    // others
    properties.setProperty("org.quartz.scheduler.instanceId", "Scheduler-"+schedulerName);
    properties.setProperty("org.quartz.scheduler.threadName", "QS-"+schedulerName);
    return properties;
  }
}
