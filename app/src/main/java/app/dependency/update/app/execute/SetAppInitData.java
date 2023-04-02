package app.dependency.update.app.execute;

import static app.dependency.update.app.util.CommonUtil.PARAM_REPO_HOME;
import static app.dependency.update.app.util.CommonUtil.SCRIPTS_DIRECTORY;

import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import app.dependency.update.app.model.Repository;
import app.dependency.update.app.model.ScriptFile;
import app.dependency.update.app.util.CommonUtil;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SetAppInitData {
  private final String[] args;

  public SetAppInitData(final String[] args) {
    this.args = args;
  }

  public void setAppInitData() {
    log.info("Set App Init Data...");
    // get the input arguments
    Map<String, String> argsMap = makeArgsMap();
    // get the scripts included in resources folder
    List<ScriptFile> scriptFiles = getScriptsInResources();
    // get the list of repositories and their type
    List<Repository> repositories = getRepositoryLocations(argsMap);
    // set app init data
    CommonUtil.setAppInitData(argsMap, scriptFiles, repositories);
  }

  private Map<String, String> makeArgsMap() {
    log.info("Args Before Conversion: [ {} ]", Arrays.asList(args));
    Map<String, String> map = new HashMap<>();

    if (CommonUtil.getSystemEnvProperty(CommonUtil.PARAM_REPO_HOME, null) == null) {
      throw new AppDependencyUpdateRuntimeException("repo_home env property must be provided");
    } else {
      map.put(
          CommonUtil.PARAM_REPO_HOME,
          CommonUtil.getSystemEnvProperty(CommonUtil.PARAM_REPO_HOME, null));
    }
    if (CommonUtil.getSystemEnvProperty(CommonUtil.ENV_MONGO_USERNAME, null) == null) {
      throw new AppDependencyUpdateRuntimeException("mongo_user env property must be provided");
    } else {
      map.put(
          CommonUtil.ENV_MONGO_USERNAME,
          CommonUtil.getSystemEnvProperty(CommonUtil.ENV_MONGO_USERNAME, null));
    }

    if (CommonUtil.getSystemEnvProperty(CommonUtil.ENV_MONGO_PASSWORD, null) == null) {
      throw new AppDependencyUpdateRuntimeException("mongo_pwd env property must be provided");
    } else {
      map.put(
          CommonUtil.ENV_MONGO_PASSWORD,
          CommonUtil.getSystemEnvProperty(CommonUtil.ENV_MONGO_PASSWORD, null));
    }

    log.info("Args Map After Conversion: [ {} ]", map);
    return map;
  }

  private List<ScriptFile> getScriptsInResources() {
    log.info("Get Scripts in Resources...");
    List<ScriptFile> scriptFiles;

    try {
      // get path of the current running JAR
      String jarPath =
          SetAppInitData.class
              .getProtectionDomain()
              .getCodeSource()
              .getLocation()
              .toURI()
              .getPath();

      // file walks JAR
      URI uri = URI.create("jar:file:" + jarPath);
      try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
        try (Stream<Path> streamPath = Files.walk(fs.getPath(SCRIPTS_DIRECTORY), 1)) {
          scriptFiles =
              streamPath
                  .filter(Files::isRegularFile)
                  .map(path -> new ScriptFile(path.getFileName().toString()))
                  .toList();
        }
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

  private List<Repository> getRepositoryLocations(final Map<String, String> argsMap) {
    log.info("Get Repository Locations...");
    List<Path> repoPaths;
    try (Stream<Path> pathStream = Files.walk(Paths.get(argsMap.get(PARAM_REPO_HOME)), 1)) {
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
                .map(mapper -> new Repository(path, CommonUtil.NPM))
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
                      return new Repository(path, CommonUtil.GRADLE, gradleModules);
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

  private List<String> readGradleModules(Path settingsGradlePath) {
    try {
      List<String> allLines = Files.readAllLines(settingsGradlePath);
      Pattern pattern =
          Pattern.compile(String.format(CommonUtil.GRADLE_BUILD_DEPENDENCIES_REGEX, "'", "'"));

      return allLines.stream()
          .filter(line -> line.contains("include"))
          .map(
              line -> {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                  return matcher.group();
                }
                return null;
              })
          .filter(Objects::nonNull)
          .toList();
    } catch (IOException ex) {
      log.error("Error in Read Gradle Modules: [ {} ]", settingsGradlePath, ex);
      return Collections.singletonList(CommonUtil.APP_MAIN_MODULE);
    }
  }
}
