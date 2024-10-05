package app.dependency.update.app.util;

import static app.dependency.update.app.util.CommonUtils.*;
import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import app.dependency.update.app.model.AppInitData;
import app.dependency.update.app.model.LatestVersionDocker;
import app.dependency.update.app.model.LatestVersionGcp;
import app.dependency.update.app.model.LatestVersionGithubActions;
import app.dependency.update.app.model.LatestVersionRuntimes;
import app.dependency.update.app.model.LatestVersions;
import app.dependency.update.app.model.Repository;
import app.dependency.update.app.model.ScriptFile;
import app.dependency.update.app.service.GradleRepoService;
import app.dependency.update.app.service.JavaService;
import app.dependency.update.app.service.NodeService;
import app.dependency.update.app.service.PythonService;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.StringUtils;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AppInitDataUtils {

  // can't use spring cache in static class, so using local cache
  private static AppInitData appInitDataCache = null;

  public static AppInitData appInitData() {
    if (appInitDataCache == null) {
      appInitDataCache = setAppInitData();
    }
    return appInitDataCache;
  }

  public static AppInitData setAppInitData() {
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

  public static void clearAppInitData() {
    log.info("Clear App Init Data...");
    appInitDataCache = null;
  }

  private static Map<String, String> makeArgsMap() {
    log.debug("Make Args Map...");
    Map<String, String> map = validateInputAndMakeArgsMap();
    log.info("Args Map After Conversion: [ {} ]", map.size());
    return map;
  }

  public static Map<String, String> validateInputAndMakeArgsMap() {
    Map<String, String> map = new HashMap<>();

    if (getSystemEnvProperty(ENV_REPO_NAME) == null) {
      throw new AppDependencyUpdateRuntimeException("repo_home env property must be provided");
    }
    map.put(ENV_REPO_NAME, getSystemEnvProperty(ENV_REPO_NAME));

    if (getSystemEnvProperty(ENV_MONGO_USERNAME) == null) {
      throw new AppDependencyUpdateRuntimeException("mongo_user env property must be provided");
    }
    map.put(ENV_MONGO_USERNAME, getSystemEnvProperty(ENV_MONGO_USERNAME));

    if (getSystemEnvProperty(ENV_MONGO_PASSWORD) == null) {
      throw new AppDependencyUpdateRuntimeException("mongo_pwd env property must be provided");
    }
    map.put(ENV_MONGO_PASSWORD, getSystemEnvProperty(ENV_MONGO_PASSWORD));

    if ("true".equals(getSystemEnvProperty(ENV_SEND_EMAIL))) {
      map.put(ENV_SEND_EMAIL, getSystemEnvProperty(ENV_SEND_EMAIL));

      if (getSystemEnvProperty(ENV_MAILJET_EMAIL_ADDRESS) == null) {
        throw new AppDependencyUpdateRuntimeException("mj_email env property must be provided");
      }
      map.put(ENV_MAILJET_PUBLIC_KEY, getSystemEnvProperty(ENV_MAILJET_PUBLIC_KEY));

      if (getSystemEnvProperty(ENV_MAILJET_PUBLIC_KEY) == null) {
        throw new AppDependencyUpdateRuntimeException("mj_public env property must be provided");
      }
      map.put(ENV_MAILJET_PRIVATE_KEY, getSystemEnvProperty(ENV_MAILJET_PRIVATE_KEY));

      if (getSystemEnvProperty(ENV_MAILJET_PRIVATE_KEY) == null) {
        throw new AppDependencyUpdateRuntimeException("mj_private env property must be provided");
      }
      map.put(ENV_MAILJET_EMAIL_ADDRESS, getSystemEnvProperty(ENV_MAILJET_EMAIL_ADDRESS));
    }

    return map;
  }

  private static List<Repository> getRepositoryLocations(final Map<String, String> argsMap) {
    log.debug("Get Repository Locations...");
    List<Path> repoPaths;
    try (Stream<Path> pathStream = Files.walk(Paths.get(argsMap.get(ENV_REPO_NAME)), 2)) {
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
    List<Repository> pythonRepositories = new ArrayList<>();
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
      try (Stream<Path> pathStream = Files.list(path)) {
        pythonRepositories.addAll(
            pathStream
                .filter(stream -> "pyproject.toml".equals(stream.getFileName().toString()))
                .map(
                    mapper -> {
                      List<String> requirementsTxts = readRequirementsTxts(path);
                      return new Repository(path, UpdateType.PYTHON_DEPENDENCIES, requirementsTxts);
                    })
                .toList());
      } catch (Exception ex) {
        throw new AppDependencyUpdateRuntimeException(
            "Python Files not found in the repo path provided!", ex);
      }
    }

    // add gradle wrapper version data
    List<Repository> gradleWrapperRepositories =
        gradleRepositories.stream()
            .map(
                repository -> {
                  String currentGradleVersion = getCurrentGradleVersionInRepo(repository);
                  return new Repository(
                      repository.getRepoPath(),
                      repository.getType(),
                      repository.getGradleModules(),
                      currentGradleVersion);
                })
            .toList();

    List<Repository> repositories = new ArrayList<>();
    repositories.addAll(npmRepositories);
    repositories.addAll(gradleWrapperRepositories);
    repositories.addAll(pythonRepositories);

    log.info("Repository list: [ {} ]", repositories.size());
    log.debug("Repository list: [ {} ]", repositories);
    return repositories;
  }

  private static List<String> readGradleModules(final Path settingsGradlePath) {
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

  private static List<ScriptFile> getScriptsInResources() {
    log.debug("Get Scripts in Resources...");
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

    log.info("Script files: [ {} ]", scriptFiles.size());
    log.debug("Script files: [ {} ]", scriptFiles);
    return scriptFiles;
  }

  private static String getCurrentGradleVersionInRepo(final Repository repository) {
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

  private static String parseDistributionUrlForGradleVersion(final String distributionUrl) {
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

  private static List<String> readRequirementsTxts(final Path path) {
    try (Stream<Path> pathStream = Files.list(path)) {
      return pathStream
          .filter(
              stream ->
                  stream.getFileName().toString().startsWith("requirements")
                      && stream.getFileName().toString().contains(".txt"))
          .map(stream -> stream.getFileName().toString())
          .toList();
    } catch (Exception ex) {
      throw new AppDependencyUpdateRuntimeException(
          "Requirements Texts Files not found in the repo path provided!", ex);
    }
  }

  private static LatestVersions getLatestVersions() {
    LatestVersionRuntimes latestVersionRuntimes = getLatestVersionRuntimes();
    LatestVersionGcp latestVersionGcp = getLatestVersionGcp();
    LatestVersionDocker latestVersionDocker = getLatestVersionDocker();
    LatestVersionGithubActions latestVersionGithubActions = getLatestVersionGithubActions();
    return LatestVersions.builder()
        .latestVersionRuntimes(latestVersionRuntimes)
        .latestVersionGcp(latestVersionGcp)
        .latestVersionDocker(latestVersionDocker)
        .latestVersionsGithubActions(latestVersionGithubActions)
        .build();
  }

  private static LatestVersionRuntimes getLatestVersionRuntimes() {
    final String java = ApplicationContextUtil.getBean(JavaService.class).getLatestJavaVersion();
    final String node = ApplicationContextUtil.getBean(NodeService.class).getLatestNodeVersion();
    final String python =
        ApplicationContextUtil.getBean(PythonService.class).getLatestPythonVersion();
    final String gradle =
        ApplicationContextUtil.getBean(GradleRepoService.class).getLatestGradleVersion();

    final LatestVersionRuntimes latestVersionRuntimes =
        LatestVersionRuntimes.builder().java(java).node(node).python(python).java(java).build();
    validateLatestVersion(latestVersionRuntimes);
    return latestVersionRuntimes;
  }

  private static LatestVersionGcp getLatestVersionGcp() {
    LatestVersionGcp latestVersion = LatestVersionGcp.builder().build();
    validateLatestVersion(latestVersion);
    return latestVersion;
  }

  private static LatestVersionDocker getLatestVersionDocker() {
    LatestVersionDocker latestVersion = LatestVersionDocker.builder().build();
    validateLatestVersion(latestVersion);
    return latestVersion;
  }

  private static LatestVersionGithubActions getLatestVersionGithubActions() {
    LatestVersionGithubActions latestVersion = LatestVersionGithubActions.builder().build();
    validateLatestVersion(latestVersion);
    return latestVersion;
  }

  private static void validateLatestVersion(final Object latestVersion) {
    Field[] fields = latestVersion.getClass().getDeclaredFields();
    try {
      for (Field field : fields) {
        field.setAccessible(true);
        if (field.getType().equals(String.class)) {
          String value = (String) field.get(latestVersion);
          if (!StringUtils.hasText(value)) {
            throw new AppDependencyUpdateRuntimeException(
                String.format("Field %s doesn't have value", field.getName()));
          }
        }
      }
    } catch (Exception ex) {
      log.error("Validate Latest Version: [{}]", latestVersion, ex);
      throw new AppDependencyUpdateRuntimeException("Latest Version Value Check Exception");
    }
  }
}
