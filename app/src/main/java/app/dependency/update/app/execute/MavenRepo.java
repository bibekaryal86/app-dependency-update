package app.dependency.update.app.execute;

import app.dependency.update.app.model.MavenDoc;
import app.dependency.update.app.model.MavenSearchResponse;
import app.dependency.update.app.model.MongoDependencies;
import app.dependency.update.app.model.MongoPlugins;
import app.dependency.update.app.util.CommonUtil;
import app.dependency.update.app.util.ConnectorUtil;
import app.dependency.update.app.util.MongoUtil;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MavenRepo {

  public void setPluginsMap() {
    List<MongoPlugins> mongoPlugins = MongoUtil.retrievePlugins();
    Map<String, String> pluginsMap =
        mongoPlugins.stream()
            .collect(Collectors.toMap(MongoPlugins::getGroup, MongoPlugins::getArtifact));
    CommonUtil.setPluginsMap(pluginsMap);
    log.info("Set Plugin Map: {}", pluginsMap);
  }

  public void setDependenciesMap() {
    List<MongoDependencies> mongoDependencies = MongoUtil.retrieveDependencies();
    Map<String, String> dependenciesMap =
        mongoDependencies.stream()
            .collect(
                Collectors.toMap(MongoDependencies::getId, MongoDependencies::getLatestVersion));
    CommonUtil.setDependenciesMap(dependenciesMap);
    log.info("Set Dependencies Map: {}", dependenciesMap);
  }

  public String getLatestVersion(String group, String artifact, String currentVersion) {
    // plugins do not have artifact information, so get artifact from pluginsMap
    if (CommonUtil.isEmpty(artifact)) {
      artifact = CommonUtil.getPluginsMap().get(group);

      // check again
      if (CommonUtil.isEmpty(artifact)) {
        // Still nothing? it is likely plugin information is not available in the local repository
        // Do not throw error, log the event and continue updating others
        log.error("Plugin information missing: {}", group);
        return currentVersion;
      }
    }

    String mavenId = group + ":" + artifact;

    if (CommonUtil.getDependenciesMap().get(mavenId) != null) {
      // return from dependencies map from local repo
      return CommonUtil.getDependenciesMap().get(mavenId);
    }

    // the group:artifact likely does not exist in the local repo yet
    // so get it from maven central repository
    MavenDoc latestVersion = getLatestVersion(group, artifact);

    if (latestVersion == null) {
      return currentVersion;
    }

    // save to local maven repo as well
    MongoUtil.insertDependencies(
        Collections.singletonList(
            MongoDependencies.builder()
                .id(mavenId)
                .latestVersion(latestVersion.getLatestVersion())
                .build()));

    return latestVersion.getLatestVersion();
  }

  private MavenDoc getLatestVersion(String group, String artifact) {
    MavenSearchResponse mavenSearchResponse = getMavenSearchResponse(group, artifact);
    log.info("Maven Search Response: [{}:{}], {}", group, artifact, mavenSearchResponse);
    return getLatestVersion(mavenSearchResponse);
  }

  private MavenDoc getLatestVersion(MavenSearchResponse mavenSearchResponse) {
    // the search returns 5 latest, filter to not get RC or alpha/beta or unfinished releases
    // the search returns sorted list already, but need to filter and get max after
    if (mavenSearchResponse != null
        && mavenSearchResponse.getResponse() != null
        && !CommonUtil.isEmpty(mavenSearchResponse.getResponse().getDocs())) {
      return mavenSearchResponse.getResponse().getDocs().stream()
          .filter(mavenDoc -> !isCheckContains(mavenDoc.getLatestVersion()))
          .max(Comparator.comparing(MavenDoc::getLatestVersion))
          .orElse(null);
    }
    return null;
  }

  private boolean isCheckContains(String version) {
    return version.contains("alpha")
        || version.contains("ALPHA")
        || version.contains("b")
        || version.contains("beta")
        || version.contains("BETA")
        || version.contains("rc")
        || version.contains("RC")
        || version.contains("snapshot")
        || version.contains("SNAPSHOT");
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
