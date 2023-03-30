package app.dependency.update.app.execute;

import app.dependency.update.app.model.BuildGradleConfigs;
import app.dependency.update.app.model.GradleConfigBlock;
import app.dependency.update.app.model.GradleDependency;
import app.dependency.update.app.util.CommonUtil;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ModifyBuildGradle {

  private final BuildGradleConfigs buildGradleConfigs;

  public ModifyBuildGradle(final BuildGradleConfigs buildGradleConfigs) {
    this.buildGradleConfigs = buildGradleConfigs;
  }

  public void modifyBuildGradle() {
    log.info("BEFORE CHANGE: [{}]", this.buildGradleConfigs.getOriginals());
    final List<String> originals = this.buildGradleConfigs.getOriginals();
    final GradleConfigBlock pluginsBlock = this.buildGradleConfigs.getPlugins();

    if (pluginsBlock != null && !pluginsBlock.getDependencies().isEmpty()) {
      for (final GradleDependency gradleDependency : pluginsBlock.getDependencies()) {
        String updatedOriginal = checkGradlePluginRequiresUpdate(gradleDependency);
        if (updatedOriginal != null) {
          int index = originals.indexOf(gradleDependency.getOriginal());
          originals.set(index, updatedOriginal);
        }
      }
    }

    log.info("AFTER CHANGE: [{}]", originals);
  }

  private String checkGradlePluginRequiresUpdate(final GradleDependency gradleDependency) {
    String latestVersion =
        new MavenRepo()
            .getLatestVersion(
                gradleDependency.getGroup(),
                gradleDependency.getArtifact(),
                gradleDependency.getVersion(),
                false);

    if (CommonUtil.isRequiresUpdate(gradleDependency.getVersion(), latestVersion)) {
      return gradleDependency.getOriginal().replace(gradleDependency.getVersion(), latestVersion);
    }

    return null;
  }
}
