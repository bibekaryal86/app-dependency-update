package app.dependency.update.app.service;

import static app.dependency.update.app.util.CommonUtils.*;

import app.dependency.update.app.connector.MavenConnector;
import app.dependency.update.app.model.LatestVersionsModel;
import app.dependency.update.app.model.MavenDoc;
import app.dependency.update.app.model.MavenResponse;
import app.dependency.update.app.model.MavenSearchResponse;
import app.dependency.update.app.model.MongoProcessSummaries;
import app.dependency.update.app.model.ProcessSummary;
import app.dependency.update.app.model.entities.Dependencies;
import app.dependency.update.app.model.entities.LatestVersionsEntity;
import app.dependency.update.app.model.entities.NpmSkips;
import app.dependency.update.app.model.entities.Packages;
import app.dependency.update.app.model.entities.Plugins;
import app.dependency.update.app.model.entities.ProcessSummaries;
import app.dependency.update.app.repository.DependenciesRepository;
import app.dependency.update.app.repository.LatestVersionsRepository;
import app.dependency.update.app.repository.NpmSkipsRepository;
import app.dependency.update.app.repository.PackagesRepository;
import app.dependency.update.app.repository.PluginsRepository;
import app.dependency.update.app.repository.ProcessSummariesRepository;
import app.dependency.update.app.util.CommonUtils;
import app.dependency.update.app.util.ProcessUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MongoRepoService {

  private final PluginsRepository pluginsRepository;
  private final DependenciesRepository dependenciesRepository;
  private final PackagesRepository packagesRepository;
  private final NpmSkipsRepository npmSkipsRepository;
  private final ProcessSummariesRepository processSummariesRepository;
  private final LatestVersionsRepository latestVersionsRepository;
  private final MavenConnector mavenConnector;
  private final GradleRepoService gradleRepoService;
  private final PypiRepoService pypiRepoService;

  public MongoRepoService(
      final PluginsRepository pluginsRepository,
      final DependenciesRepository dependenciesRepository,
      final PackagesRepository packagesRepository,
      final NpmSkipsRepository npmSkipsRepository,
      final ProcessSummariesRepository processSummariesRepository,
      final LatestVersionsRepository latestVersionsRepository,
      final MavenConnector mavenConnector,
      final GradleRepoService gradleRepoService,
      final PypiRepoService pypiRepoService) {
    this.pluginsRepository = pluginsRepository;
    this.dependenciesRepository = dependenciesRepository;
    this.packagesRepository = packagesRepository;
    this.npmSkipsRepository = npmSkipsRepository;
    this.processSummariesRepository = processSummariesRepository;
    this.latestVersionsRepository = latestVersionsRepository;
    this.mavenConnector = mavenConnector;
    this.gradleRepoService = gradleRepoService;
    this.pypiRepoService = pypiRepoService;
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

  // save plugin, no cache evict
  // reason: called from middle of execution, will be reset at the end
  public void savePlugin(final String group, final String version) {
    log.info("Save Plugin: [ {} ] | [ {} ]", group, version);
    try {
      pluginsRepository.save(
          Plugins.builder().group(group).version(version).skipVersion(false).build());
    } catch (Exception ex) {
      ProcessUtils.setErrorsOrExceptions(true);
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
  // reason: called from middle of execution, will be reset at the end
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
      ProcessUtils.setErrorsOrExceptions(true);
      log.error("ERROR Save Dependency: [ {} ] | [ {} ]", mavenId, latestVersion, ex);
    }
  }

  @Cacheable(value = "packagesMap", unless = "#result==null")
  public Map<String, Packages> packagesMap() {
    List<Packages> packages = packagesRepository.findAll();
    log.info("Packages Map: [ {} ]", packages.size());
    return packages.stream().collect(Collectors.toMap(Packages::getName, onePackage -> onePackage));
  }

  @CacheEvict(value = "packagesMap", allEntries = true, beforeInvocation = true)
  public void clearPackagesMap() {
    log.info("Clear Packages Map...");
  }

  @CacheEvict(value = "packagesMap", allEntries = true, beforeInvocation = true)
  public void savePackage(final Packages onePackage) {
    log.info("Save Package: [ {} ]", onePackage);
    packagesRepository.save(onePackage);
  }

  // save package, no cache evict
  // reason: called from middle of execution, will be reset at the end
  public void savePackage(final String name, final String version) {
    log.info("Save Package: [ {} ] | [ {} ]", name, version);
    try {
      packagesRepository.save(
          Packages.builder().name(name).version(version).skipVersion(false).build());
    } catch (Exception ex) {
      ProcessUtils.setErrorsOrExceptions(true);
      log.error("ERROR Save Package: [ {} ] | [ {} ]", name, version, ex);
    }
  }

  @Cacheable(value = "npmSkipsMap", unless = "#result==null")
  public Map<String, NpmSkips> npmSkipsMap() {
    List<NpmSkips> npmSkips = npmSkipsRepository.findAll();
    log.info("NpmSkips Map: [ {} ]", npmSkips.size());
    return npmSkips.stream().collect(Collectors.toMap(NpmSkips::getName, npmSkip -> npmSkip));
  }

  @CacheEvict(value = "npmSkipsMap", allEntries = true, beforeInvocation = true)
  public void clearNpmSkipsMap() {
    log.info("Clear NpmSkips Map...");
  }

  @CacheEvict(value = "npmSkipsMap", allEntries = true, beforeInvocation = true)
  public void saveNpmSkip(final NpmSkips npmSkip) {
    log.info("Save NpmSkips: [ {} ]", npmSkip);
    npmSkipsRepository.save(npmSkip);
  }

  @CacheEvict(value = "pluginsMap", allEntries = true, beforeInvocation = true)
  public void updatePluginsInMongo(final Map<String, Plugins> pluginsLocal) {
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
      ProcessUtils.setMongoPluginsToUpdate(pluginsToUpdate.size());
    }
  }

  @CacheEvict(value = "dependenciesMap", allEntries = true, beforeInvocation = true)
  public void updateDependenciesInMongo(final Map<String, Dependencies> dependenciesLocal) {
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
      ProcessUtils.setMongoDependenciesToUpdate(dependenciesToUpdate.size());
    }
  }

  @CacheEvict(value = "packagesMap", allEntries = true, beforeInvocation = true)
  public void updatePackagesInMongo(final Map<String, Packages> packagesLocal) {
    List<Packages> packages = packagesRepository.findAll();
    List<Packages> packagesToUpdate = new ArrayList<>();

    packages.forEach(
        onePackage -> {
          String name = onePackage.getName();
          String currentVersion = onePackage.getVersion();
          // get latest version from Pypi Search
          String latestVersion = pypiRepoService.getLatestPackageVersion(name);
          // check if local maven repo needs updating
          if (isRequiresUpdate(currentVersion, latestVersion)) {
            packagesToUpdate.add(
                Packages.builder()
                    .id(packagesLocal.get(onePackage.getName()).getId())
                    .name(onePackage.getName())
                    .version(latestVersion)
                    .skipVersion(false)
                    .build());
          }
        });

    log.info("Mongo Packages to Update: [{}]\n[{}]", packagesToUpdate.size(), packagesToUpdate);

    if (!packagesToUpdate.isEmpty()) {
      packagesRepository.saveAll(packagesToUpdate);
      log.info("Mongo Packages Updated...");
      ProcessUtils.setMongoPackagesToUpdate(packagesToUpdate.size());
    }
  }

  @Scheduled(cron = "0 0 10 * * *") // Runs daily at midnight
  public void deleteOldDocuments() {
    LocalDateTime oneMonthAgo = LocalDateTime.now().minusDays(45);
    processSummariesRepository.deleteByUpdateDateTimeBefore(oneMonthAgo);
    log.info("Deleted Process Summaries before: [{}]", oneMonthAgo);
  }

  public MongoProcessSummaries getProcessSummaries(
      final String updateType,
      final LocalDate updateDate,
      final int pageNumber,
      final int pageSize) {
    log.debug(
        "Get Process Summaries: [ {} ] | [ {} ] | [ {} ] | [ {} ]",
        updateType,
        updateDate,
        pageNumber,
        pageSize);

    Pageable pageable = PageRequest.of(pageNumber, pageSize);

    if (updateDate != null) {
      LocalDateTime startOfDay = updateDate.atStartOfDay();
      LocalDateTime endOfDay = updateDate.atTime(LocalTime.MAX);
      if (!isEmpty(updateType)) {
        List<ProcessSummaries> processSummaries =
            processSummariesRepository.findByUpdateTypeAndUpdateDate(
                updateType, startOfDay, endOfDay);
        return getMongoProcessSummaries(
            processSummaries, 0, 1, processSummaries.size(), processSummaries.size());
      }

      List<ProcessSummaries> processSummaries =
          processSummariesRepository.findByUpdateDate(startOfDay, endOfDay);
      return getMongoProcessSummaries(
          processSummaries, 0, 1, processSummaries.size(), processSummaries.size());
    } else if (!isEmpty(updateType)) {
      Page<ProcessSummaries> processSummariesPage =
          processSummariesRepository.findByUpdateType(updateType, pageable);
      return getMongoProcessSummaries(
          processSummariesPage.getContent(),
          processSummariesPage.getNumber(),
          processSummariesPage.getTotalPages(),
          (int) processSummariesPage.getTotalElements(),
          processSummariesPage.getSize());
    } else {
      Page<ProcessSummaries> processSummariesPage = processSummariesRepository.findAll(pageable);
      return getMongoProcessSummaries(
          processSummariesPage.getContent(),
          processSummariesPage.getNumber(),
          processSummariesPage.getTotalPages(),
          (int) processSummariesPage.getTotalElements(),
          processSummariesPage.getSize());
    }
  }

  public void saveProcessSummaries(final ProcessSummaries processSummaries) {
    log.debug("Save Process Summaries: [ {} ]", processSummaries);
    processSummariesRepository.save(processSummaries);
  }

  public void saveLatestVersions(final LatestVersionsModel latestVersionsModel) {
    log.info("Save Latest Version: [{}]", latestVersionsModel);
    LatestVersionsEntity latestVersionsEntity =
        LatestVersionsEntity.builder()
            .nginx(latestVersionsModel.getLatestVersionAppServers().getNginx())
            .gradle(latestVersionsModel.getLatestVersionBuildTools().getGradle())
            .checkout(latestVersionsModel.getLatestVersionGithubActions().getCheckout())
            .setupJava(latestVersionsModel.getLatestVersionGithubActions().getSetupJava())
            .setupGradle(latestVersionsModel.getLatestVersionGithubActions().getSetupGradle())
            .setupNode(latestVersionsModel.getLatestVersionGithubActions().getSetupNode())
            .setupPython(latestVersionsModel.getLatestVersionGithubActions().getSetupPython())
            .codeql(latestVersionsModel.getLatestVersionGithubActions().getCodeql())
            .java(latestVersionsModel.getLatestVersionLanguages().getJava())
            .node(latestVersionsModel.getLatestVersionLanguages().getNode())
            .python(latestVersionsModel.getLatestVersionLanguages().getPython())
            .updateDateTime(LocalDateTime.now())
            .build();
    latestVersionsRepository.save(latestVersionsEntity);
  }

  public Optional<LatestVersionsEntity> getMostRecentLatestVersionsEntity() {
    log.debug("Get Most Recent Latest Versions Entity...");
    return latestVersionsRepository.findFirstByOrderByUpdateDateTimeDesc();
  }

  private MongoProcessSummaries getMongoProcessSummaries(
      final List<ProcessSummaries> processSummaries,
      final int currentPage,
      final int totalPages,
      final int totalElements,
      final int pageSize) {
    List<ProcessSummary> processSummaryList =
        processSummaries.stream()
            .map(
                processSummaryMongo -> {
                  ProcessSummary processSummary = ProcessSummary.builder().build();
                  BeanUtils.copyProperties(processSummaryMongo, processSummary);
                  return processSummary;
                })
            .toList();
    return MongoProcessSummaries.builder()
        .processSummaries(processSummaryList)
        .currentPage(currentPage)
        .totalPages(totalPages)
        .totalElements(totalElements)
        .pageSize(pageSize)
        .build();
  }

  private String getLatestDependencyVersion(
      final String group, final String artifact, final String currentVersion) {

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

    return mavenDoc.getV();
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
