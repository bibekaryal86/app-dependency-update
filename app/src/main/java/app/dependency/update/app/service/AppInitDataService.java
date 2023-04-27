package app.dependency.update.app.service;

import static app.dependency.update.app.util.CommonUtils.*;
import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import app.dependency.update.app.model.AppInitData;
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

  @Cacheable(value = "appInitData", unless = "#result==null")
  public AppInitData appInitData() {
    log.info("Set App Init Data...");
    // get the input arguments
    Map<String, String> argsMap = makeArgsMap();
    // get the list of repositories and their type
    List<Repository> repositories = getRepositoryLocations(argsMap);
    // get the scripts included in resources folder
    List<ScriptFile> scriptFiles = getScriptsInResources();

    return AppInitData.builder()
        .argsMap(argsMap)
        .repositories(repositories)
        .scriptFiles(scriptFiles)
        .build();
  }

  @CacheEvict(value = "appInitData", allEntries = true, beforeInvocation = true)
  public void clearAppInitData() {
    log.info("Clear App Init Data...");
  }

  private Map<String, String> makeArgsMap() {
    log.info("Make Args Map...");
    Map<String, String> map = new HashMap<>();

    if (getSystemEnvProperty(PARAM_REPO_HOME, null) == null) {
      throw new AppDependencyUpdateRuntimeException("repo_home env property must be provided");
    } else {
      map.put(PARAM_REPO_HOME, getSystemEnvProperty(PARAM_REPO_HOME, null));
    }
    if (getSystemEnvProperty(ENV_MONGO_USERNAME, null) == null) {
      throw new AppDependencyUpdateRuntimeException("mongo_user env property must be provided");
    } else {
      map.put(ENV_MONGO_USERNAME, getSystemEnvProperty(ENV_MONGO_USERNAME, null));
    }
    if (getSystemEnvProperty(ENV_MONGO_PASSWORD, null) == null) {
      throw new AppDependencyUpdateRuntimeException("mongo_pwd env property must be provided");
    } else {
      map.put(ENV_MONGO_PASSWORD, getSystemEnvProperty(ENV_MONGO_PASSWORD, null));
    }

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

    List<Repository> repositories = new ArrayList<>();
    for (Path path : repoPaths) {
      try (Stream<Path> pathStream = Files.list(path)) {
        repositories.addAll(
            pathStream
                .filter(stream -> "package.json".equals(stream.getFileName().toString()))
                .map(mapper -> new Repository(path, UpdateType.NPM_DEPENDENCIES))
                .toList());
      } catch (Exception ex) {
        throw new AppDependencyUpdateRuntimeException(
            "NPM Files not found in the repo path provided!", ex);
      }
      try (Stream<Path> pathStream = Files.list(path)) {
        repositories.addAll(
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

  private List<ScriptFile> getScriptsInResources() {
    log.info("Get Scripts in Resources...");
    List<ScriptFile> scriptFiles = new ArrayList<>();

    try {
      Resource[] resources =
          new PathMatchingResourcePatternResolver().getResources("classpath:scripts/*.sh");
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
}