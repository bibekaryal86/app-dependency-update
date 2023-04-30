package app.dependency.update.app.service;

import static app.dependency.update.app.util.CommonUtils.*;

import app.dependency.update.app.connector.MavenConnector;
import app.dependency.update.app.model.MavenDoc;
import app.dependency.update.app.model.MavenResponse;
import app.dependency.update.app.model.MavenSearchResponse;
import app.dependency.update.app.model.dto.Dependencies;
import app.dependency.update.app.model.dto.Plugins;
import app.dependency.update.app.repository.DependenciesRepository;
import app.dependency.update.app.repository.PluginsRepository;
import app.dependency.update.app.util.CommonUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MavenRepoService {

  private final PluginsRepository pluginsRepository;
  private final DependenciesRepository dependenciesRepository;
  private final MavenConnector mavenConnector;

  public MavenRepoService(
      final PluginsRepository pluginsRepository,
      final DependenciesRepository dependenciesRepository,
      final MavenConnector mavenConnector) {
    this.pluginsRepository = pluginsRepository;
    this.dependenciesRepository = dependenciesRepository;
    this.mavenConnector = mavenConnector;
  }

  @Cacheable(value = "pluginsMap", unless = "#result==null")
  public Map<String, Plugins> pluginsMap() {
    List<Plugins> plugins = pluginsRepository.findAll();
    log.info("Plugins Map: [ {} ]", plugins.size());
    return plugins.stream().collect(Collectors.toMap(Plugins::getGroup, plugin -> plugin));
  }

  @CacheEvict(value = "pluginsMap", allEntries = true, beforeInvocation = true)
  public void clearPluginsMap() {
    log.info("Clear Plugins Map...");
  }

  @CacheEvict(value = "pluginsMap", allEntries = true, beforeInvocation = true)
  public void savePlugin(final Plugins plugin) {
    log.info("Save Plugin: [ {} ]", plugin);
    pluginsRepository.save(plugin);
  }

  @Cacheable(value = "dependenciesMap", unless = "#result==null")
  public Map<String, Dependencies> dependenciesMap() {
    List<Dependencies> dependencies = dependenciesRepository.findAll();
    log.info("Dependencies Map: [ {} ]", dependencies.size());
    return dependencies.stream()
        .collect(Collectors.toMap(Dependencies::getMavenId, dependency -> dependency));
  }

  @CacheEvict(value = "dependenciesMap", allEntries = true, beforeInvocation = true)
  public void clearDependenciesMap() {
    log.info("Clear Dependencies Map...");
  }

  @CacheEvict(value = "dependenciesMap", allEntries = true, beforeInvocation = true)
  public void saveDependency(final Dependencies dependency) {
    log.info("Save Dependency: [ {} ]", dependency);
    dependenciesRepository.save(dependency);
  }

  @CacheEvict(value = "dependenciesMap", allEntries = true, beforeInvocation = true)
  public void updateDependenciesInMongo() {
    // get from Mongo than local cache
    List<Dependencies> dependencies = dependenciesRepository.findAll();
    List<Dependencies> dependenciesToUpdate = new ArrayList<>();

    dependencies.forEach(
        dependency -> {
          String[] mavenIdArray = dependency.getMavenId().split(":");
          String currentVersion = dependency.getLatestVersion();
          // get current version from Maven Central Repository
          String latestVersion =
              getLatestVersion(mavenIdArray[0], mavenIdArray[1], currentVersion, true);
          // check if local maven repo needs updating
          if (isRequiresUpdate(currentVersion, latestVersion)) {
            dependenciesToUpdate.add(
                Dependencies.builder()
                    .id(dependenciesMap().get(dependency.getMavenId()).getId())
                    .mavenId(dependency.getMavenId())
                    .latestVersion(latestVersion)
                    .build());
          }
        });

    log.info(
        "Mongo Dependencies to Update: [{}]\n[{}]",
        dependenciesToUpdate.size(),
        dependenciesToUpdate);

    if (!dependenciesToUpdate.isEmpty()) {
      dependenciesRepository.saveAll(dependenciesToUpdate);
    }
  }

  public String getLatestVersion(
      final String group,
      final String artifact,
      final String currentVersion,
      final boolean forceRemote) {

    if (forceRemote) {
      return getLatestVersion(group, artifact, currentVersion);
    }

    String mavenId = group + ":" + artifact;
    Dependencies dependencyInCache = dependenciesMap().get(mavenId);

    if (dependencyInCache != null) {
      return dependencyInCache.getLatestVersion();
    }

    // the group:artifact likely does not exist in the cache yet
    // so get it from maven central repository
    String latestVersion = getLatestVersion(group, artifact, currentVersion);
    // save to mongodb as well
    dependenciesRepository.save(
        Dependencies.builder().mavenId(mavenId).latestVersion(latestVersion).build());
    return latestVersion;
  }

  private String getLatestVersion(
      final String group, final String artifact, final String currentVersion) {
    MavenSearchResponse mavenSearchResponse =
        mavenConnector.getMavenSearchResponse(group, artifact);
    MavenDoc mavenDoc = getLatestVersion(mavenSearchResponse);
    log.info(
        "Maven Search Response: [ {} ], [ {} ], [ {} ], [ {} ]",
        group,
        artifact,
        mavenDoc,
        mavenSearchResponse);

    if (mavenDoc == null) {
      return currentVersion;
    }
    return mavenDoc.getV();
  }

  private MavenDoc getLatestVersion(final MavenSearchResponse mavenSearchResponse) {
    // the search returns 5 latest, filter to not get RC or alpha/beta or unfinished releases
    // the search returns sorted list already, but need to filter and get max after
    if (mavenSearchResponse != null
        && mavenSearchResponse.getResponse() != null
        && !isEmpty(mavenSearchResponse.getResponse().getDocs())) {
      MavenResponse mavenResponse = mavenSearchResponse.getResponse();
      return mavenResponse.getDocs().stream()
          .filter(mavenDoc -> !isCheckPreReleaseVersion(mavenDoc.getV()))
          .max(
              Comparator.comparing(
                  MavenDoc::getV, Comparator.comparing(CommonUtils::getVersionToCompare)))
          .orElse(null);
    }
    return null;
  }

  private boolean isCheckPreReleaseVersion(final String version) {
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
}
