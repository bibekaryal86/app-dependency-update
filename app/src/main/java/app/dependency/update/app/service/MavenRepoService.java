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
  private final GradleRepoService gradleRepoService;

  public MavenRepoService(
      final PluginsRepository pluginsRepository,
      final DependenciesRepository dependenciesRepository,
      final MavenConnector mavenConnector,
      final GradleRepoService gradleRepoService) {
    this.pluginsRepository = pluginsRepository;
    this.dependenciesRepository = dependenciesRepository;
    this.mavenConnector = mavenConnector;
    this.gradleRepoService = gradleRepoService;
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

  public void savePlugin(final String group, final String version) {
    log.info("Save Plugin: [ {} ] | [ {} ]", group, version);
    try {
      pluginsRepository.save(
          Plugins.builder().group(group).version(version).skipVersion(false).build());
    } catch (Exception ex) {
      log.error("ERROR Save Plugin: [ {} ] | [ {} ]", group, version, ex);
    }
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

  // save dependency, no cache evict
  public void saveDependency(final String mavenId, final String latestVersion) {
    log.info("Save Dependency: [ {} ] | [ {} ]", mavenId, latestVersion);
    try {
      dependenciesRepository.save(
          Dependencies.builder()
              .mavenId(mavenId)
              .latestVersion(latestVersion)
              .skipVersion(false)
              .build());
    } catch (Exception ex) {
      log.error("ERROR Save Dependency: [ {} ] | [ {} ]", mavenId, latestVersion, ex);
    }
  }

  @CacheEvict(value = "pluginsMap", allEntries = true, beforeInvocation = true)
  public void updatePluginsInMongo() {
    Map<String, Plugins> pluginsLocal = pluginsMap();
    List<Plugins> plugins = pluginsRepository.findAll();
    List<Plugins> pluginsToUpdate = new ArrayList<>();

    plugins.forEach(
        plugin -> {
          String group = plugin.getGroup();
          String currentVersion = plugin.getVersion();
          // get latest version from Gradle Plugin Repository
          String latestVersion = gradleRepoService.getLatestGradlePlugin(group);
          // check if local maven repo needs updating
          if (isRequiresUpdate(currentVersion, latestVersion)) {
            pluginsToUpdate.add(
                Plugins.builder()
                    .id(pluginsLocal.get(plugin.getGroup()).getId())
                    .group(plugin.getGroup())
                    .version(latestVersion)
                    .skipVersion(false)
                    .build());
          }
        });

    log.info("Mongo Plugins to Update: [{}]\n[{}]", pluginsToUpdate.size(), pluginsToUpdate);

    if (!pluginsToUpdate.isEmpty()) {
      pluginsRepository.saveAll(pluginsToUpdate);
      log.info("Mongo Plugins Updated...");
    }
  }

  @CacheEvict(value = "dependenciesMap", allEntries = true, beforeInvocation = true)
  public void updateDependenciesInMongo() {
    Map<String, Dependencies> dependenciesLocal = dependenciesMap();
    List<Dependencies> dependencies = dependenciesRepository.findAll();
    List<Dependencies> dependenciesToUpdate = new ArrayList<>();

    dependencies.forEach(
        dependency -> {
          String[] mavenIdArray = dependency.getMavenId().split(":");
          String currentVersion = dependency.getLatestVersion();
          // get current version from Maven Central Repository
          String latestVersion =
              getLatestDependencyVersion(mavenIdArray[0], mavenIdArray[1], currentVersion);
          // check if local maven repo needs updating
          if (isRequiresUpdate(currentVersion, latestVersion)) {
            dependenciesToUpdate.add(
                Dependencies.builder()
                    .id(dependenciesLocal.get(dependency.getMavenId()).getId())
                    .mavenId(dependency.getMavenId())
                    .latestVersion(latestVersion)
                    .skipVersion(false)
                    // set skipVersion as false when bumping to a new version
                    .build());
          }
        });

    log.info(
        "Mongo Dependencies to Update: [{}]\n[{}]",
        dependenciesToUpdate.size(),
        dependenciesToUpdate);

    if (!dependenciesToUpdate.isEmpty()) {
      dependenciesRepository.saveAll(dependenciesToUpdate);
      log.info("Mongo Dependencies Updated...");
    }
  }

  private String getLatestDependencyVersion(
      final String group, final String artifact, final String currentVersion) {

    String mavenId = group + ":" + artifact;

    // the group:artifact likely does not exist in the cache yet
    // so get it from maven central repository
    MavenSearchResponse mavenSearchResponse =
        mavenConnector.getMavenSearchResponse(group, artifact);
    MavenDoc mavenDoc = getLatestDependencyVersion(mavenSearchResponse);
    log.debug(
        "Maven Search Response: [ {} ], [ {} ], [ {} ], [ {} ]",
        group,
        artifact,
        mavenDoc,
        mavenSearchResponse);

    if (mavenDoc == null) {
      return currentVersion;
    }

    String latestVersion = mavenDoc.getV();
    // save to mongodb as well
    saveDependency(mavenId, latestVersion);
    return latestVersion;
  }

  private MavenDoc getLatestDependencyVersion(final MavenSearchResponse mavenSearchResponse) {
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
}
