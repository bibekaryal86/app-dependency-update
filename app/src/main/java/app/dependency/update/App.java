/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package app.dependency.update;

import app.dependency.update.app.execute.CreateTempScriptFiles;
import app.dependency.update.app.execute.DeleteTempScriptFiles;
import app.dependency.update.app.execute.GetAppInitData;
import app.dependency.update.app.execute.ThreadMonitor;
import app.dependency.update.app.model.AppInitData;
import app.dependency.update.app.schedule.AppScheduler;
import app.dependency.update.app.update.UpdateDependencies;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class App {
  public static void main(String[] args) {
    log.info("Begin app-dependency-update initialization...");

    // start scheduler
    new AppScheduler().startScheduler();
    // delete temp folders if exist at the beginning
    new DeleteTempScriptFiles();
    // get initial app data
    final AppInitData appInitData = new GetAppInitData(args).getAppInitData();
    // create temp script files
    new CreateTempScriptFiles(appInitData.getScriptFiles()).createTempScriptFiles();
    // update dependencies
    new UpdateDependencies(appInitData).updateDependencies();
    // monitor threads
    new ThreadMonitor(appInitData);

    log.info("End app-dependency-update initialization...");
  }
}
