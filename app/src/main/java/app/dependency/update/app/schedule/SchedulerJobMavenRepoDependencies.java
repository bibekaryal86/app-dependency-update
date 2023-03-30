package app.dependency.update.app.schedule;

import app.dependency.update.app.execute.MavenRepo;
import app.dependency.update.app.model.MongoDependency;
import app.dependency.update.app.util.CommonUtil;
import app.dependency.update.app.util.MongoUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

@Slf4j
public class SchedulerJobMavenRepoDependencies implements Job {

  @Override
  public void execute(final JobExecutionContext context) throws JobExecutionException {
    log.info("Start SchedulerJobMavenRepoDependencies...");
    CompletableFuture.runAsync(this::execute);
  }

  private void execute() {
    // get the list of dependencies that needs updating
    List<MongoDependency> mongoDependencies = findMongoDependenciesToUpdate();
    // update the mongo maven repo
    MongoUtil.updateDependencies(mongoDependencies);
    // set dependencies map
    new MavenRepo().setDependenciesMap();
    log.info("Finish SchedulerJobMavenRepoDependencies...");
  }

  private List<MongoDependency> findMongoDependenciesToUpdate() {
    // get the dependencies, best to get it from mongo than from local Map
    List<MongoDependency> mongoDependencies = MongoUtil.retrieveDependencies();
    List<MongoDependency> mongoDependenciesToUpdate = new ArrayList<>();

    mongoDependencies.forEach(
        mongoDependency -> {
          String[] mavenIdArray = mongoDependency.getId().split(":");
          String currentVersion = mongoDependency.getLatestVersion();
          // get current version from Maven Central Repository
          String latestVersion =
              new MavenRepo()
                  .getLatestVersion(mavenIdArray[0], mavenIdArray[1], currentVersion, true);
          // check if local maven repo needs updating
          if (CommonUtil.isRequiresUpdate(currentVersion, latestVersion)) {
            mongoDependenciesToUpdate.add(
                MongoDependency.builder()
                    .id(mongoDependency.getId())
                    .latestVersion(latestVersion)
                    .build());
          }
        });

    log.info(
        "Mongo Dependencies to Update: {}\n{}",
        mongoDependenciesToUpdate.size(),
        mongoDependenciesToUpdate);

    return mongoDependenciesToUpdate;
  }
}
