package app.dependency.update.app.util;

import app.dependency.update.app.model.MavenSearchResponse;
import app.dependency.update.app.model.MongoDependencies;
import app.dependency.update.app.model.MongoPlugins;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MavenRepoUtil {
  private static Map<String, String> pluginsMap = new ConcurrentHashMap<>();
  private static Map<String, String> dependenciesMap = new ConcurrentHashMap<>();

  public static Map<String, String> getPluginsMap() {
    if (CommonUtil.isEmpty(pluginsMap)) {
      return setPluginsMap();
    }
    return pluginsMap;
  }

  public static Map<String, String> getDependenciesMap() {
    if (CommonUtil.isEmpty(dependenciesMap)) {
      return setDependenciesMap();
    }
    return dependenciesMap;
  }

  public static String getLatestVersion(String group, String artifact, String currentVersion) {
    // plugins do not have artifact information, so get artifact from pluginsMap
    if (CommonUtil.isEmpty(artifact)) {
      artifact = getPluginsMap().get(group);

      if (CommonUtil.isEmpty(artifact)) {
        // If it is still null, it is likely plugin information is not available in the local
        // repository
        // Do not throw error, log the event and continue updating others
        log.error("Plugin information missing: {}", group);
        return currentVersion;
      }
    }

    String mavenId = group + ":" + artifact;

    if (getDependenciesMap().get(mavenId) != null) {
      // return from dependencies map from local repo
      return getDependenciesMap().get(mavenId);
    }

    // the group:artifact likely does not exist in the local repo yet
    // so get it from maven central repository
    String latestVersion = getLatestVersion(group, artifact);

    if (latestVersion == null) {
      latestVersion = currentVersion;
    }

    return latestVersion;
  }

  private static String getLatestVersion(String group, String artifact) {
    MavenSearchResponse mavenSearchResponse = getMavenSearchResponse(group, artifact);
    log.info("Maven Search Response: [{}:{}], {}", group, artifact, mavenSearchResponse);

    if (mavenSearchResponse != null
        && mavenSearchResponse.getResponse() != null
        && !CommonUtil.isEmpty(mavenSearchResponse.getResponse().getDocs())) {
      String latestVersion = mavenSearchResponse.getResponse().getDocs().get(0).getLatestVersion();
      // save to local repo as well
      String mavenId = group + ":" + artifact;
      MongoUtil.insertDependencies(
          Collections.singletonList(
              MongoDependencies.builder().id(mavenId).latestVersion(latestVersion).build()));
      dependenciesMap.putIfAbsent(mavenId, latestVersion);
      return latestVersion;
    }
    return null;
  }

  private static MavenSearchResponse getMavenSearchResponse(String group, String artifact) {
    return (MavenSearchResponse)
        ConnectorUtil.sendHttpRequest(
            String.format(CommonUtil.MAVEN_SEARCH_ENDPOINT, group, artifact),
            CommonUtil.HttpMethod.GET,
            null,
            null,
            null,
            MavenSearchResponse.class);
  }

  private static Map<String, String> setPluginsMap() {
    List<MongoPlugins> mongoPlugins = MongoUtil.retrievePlugins();
    pluginsMap =
        mongoPlugins.stream()
            .collect(Collectors.toMap(MongoPlugins::getGroup, MongoPlugins::getArtifact));
    log.info("Set Plugin Map: {}", pluginsMap);
    return pluginsMap;
  }

  private static Map<String, String> setDependenciesMap() {
    List<MongoDependencies> mongoDependencies = MongoUtil.retrieveDependencies();
    dependenciesMap =
        mongoDependencies.stream()
            .collect(
                Collectors.toMap(MongoDependencies::getId, MongoDependencies::getLatestVersion));
    log.info("Set Dependencies Map: {}", dependenciesMap);
    return dependenciesMap;
  }
}
