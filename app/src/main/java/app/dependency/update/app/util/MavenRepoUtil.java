package app.dependency.update.app.util;

import app.dependency.update.app.model.MavenSearchResponse;
import app.dependency.update.app.model.MongoDependencies;
import app.dependency.update.app.model.MongoPlugins;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MavenRepoUtil {
  private static Map<String, String> pluginsMap = null;
  private static Map<String, String> dependenciesMap = null;

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

  public static String getLatestVersion(String group, String artifact, String currentVersion) {
    String mavenId = group + ":" + artifact;

    if (getDependenciesMap().get(mavenId) != null) {
      return getDependenciesMap().get(mavenId);
    }

    MavenSearchResponse mavenSearchResponse = getMavenSearchResponse(group, artifact);
    log.info("Maven Search Response: [{}:{}], {}", group, artifact, mavenSearchResponse);

    if (mavenSearchResponse != null
        && mavenSearchResponse.getResponse() != null
        && !CommonUtil.isEmpty(mavenSearchResponse.getResponse().getDocs())) {
      // there could be more than one matching response, but that is highly unlikely
      // also highly likely that the build will fair after PR in that scenario
      // so get the first element in the list
      dependenciesMap.put(
          mavenId, mavenSearchResponse.getResponse().getDocs().get(0).getLatestVersion());
      return mavenSearchResponse.getResponse().getDocs().get(0).getLatestVersion();
    }

    dependenciesMap.put(mavenId, currentVersion);
    return currentVersion;
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
}
