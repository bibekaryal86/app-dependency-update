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

  public void startScheduler() {
    log.info("Start Scheduler...");

    try {
      Scheduler scheduler = new StdSchedulerFactory(getProperties()).getScheduler();
      scheduler.start();

      // schedule to get gradle plugins from local maven repo and set the Map in CommonUtil
      scheduler.scheduleJob(
          getJobDetailMavenRepoPlugins(),
          getTrigger(
              SchedulerJobMavenRepoPlugins.class.getSimpleName(),
              getJobDetailMavenRepoPlugins(),
              CronScheduleBuilder.dailyAtHourAndMinute(10, 10)));
      // scheduler to get/manipulate/save dependencies for local maven repo
      scheduler.scheduleJob(
          getJobDetailMavenRepoDependencies(),
          getTrigger(
              SchedulerJobMavenRepoDependencies.class.getSimpleName(),
              getJobDetailMavenRepoDependencies(),
              CronScheduleBuilder.dailyAtHourAndMinute(10, 15)));
    } catch (SchedulerException ex) {
      throw new AppDependencyUpdateRuntimeException("Scheduler Initialization Error", ex);
    }
  }

  private Trigger getTrigger(
      String identity, JobDetail jobDetail, CronScheduleBuilder cronScheduleBuilder) {
    return TriggerBuilder.newTrigger()
        .withIdentity(identity)
        .withSchedule(cronScheduleBuilder)
        .forJob(jobDetail)
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

  private Properties getProperties() {
    String falseStr = "false"; // for lint smell
    Properties properties = new Properties();
    // default properties from quartz.properties
    properties.setProperty("org.quartz.scheduler.instanceName", "Quartz"); // DefaultQuartzScheduler
    properties.setProperty("org.quartz.scheduler.rmi.export", falseStr);
    properties.setProperty("org.quartz.scheduler.rmi.proxy", falseStr);
    properties.setProperty("org.quartz.scheduler.rmi.wrapJobExecutionInUserTransaction", falseStr);
    properties.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
    properties.setProperty("org.quartz.threadPool.threadCount", "5"); // 10
    properties.setProperty("org.quartz.threadPool.threadPriority", "5");
    properties.setProperty(
        "org.quartz.threadPool.threadsInheritContextClassLoaderOfInitializingThread", "true");
    properties.setProperty("org.quartz.jobStore.misfireThreshold", "60000");
    properties.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
    // others
    properties.setProperty("org.quartz.scheduler.instanceId", "Scheduler");
    properties.setProperty("org.quartz.scheduler.threadName", "QS");
    return properties;
  }
}
