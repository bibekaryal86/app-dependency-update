package app.dependency.update.app.service;

import static app.dependency.update.app.util.CommonUtils.*;
import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.connector.GradleConnector;
import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import app.dependency.update.app.model.AppInitData;
import app.dependency.update.app.model.GradleReleaseResponse;
import app.dependency.update.app.model.Repository;
import app.dependency.update.app.model.ScriptFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AppInitDataService {

  private final GradleConnector gradleConnector;

  public AppInitDataService(final GradleConnector gradleConnector) {
    this.gradleConnector = gradleConnector;
  }

  @Cacheable(value = "appInitData", unless = "#result==null")
  public AppInitData appInitData() {
    log.info("Set App Init Data...");
    boolean isWindows = System.getProperty("os.name").contains("Windows");
    // get the input arguments
    Map<String, String> argsMap = makeArgsMap();
    // get the list of repositories and their type
    List<Repository> repositories = getRepositoryLocations(argsMap);
    // get the scripts included in resources folder
    List<ScriptFile> scriptFiles = getScriptsInResources(isWindows);

    return AppInitData.builder()
        .argsMap(argsMap)
        .repositories(repositories)
        .scriptFiles(scriptFiles)
        .isWindows(isWindows)
        .build();
  }

  @CacheEvict(value = "appInitData", allEntries = true, beforeInvocation = true)
  public void clearAppInitData() {
    log.info("Clear App Init Data...");
  }

  private Map<String, String> makeArgsMap() {
    log.info("Make Args Map...");
    Map<String, String> map = new HashMap<>();
    map.put(PARAM_REPO_HOME, getSystemEnvProperty(PARAM_REPO_HOME, null));
    map.put(ENV_MONGO_USERNAME, getSystemEnvProperty(ENV_MONGO_USERNAME, null));
    map.put(ENV_MONGO_PASSWORD, getSystemEnvProperty(ENV_MONGO_PASSWORD, null));

    log.info("Args Map After Conversion: [ {} ]", map);
    return map;
  }

  private List<Repository> getRepositoryLocations(final Map<String, String> argsMap) {
    log.info("Get Repository Locations...");
    List<Path> repoPaths;
    try (Stream<Path> pathStream = Files.walk(Paths.get(argsMap.get(PARAM_REPO_HOME)), 2)) {
      repoPaths = pathStream.filter(Files::isDirectory).toList();
    } catch (Exception ex) {
      throw new AppDependencyUpdateRuntimeException(
          "Repositories not found in the repo path provided!", ex);
    }

    if (repoPaths.isEmpty()) {
      throw new AppDependencyUpdateRuntimeException(
          "Repositories not found in the repo path provided!");
    }

    List<Repository> npmRepositories = new ArrayList<>();
    List<Repository> gradleRepositories = new ArrayList<>();
    for (Path path : repoPaths) {
      try (Stream<Path> pathStream = Files.list(path)) {
        npmRepositories.addAll(
            pathStream
                .filter(stream -> "package.json".equals(stream.getFileName().toString()))
                .map(mapper -> new Repository(path, UpdateType.NPM_DEPENDENCIES))
                .toList());
      } catch (Exception ex) {
        throw new AppDependencyUpdateRuntimeException(
            "NPM Files not found in the repo path provided!", ex);
      }
      try (Stream<Path> pathStream = Files.list(path)) {
        gradleRepositories.addAll(
            pathStream
                .filter(stream -> "settings.gradle".equals(stream.getFileName().toString()))
                .map(
                    mapper -> {
                      List<String> gradleModules = readGradleModules(mapper);
                      return new Repository(path, UpdateType.GRADLE_DEPENDENCIES, gradleModules);
                    })
                .toList());
      } catch (Exception ex) {
        throw new AppDependencyUpdateRuntimeException(
            "Gradle Repositories not found in the repo path provided!", ex);
      }
    }

    // add gradle wrapper version data
    String latestGradleVersion = getLatestGradleVersion();
    List<Repository> gradleWrapperRepositories =
        gradleRepositories.stream()
            .map(
                repository -> {
                  String currentGradleVersion = getCurrentGradleVersionInRepo(repository);
                  if (isRequiresUpdate(currentGradleVersion, latestGradleVersion)) {
                    return new Repository(
                        repository.getRepoPath(),
                        repository.getType(),
                        repository.getGradleModules(),
                        latestGradleVersion,
                        currentGradleVersion);
                  }
                  return repository;
                })
            .toList();

    List<Repository> repositories = new ArrayList<>();
    repositories.addAll(npmRepositories);
    repositories.addAll(gradleWrapperRepositories);

    log.info("Repository list: [ {} ]", repositories);
    return repositories;
  }

  private List<String> readGradleModules(final Path settingsGradlePath) {
    try {
      List<String> allLines = Files.readAllLines(settingsGradlePath);
      Pattern pattern = Pattern.compile(String.format(GRADLE_BUILD_DEPENDENCIES_REGEX, "'", "'"));

      return allLines.stream()
          .filter(line -> line.contains("include"))
          .map(
              line -> {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                  return matcher.group().replace(":", "");
                }
                return null;
              })
          .filter(Objects::nonNull)
          .toList();
    } catch (IOException ex) {
      log.error("Error in Read Gradle Modules: [ {} ]", settingsGradlePath, ex);
      return Collections.singletonList(APP_MAIN_MODULE);
    }
  }

  private List<ScriptFile> getScriptsInResources(final boolean isWindows) {
    log.info("Get Scripts in Resources...");
    List<ScriptFile> scriptFiles = new ArrayList<>();

    try {
      String extension = isWindows ? ".bat" : ".sh";
      Resource[] resources =
          new PathMatchingResourcePatternResolver().getResources("classpath:scripts/*" + extension);
      for (Resource resource : resources) {
        scriptFiles.add(new ScriptFile(Objects.requireNonNull(resource.getFilename())));
      }
    } catch (Exception ex) {
      throw new AppDependencyUpdateRuntimeException("Error reading script files in resources", ex);
    }

    if (scriptFiles.isEmpty()) {
      throw new AppDependencyUpdateRuntimeException("Script files not found in resources");
    }

    log.info("Script files: [ {} ]", scriptFiles);
    return scriptFiles;
  }

  private String getLatestGradleVersion() {
    List<GradleReleaseResponse> gradleReleaseResponses = gradleConnector.getGradleReleases();
    // get rid of draft and prerelease and sort by name descending
    Optional<GradleReleaseResponse> optionalLatestGradleRelease =
        gradleReleaseResponses.stream()
            .filter(
                gradleReleaseResponse ->
                    !(gradleReleaseResponse.isPrerelease() || gradleReleaseResponse.isDraft()))
            .max(Comparator.comparing(GradleReleaseResponse::getName));

    GradleReleaseResponse latestGradleRelease = optionalLatestGradleRelease.orElse(null);
    log.info("Latest Gradle Release: [ {} ]", optionalLatestGradleRelease);

    if (latestGradleRelease == null) {
      log.error("Latest Gradle Release Null Error...");
      return null;
    }

    return latestGradleRelease.getName();
  }

  private String getCurrentGradleVersionInRepo(final Repository repository) {
    Path wrapperPath =
        Path.of(repository.getRepoPath().toString().concat(GRADLE_WRAPPER_PROPERTIES));
    try {
      List<String> allLines = Files.readAllLines(wrapperPath);
      String distributionUrl =
          allLines.stream()
              .filter(line -> line.startsWith("distributionUrl"))
              .findFirst()
              .orElse(null);

      if (distributionUrl != null) {
        return parseDistributionUrlForGradleVersion(distributionUrl);
      }
    } catch (IOException e) {
      log.error("Error reading gradle-wrapper.properties: [ {} ]", repository);
    }
    return null;
  }

  private String parseDistributionUrlForGradleVersion(final String distributionUrl) {
    // matches text between two hyphens
    // eg: distributionUrl=https\://services.gradle.org/distributions/gradle-8.0-bin.zip
    Pattern pattern = Pattern.compile(GRADLE_WRAPPER_REGEX);
    Matcher matcher = pattern.matcher(distributionUrl);
    if (matcher.find()) {
      return matcher.group();
    } else {
      return null;
    }
  }
}
