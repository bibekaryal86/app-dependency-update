package app.dependency.update.app.util;

import app.dependency.update.app.model.GradlePlugin;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GradlePluginUtil {
  private static Map<String, String> pluginGroupArtifactMap = null;

  public static Map<String, String> getPluginGroupArtifactMap() {
    return Objects.requireNonNullElseGet(
        pluginGroupArtifactMap, GradlePluginUtil::setPluginGroupArtifactMap);
  }

  private static Map<String, String> setPluginGroupArtifactMap() {
    List<GradlePlugin> gradlePlugins = MongoUtil.retrieveGradlePlugins();
    pluginGroupArtifactMap =
        gradlePlugins.stream()
            .collect(Collectors.toMap(GradlePlugin::getGroup, GradlePlugin::getArtifact));
    log.info("Set Gradle Plugin Group Artifact Map: {}", pluginGroupArtifactMap);
    return pluginGroupArtifactMap;
  }
}
