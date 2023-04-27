package app.dependency.update.app.config;

import static app.dependency.update.app.util.CommonUtils.*;

import app.dependency.update.app.service.AppInitDataService;
import app.dependency.update.app.service.MavenRepoService;
import app.dependency.update.app.service.ScriptFilesService;
import app.dependency.update.app.service.UpdateRepoService;
import app.dependency.update.app.util.ThreadMonitor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
public class SchedulerConfig {

  private final ThreadMonitor threadMonitor;
  private final AppInitDataService appInitDataService;
  private final MavenRepoService mavenRepoService;
  private final ScriptFilesService scriptFilesService;
  private final UpdateRepoService updateRepoService;

  public SchedulerConfig(
      final ThreadMonitor threadMonitor,
      final AppInitDataService appInitDataService,
      final MavenRepoService mavenRepoService,
      final ScriptFilesService scriptFilesService,
      final UpdateRepoService updateRepoService) {
    this.threadMonitor = threadMonitor;
    this.appInitDataService = appInitDataService;
    this.mavenRepoService = mavenRepoService;
    this.scriptFilesService = scriptFilesService;
    this.updateRepoService = updateRepoService;
  }

  // run every five seconds
  @Scheduled(cron = "5 * * * * *")
  private void schedulerThreadMonitor() {
    threadMonitor.monitorThreads();
  }

  // first minute
  @Scheduled(cron = "0 1 20 * * *")
  private void schedulerAppInitData() {
    appInitDataService.clearAppInitData();
    appInitDataService.appInitData();
  }

  // first minute
  @Scheduled(cron = "0 1 20 * * *")
  private void schedulerPluginsMap() {
    mavenRepoService.clearPluginsMap();
    mavenRepoService.pluginsMap();
  }

  // first minute
  @Scheduled(cron = "0 1 20 * * *")
  private void schedulerDependenciesMap() {
    mavenRepoService.clearDependenciesMap();
    mavenRepoService.updateDependenciesInMongo();
    mavenRepoService.dependenciesMap();
  }

  // second minute
  @Scheduled(cron = "0 2 20 * * *")
  private void schedulerDeleteTempScriptFilesBegin() {
    scriptFilesService.deleteTempScriptFiles();
  }

  // third minute
  @Scheduled(cron = "0 3 20 * * *")
  private void schedulerCreateTempScriptFiles() {
    scriptFilesService.createTempScriptFiles();
  }

  // fourth minute
  @Scheduled(cron = "0 4 20 * * *")
  private void schedulerGithubLocalPullBegin() {
    updateRepoService.updateRepos(UpdateType.GITHUB_PULL, false, null);
  }

  // fifth minute
  @Scheduled(cron = "0 5 20 * * *")
  private void schedulerGradleWrapperUpdate() {
    updateRepoService.updateRepos(UpdateType.GRADLE_WRAPPER, false, null);
  }

  // twenty-second minute
  @Scheduled(cron = "0 22 20 * * *")
  private void schedulerGithubPullRequestMergeWrapper() {
    updateRepoService.updateRepos(UpdateType.GITHUB_MERGE, false, null);
  }

  // twenty-fifth minute
  @Scheduled(cron = "0 25 20 * * *")
  private void schedulerGithubLocalPullMiddle() {
    updateRepoService.updateRepos(UpdateType.GITHUB_PULL, false, null);
  }

  // thirtieth minute
  @Scheduled(cron = "0 30 20 * * *")
  private void schedulerNpmDependenciesUpdate() {
    updateRepoService.updateRepos(UpdateType.NPM_DEPENDENCIES, false, null);
  }

  // thirty-fifth minute
  @Scheduled(cron = "0 35 20 * * *")
  private void schedulerGradleDependenciesUpdate() {
    updateRepoService.updateRepos(UpdateType.GRADLE_DEPENDENCIES, false, null);
  }

  // fifty-second minute
  @Scheduled(cron = "0 52 20 * * *")
  private void schedulerGithubPullRequestMergeDependencies() {
    updateRepoService.updateRepos(UpdateType.GITHUB_MERGE, false, null);
  }

  // fifty-fifth minute
  @Scheduled(cron = "0 55 20 * * *")
  private void schedulerGithubLocalPullEnd() {
    updateRepoService.updateRepos(UpdateType.GITHUB_PULL, false, null);
  }

  // fifty-ninth minute
  @Scheduled(cron = "0 59 20 * * *")
  private void schedulerDeleteTempScriptFilesEnd() {
    scriptFilesService.deleteTempScriptFiles();
  }
}
