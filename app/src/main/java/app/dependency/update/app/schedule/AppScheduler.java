package app.dependency.update.app.schedule;

import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import app.dependency.update.app.model.AppInitData;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

@Slf4j
public class AppScheduler {

  private static final String BEGIN = "Begin";
  private static final String END = "End";
  private static final Map<String, CronScheduleBuilder> SCHEDULER_CRON_BUILDER_MAP =
      Map.ofEntries(
          new AbstractMap.SimpleEntry<>(
              SchedulerJobDeleteTempScriptFiles.class.getSimpleName() + BEGIN,
              CronScheduleBuilder.weeklyOnDayAndHourAndMinute(5, 22, 0)),
          new AbstractMap.SimpleEntry<>(
              SchedulerJobCreateTempScriptFiles.class.getSimpleName(),
              CronScheduleBuilder.weeklyOnDayAndHourAndMinute(5, 22, 5)),
          new AbstractMap.SimpleEntry<>(
              SchedulerJobMavenRepoPlugins.class.getSimpleName(),
              CronScheduleBuilder.dailyAtHourAndMinute(22, 10)),
          new AbstractMap.SimpleEntry<>(
              SchedulerJobMavenRepoDependencies.class.getSimpleName(),
              CronScheduleBuilder.dailyAtHourAndMinute(22, 15)),
          new AbstractMap.SimpleEntry<>(
              SchedulerJobDeleteTempScriptFiles.class.getSimpleName() + END,
              CronScheduleBuilder.weeklyOnDayAndHourAndMinute(5, 22, 30)));

  public void startUpdateRepoScheduler() {
    log.info("Start Repo Scheduler...");
    String schedulerName = "UpdateRepo";

    try {
      Scheduler scheduler =
          new StdSchedulerFactory(getProperties(schedulerName)).getScheduler();
      scheduler.start();
      log.info("UpdateRepo Scheduler Name: {} | {}", scheduler.getSchedulerName(), scheduler.getSchedulerInstanceId());

      // schedule to get gradle plugins from local maven repo and set the Map in CommonUtil
      JobDetail jobDetailMavenRepoPlugins = getJobDetailMavenRepoPlugins();
      Trigger triggerMavenRepoPlugins =
          getTrigger(
              SchedulerJobMavenRepoPlugins.class.getSimpleName(),
              SCHEDULER_CRON_BUILDER_MAP.get(SchedulerJobMavenRepoPlugins.class.getSimpleName()));
      scheduler.scheduleJob(jobDetailMavenRepoPlugins, triggerMavenRepoPlugins);

      // scheduler to get/manipulate/save dependencies for local maven repo and set the Map in
      // CommonUtil
      JobDetail jobDetailMavenRepoDependencies = getJobDetailMavenRepoDependencies();
      Trigger triggerMavenRepoDependencies =
          getTrigger(
              SchedulerJobMavenRepoDependencies.class.getSimpleName(),
              SCHEDULER_CRON_BUILDER_MAP.get(
                  SchedulerJobMavenRepoDependencies.class.getSimpleName()));
      scheduler.scheduleJob(jobDetailMavenRepoDependencies, triggerMavenRepoDependencies);
    } catch (SchedulerException ex) {
      throw new AppDependencyUpdateRuntimeException(
          schedulerName + " Scheduler Initialization Error", ex);
    }
  }

  public void startFileSystemScheduler(final AppInitData appInitData) {
    log.info("Start File System Scheduler...");
    String schedulerName = "FileSystem";

    try {
      Scheduler scheduler =
          new StdSchedulerFactory(getProperties(schedulerName)).getScheduler();
      scheduler.start();
      log.info("FileSystem Scheduler Name: {} | {}", scheduler.getSchedulerName(), scheduler.getSchedulerInstanceId());

      // schedule to delete temp script files before running scripts
      JobDetail jobDetailDeleteTempScriptFiles = getJobDetailDeleteTempScriptFiles(BEGIN);
      Trigger triggerDeleteTempScriptFiles =
          getTrigger(
              SchedulerJobDeleteTempScriptFiles.class.getSimpleName() + BEGIN,
              SCHEDULER_CRON_BUILDER_MAP.get(
                  SchedulerJobDeleteTempScriptFiles.class.getSimpleName() + BEGIN));
      scheduler.scheduleJob(jobDetailDeleteTempScriptFiles, triggerDeleteTempScriptFiles);

      // schedule to delete temp script files after running scripts
      jobDetailDeleteTempScriptFiles = getJobDetailDeleteTempScriptFiles(END);
      triggerDeleteTempScriptFiles =
          getTrigger(
              SchedulerJobDeleteTempScriptFiles.class.getSimpleName() + END,
              SCHEDULER_CRON_BUILDER_MAP.get(
                  SchedulerJobDeleteTempScriptFiles.class.getSimpleName() + END));
      scheduler.scheduleJob(jobDetailDeleteTempScriptFiles, triggerDeleteTempScriptFiles);

      // scheduler to create temp script files
      JobDetail jobDetailCreateTempScriptFiles = getJobDetailCreateTempScriptFiles(appInitData);
      Trigger triggerCreateTempScriptFiles =
          getTrigger(
              SchedulerJobCreateTempScriptFiles.class.getSimpleName(),
              SCHEDULER_CRON_BUILDER_MAP.get(
                  SchedulerJobCreateTempScriptFiles.class.getSimpleName()));
      scheduler.scheduleJob(jobDetailCreateTempScriptFiles, triggerCreateTempScriptFiles);
    } catch (SchedulerException ex) {
      throw new AppDependencyUpdateRuntimeException(
          schedulerName + " Scheduler Initialization Error", ex);
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

  private JobDetail getJobDetailDeleteTempScriptFiles(String identitySuffix) {
    return JobBuilder.newJob(SchedulerJobDeleteTempScriptFiles.class)
        .withIdentity(SchedulerJobDeleteTempScriptFiles.class.getSimpleName() + identitySuffix)
        .build();
  }

  private JobDetail getJobDetailCreateTempScriptFiles(AppInitData appInitData) {
    JobDataMap jobDataMap = new JobDataMap();
    jobDataMap.put(SchedulerJobCreateTempScriptFiles.APP_INIT_DATA_MAP, appInitData);
    return JobBuilder.newJob(SchedulerJobCreateTempScriptFiles.class)
        .withIdentity(SchedulerJobCreateTempScriptFiles.class.getSimpleName())
        .usingJobData(jobDataMap)
        .build();
  }

  private Properties getProperties(String schedulerName) {
    String fStr = "false";
    Properties properties = new Properties();
    // default properties from quartz.properties
    properties.setProperty("org.quartz.scheduler.instanceName", "Quartz-" + schedulerName);
    properties.setProperty("org.quartz.scheduler.rmi.export", fStr);
    properties.setProperty("org.quartz.scheduler.rmi.proxy", fStr);
    properties.setProperty("org.quartz.scheduler.rmi.wrapJobExecutionInUserTransaction", fStr);
    properties.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
    properties.setProperty("org.quartz.threadPool.threadCount", "10");
    properties.setProperty("org.quartz.threadPool.threadPriority", "5");
    properties.setProperty(
        "org.quartz.threadPool.threadsInheritContextClassLoaderOfInitializingThread", "true");
    properties.setProperty("org.quartz.jobStore.misfireThreshold", "60000");
    properties.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
    // others
    properties.setProperty("org.quartz.scheduler.instanceId", "Scheduler-" + schedulerName);
    properties.setProperty("org.quartz.scheduler.threadName", "QS-" + schedulerName);
    return properties;
  }
}
