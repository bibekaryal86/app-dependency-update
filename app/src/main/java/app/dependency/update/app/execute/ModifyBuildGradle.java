package app.dependency.update.app.execute;

import app.dependency.update.app.model.BuildGradleConfigs;
import app.dependency.update.app.model.GradleConfigBlock;
import app.dependency.update.app.model.GradleDependency;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ModifyBuildGradle {

  private final BuildGradleConfigs buildGradleConfigs;

  public ModifyBuildGradle(final BuildGradleConfigs buildGradleConfigs) {
    this.buildGradleConfigs = buildGradleConfigs;
  }

  public void modifyBuildGradle() {
    GradleConfigBlock pluginsBlock = this.buildGradleConfigs.getPlugins();
    if (pluginsBlock != null && !pluginsBlock.getDependencies().isEmpty()) {}
  }

  public void checkGradlePluginRequiresUpdate(final GradleDependency gradleDependency) {}
}
