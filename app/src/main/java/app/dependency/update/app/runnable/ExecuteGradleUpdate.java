package app.dependency.update.app.runnable;

import static app.dependency.update.app.util.CommonUtils.*;
import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.model.BuildGradleConfigs;
import app.dependency.update.app.model.GradleConfigBlock;
import app.dependency.update.app.model.GradleDefinition;
import app.dependency.update.app.model.GradleDependency;
import app.dependency.update.app.model.GradlePlugin;
import app.dependency.update.app.model.Repository;
import app.dependency.update.app.model.ScriptFile;
import app.dependency.update.app.model.entities.Dependencies;
import app.dependency.update.app.model.entities.Plugins;
import app.dependency.update.app.service.MongoRepoService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExecuteGradleUpdate implements Runnable {
  private final String threadName;
  private final String latestGradleVersion;
  private final String latestJavaVersionMajor;
  private final Repository repository;
  private final ScriptFile scriptFile;
  private final List<String> arguments;
  private final Map<String, Plugins> pluginsMap;
  private final Map<String, Dependencies> dependenciesMap;
  private final MongoRepoService mongoRepoService;
  private Thread thread;
  private boolean isExecuteScriptRequired = false;

  public ExecuteGradleUpdate(
      final String latestGradleVersion,
      final String latestJavaVersionMajor,
      final Repository repository,
      final ScriptFile scriptFile,
      final List<String> arguments,
      final MongoRepoService mongoRepoService) {
    this.threadName = threadName(repository, this.getClass().getSimpleName());
    this.latestGradleVersion = latestGradleVersion;
    this.latestJavaVersionMajor = latestJavaVersionMajor;
    this.repository = repository;
    this.scriptFile = scriptFile;
    this.arguments = arguments;
    this.pluginsMap = mongoRepoService.pluginsMap();
    this.dependenciesMap = mongoRepoService.dependenciesMap();
    this.mongoRepoService = mongoRepoService;
  }

  @Override
  public void run() {
    executeGradleUpdate();
  }

  public Thread start() {
    if (this.thread == null) {
      this.thread = new Thread(this, this.threadName);
      this.thread.start();
    }
    return this.thread;
  }

  private void executeGradleUpdate() {
    executeBuildGradleUpdate();
    executeGradleWrapperUpdate();

    if (this.isExecuteScriptRequired) {
      Thread executeThread =
          new ExecuteScriptFile(
                  threadName(repository, "-" + this.getClass().getSimpleName()),
                  // simple name used in thread name for current class already, so use "-"
                  this.scriptFile,
                  this.arguments)
              .start();
      join(executeThread);
    }
  }

  private boolean writeToFile(final Path path, final List<String> content) {
    try {
      Files.write(path, content, StandardCharsets.UTF_8);
      return true;
    } catch (IOException ex) {
      log.error("Error Saving Updated File: [ {} ]", path, ex);
      return false;
    }
  }

  /*
   * BUILD.GRADLE UPDATE
   */

  // suppressing sonarlint rule for cognitive complexity of method too high
  @SuppressWarnings("java:S3776")
  private void executeBuildGradleUpdate() {
    try {
      List<String> gradleModules = this.repository.getGradleModules();
      for (String gradleModule : gradleModules) {
        log.debug(
            "Update Gradle Build File for Module: [ {} ] [ {} ]",
            this.repository.getRepoName(),
            gradleModule);
        BuildGradleConfigs buildGradleConfigs = readBuildGradle(gradleModule);
        if (buildGradleConfigs == null) {
          log.error("Build Gradle Configs is null: [ {} ]", this.repository.getRepoPath());
        } else {
          List<String> buildGradleContent = modifyBuildGradle(buildGradleConfigs);

          if (isEmpty(buildGradleContent)) {
            log.debug("Build Gradle Configs not updated: [ {} ]", this.repository.getRepoPath());
          } else {
            boolean isWriteToFile =
                writeBuildGradleToFile(buildGradleConfigs.getBuildGradlePath(), buildGradleContent);

            if (isWriteToFile) {
              this.isExecuteScriptRequired = true;
            } else {
              log.debug(
                  "Build Gradle Changes Not Written to File: [ {} ]",
                  this.repository.getRepoPath());
            }
          }
        }
      }
    } catch (Exception ex) {
      log.error("Error in Execute Build Gradle Update: ", ex);
    }
  }

  private boolean writeBuildGradleToFile(
      final Path buildGradlePath, final List<String> buildGradleContent) {
    log.debug("Writing to build.gradle file: [ {} ]", buildGradlePath);
    return writeToFile(buildGradlePath, buildGradleContent);
  }

  private BuildGradleConfigs readBuildGradle(final String gradleModule) {
    Path buildGradlePath =
        Path.of(
            this.repository
                .getRepoPath()
                .toString()
                .concat(PATH_DELIMITER)
                .concat(gradleModule)
                .concat(PATH_DELIMITER)
                .concat(BUILD_GRADLE));

    try {
      List<String> allLines = Files.readAllLines(buildGradlePath);
      GradleConfigBlock plugins = getPluginsBlock(allLines);
      GradleConfigBlock dependencies = getDependenciesBlock(allLines, -1);

      // there might be dependencies block inside buildscript block
      GradleConfigBlock dependenciesBuildScript;
      int dependenciesInBuildscriptBlock = getDependenciesBlockBuildscriptBeginPosition(allLines);
      if (dependenciesInBuildscriptBlock > 0) {
        dependenciesBuildScript = getDependenciesBlock(allLines, dependenciesInBuildscriptBlock);
      } else {
        dependenciesBuildScript =
            GradleConfigBlock.builder()
                .dependencies(new ArrayList<>())
                .dependencies(new ArrayList<>())
                .build();
      }

      return BuildGradleConfigs.builder()
          .buildGradlePath(buildGradlePath)
          .originals(allLines)
          .plugins(plugins)
          .dependencies(List.of(dependencies, dependenciesBuildScript))
          .build();
    } catch (IOException e) {
      log.error(
          "Error reading build.gradle: [ {} ] [ {} ]", this.repository.getRepoName(), gradleModule);
    }
    return null;
  }

  // suppressing sonarlint rule to not use more than break or continue statement
  // suppressing sonarlint rule for cognitive complexity of method too high
  @SuppressWarnings({"java:S135", "java:S3776"})
  private GradleConfigBlock getPluginsBlock(final List<String> allLines) {
    List<GradlePlugin> plugins = new ArrayList<>();
    int pluginsBeginPosition = allLines.indexOf("plugins {");

    if (pluginsBeginPosition >= 0) {
      for (int i = pluginsBeginPosition + 1; i < allLines.size(); i++) {
        final String plugin = allLines.get(i);
        // check if this is the end of the block
        if (plugin.equals("}") && isEndOfABlock(allLines, i + 1)) {
          break;
        }
        // ignore comments, new lines and plugins that don't have version
        if (leftTrim(plugin).startsWith("//") || !plugin.contains("version")) {
          continue;
        }
        // Example:    id 'io.freefair.lombok' version '6.6.3'
        String[] pluginArray = plugin.trim().split(" ");
        if (pluginArray.length != 4) {
          continue;
        }
        String group = "";
        String version = "";

        if (pluginArray[1].contains("'")) {
          group = pluginArray[1].replace("'", "");
        } else {
          group = pluginArray[1].replace("\"", "");
        }
        if (pluginArray[3].contains("'")) {
          version = pluginArray[3].replace("'", "");
        } else {
          version = pluginArray[3].replace("\"", "");
        }
        plugins.add(GradlePlugin.builder().original(plugin).group(group).version(version).build());
      }
    } else {
      log.debug("No plugins in the project...");
    }

    return GradleConfigBlock.builder().plugins(plugins).build();
  }

  private int getDependenciesBlockBuildscriptBeginPosition(final List<String> allLines) {
    int buildscriptBeginPosition = allLines.indexOf("buildscript {");
    int dependenciesBeginPosition = -1;

    if (buildscriptBeginPosition >= 0) {
      for (int i = buildscriptBeginPosition + 1; i < allLines.size(); i++) {
        String buildscript = allLines.get(i);
        // check if this is the end of the block
        if (buildscript.equals("}") && isEndOfABlock(allLines, i + 1)) {
          break;
        }
        if (buildscript.contains("dependencies {")) {
          dependenciesBeginPosition = i;
          break;
        }
      }
    }
    return dependenciesBeginPosition;
  }

  // suppressing sonarlint rule for cognitive complexity of method too high
  @SuppressWarnings("java:S3776")
  private GradleConfigBlock getDependenciesBlock(
      final List<String> allLines, final int buildscriptDependenciesBeginPosition) {
    List<GradleDefinition> gradleDefinitions = new ArrayList<>();
    List<GradleDependency> gradleDependencies = new ArrayList<>();

    int dependenciesBeginPosition;
    if (buildscriptDependenciesBeginPosition > 0) {
      dependenciesBeginPosition = buildscriptDependenciesBeginPosition;
    } else {
      dependenciesBeginPosition = allLines.indexOf("dependencies {");
    }

    if (dependenciesBeginPosition >= 0) {
      for (int i = dependenciesBeginPosition + 1; i < allLines.size(); i++) {
        final String original = allLines.get(i);
        // There is a chance `dependency` could be modified, so keep `original` untouched
        String dependency = allLines.get(i);
        // check if this is the end of the block
        if (dependency.equals("}") && isEndOfABlock(allLines, i + 1)) {
          break;
        }
        // Examples from mvnrepository - Gradle: #1, Gradle (Short): #2, Gradle (Kotlin): #3
        // 1: implementation group: 'ch.qos.logback', name: 'logback-classic', version: '1.4.5'
        // 2: implementation 'com.google.code.gson:gson:2.10.1'
        // 3: implementation ('com.google.code.gson:gson:2.10.1')
        // 4: testImplementation('org.springframework.boot:spring-boot-starter-test:2.3.0.RELEASE')
        // 5: implementation('org.slf4j:slf4j-api') version set as strict or require or other
        // 6: classpath 'org.postgresql:postgresql:42.1.3' (this is in buildscript block)
        if (isDependencyDeclaration(leftTrim(dependency))) {
          if (dependency.contains("(") && dependency.contains(")")) {
            dependency = dependency.replace("(", " ").replace(")", " ");
          }

          GradleDependency gradleDependency = getGradleDependency(dependency, original);
          if (gradleDependency != null) {
            gradleDependencies.add(gradleDependency);
          }
        } else if (isDefinitionDeclaration(leftTrim(dependency))) {
          GradleDefinition gradleDefinition = getGradleDefinition(dependency);
          if (gradleDefinition != null) {
            gradleDefinitions.add(gradleDefinition);
          }
        }
      }
    } else {
      log.debug(
          "No [buildscriptDependenciesBeginPosition={}] dependencies in the project...",
          buildscriptDependenciesBeginPosition);
    }

    return GradleConfigBlock.builder()
        .definitions(gradleDefinitions)
        .dependencies(gradleDependencies)
        .build();
  }

  private boolean isDependencyDeclaration(final String dependency) {
    List<String> dependencyConfigurations =
        Arrays.asList(
            "api",
            "compileOnlyApi",
            "implementation",
            "testImplementation",
            "compileOnly",
            "testCompileOnly",
            "runtimeOnly",
            "testRuntimeOnly",
            "classpath");
    return dependencyConfigurations.stream().anyMatch(dependency::startsWith);
  }

  private boolean isDefinitionDeclaration(final String dependency) {
    return dependency.startsWith("def");
  }

  private GradleDependency getGradleDependency(final String dependency, final String original) {
    // Get between `'` or `"`
    Pattern pattern;
    if (dependency.contains("'") && !dependency.contains("\"")) {
      pattern = Pattern.compile(String.format(GRADLE_BUILD_DEPENDENCIES_REGEX, "'", "'"));
    } else if (!dependency.contains("'") && dependency.contains("\"")) {
      pattern = Pattern.compile(String.format(GRADLE_BUILD_DEPENDENCIES_REGEX, "\"", "\""));
    } else {
      return null;
    }

    Matcher matcher = pattern.matcher(dependency);
    if (matcher.find()) {
      String group = matcher.group();
      if (group.contains(":")) {
        // From above examples this matches - #2, #3 and #4
        return getGradleDependency(group, null, original);
      } else {
        // From examples this matches - #1
        Stream<MatchResult> matcherResultStream = matcher.results();
        List<String> artifactVersion =
            matcherResultStream
                .map(
                    matchResult -> {
                      if (!matchResult.group().contains(",")) {
                        return matchResult.group().trim();
                      }
                      return null;
                    })
                .filter(Objects::nonNull)
                .toList();
        return getGradleDependency(group, artifactVersion, original);
      }
    }

    return null;
  }

  private GradleDependency getGradleDependency(
      final String group, final List<String> artifactVersion, final String original) {
    String[] dependencyArray;
    if (artifactVersion == null) {
      dependencyArray = group.split(":");
    } else {
      dependencyArray = (group + ":" + String.join(":", artifactVersion)).split(":");
    }

    if (dependencyArray.length == 3) {
      return GradleDependency.builder()
          .original(original)
          .group(dependencyArray[0])
          .artifact(dependencyArray[1])
          .version(dependencyArray[2])
          .build();
    }

    return null;
  }

  private GradleDefinition getGradleDefinition(final String dependency) {
    Pattern pattern = Pattern.compile(String.format(GRADLE_BUILD_DEPENDENCIES_REGEX, "\"", "\""));
    Matcher matcher = pattern.matcher(dependency);

    if (matcher.find()) {
      String value = matcher.group();
      pattern = Pattern.compile(GRADLE_BUILD_DEFINITION_REGEX);
      matcher = pattern.matcher(dependency);

      if (matcher.find()) {
        String defName = matcher.group();
        String[] defNameArray = defName.split(" ");
        if (defNameArray.length == 2) {
          return GradleDefinition.builder()
              .original(dependency)
              .name(defNameArray[1])
              .value(value)
              .build();
        }
      }
    }
    return null;
  }

  private List<String> getJavaBlock(final List<String> allLines) {
    List<String> javaLines = new ArrayList<>();
    int javaBeginPosition = allLines.indexOf("java {");

    if (javaBeginPosition >= 0) {
      for (int i = javaBeginPosition + 1; i < allLines.size(); i++) {
        String javaLine = allLines.get(i);
        // check if this is the end of the block
        if (javaLine.equals("}") && isEndOfABlock(allLines, i + 1)) {
          break;
        }
        javaLines.add(javaLine);
      }
    }
    return javaLines;
  }

  private boolean isEndOfABlock(final List<String> allLines, final int positionPlusOne) {
    // assumption: only one empty line between blocks and at the end of file

    // check 1: if this is the end of file and nothing exists after
    if (isDoesNotExist(allLines, positionPlusOne)) {
      return true;
    }
    // check 2: if this is the end of file and only next line is empty line
    if (allLines.get(positionPlusOne).trim().isEmpty()
        && isDoesNotExist(allLines, positionPlusOne + 1)) {
      return true;
    }
    // check 3: check against end of super block (Eg: buildscript { dependencies {} })
    if (allLines.get(positionPlusOne).trim().equals("}")) {
      return true;
    }
    // check 4: check against beginning of another block (Eg: repositories {)
    Pattern pattern = Pattern.compile(GRADLE_BUILD_BLOCK_END_REGEX);
    Matcher matcher;
    if (allLines.get(positionPlusOne).trim().isEmpty()) {
      matcher = pattern.matcher(allLines.get(positionPlusOne + 1));
    } else {
      // though assumed, still check if there is no empty lines between blocks
      matcher = pattern.matcher(allLines.get(positionPlusOne));
    }
    return matcher.find();
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  private boolean isDoesNotExist(final List<String> allLines, final int positionPlusOne) {
    try {
      allLines.get(positionPlusOne);
      return false;
    } catch (IndexOutOfBoundsException ignored) {
      return true;
    }
  }

  private List<String> modifyBuildGradle(final BuildGradleConfigs buildGradleConfigs) {
    final List<String> originals = new ArrayList<>(buildGradleConfigs.getOriginals());

    final GradleConfigBlock pluginsBlock = buildGradleConfigs.getPlugins();
    modifyPluginsBlock(pluginsBlock, originals);

    final List<GradleConfigBlock> dependenciesBlock = buildGradleConfigs.getDependencies();
    for (GradleConfigBlock dependencyBlock : dependenciesBlock) {
      modifyDependenciesBlock(dependencyBlock, originals);
    }

    modifyJavaBlock(originals);

    if (originals.equals(buildGradleConfigs.getOriginals())) {
      return Collections.emptyList();
    } else {
      return originals;
    }
  }

  // suppressing sonarlint rule for cognitive complexity of method too high
  @SuppressWarnings("java:S3776")
  private void modifyPluginsBlock(
      final GradleConfigBlock pluginsBlock, final List<String> originals) {
    List<String> updatedPlugins = new ArrayList<>();
    if (pluginsBlock != null && !pluginsBlock.getPlugins().isEmpty()) {
      for (final GradlePlugin gradlePlugin : pluginsBlock.getPlugins()) {
        if (gradlePlugin.getVersion().startsWith("$")) {
          // this updates definition
          String definitionName = gradlePlugin.getVersion().replace("$", "");

          if (updatedPlugins.contains(definitionName)) {
            continue;
          }

          Optional<GradleDefinition> gradleDefinitionOptional =
              pluginsBlock.getDefinitions().stream()
                  .filter(gradleDefinition -> gradleDefinition.getName().equals(definitionName))
                  .findFirst();
          if (gradleDefinitionOptional.isPresent()) {
            GradleDefinition gradleDefinition = gradleDefinitionOptional.get();
            String version = gradleDefinition.getValue();
            GradlePlugin modifiedGradlePlugin =
                GradlePlugin.builder().group(gradlePlugin.getGroup()).version(version).build();

            String updatedOriginal = modifyPlugin(modifiedGradlePlugin, gradleDefinition);

            if (updatedOriginal != null) {
              int index = originals.indexOf(gradleDefinition.getOriginal());
              if (index >= 0) {
                originals.set(index, updatedOriginal);
                updatedPlugins.add(definitionName);
              }
            }
          }
        } else {
          String updatedOriginal = modifyPlugin(gradlePlugin, null);
          if (updatedOriginal != null) {
            int index = originals.indexOf(gradlePlugin.getOriginal());
            if (index >= 0) {
              originals.set(index, updatedOriginal);
            }
          }
        }
      }
    }
  }

  // suppressing sonarlint rule for cognitive complexity of method too high
  @SuppressWarnings("java:S3776")
  private void modifyDependenciesBlock(
      final GradleConfigBlock dependenciesBlock, final List<String> originals) {
    List<String> updatedDefinitions = new ArrayList<>();
    if (dependenciesBlock != null && !dependenciesBlock.getDependencies().isEmpty()) {
      for (final GradleDependency gradleDependency : dependenciesBlock.getDependencies()) {
        if (gradleDependency.getVersion().startsWith("$")) {
          // this updates definition
          String definitionName = gradleDependency.getVersion().replace("$", "");

          if (updatedDefinitions.contains(definitionName)) {
            continue;
          }

          Optional<GradleDefinition> gradleDefinitionOptional =
              dependenciesBlock.getDefinitions().stream()
                  .filter(gradleDefinition -> gradleDefinition.getName().equals(definitionName))
                  .findFirst();
          if (gradleDefinitionOptional.isPresent()) {
            GradleDefinition gradleDefinition = gradleDefinitionOptional.get();
            String version = gradleDefinition.getValue();
            GradleDependency modifiedGradleDependency =
                GradleDependency.builder()
                    .group(gradleDependency.getGroup())
                    .artifact(gradleDependency.getArtifact())
                    .version(version)
                    .build();

            String updatedOriginal = modifyDependency(modifiedGradleDependency, gradleDefinition);

            if (updatedOriginal != null) {
              int index = originals.indexOf(gradleDefinition.getOriginal());
              if (index >= 0) {
                originals.set(index, updatedOriginal);
                updatedDefinitions.add(definitionName);
              }
            }
          }
        } else {
          String updatedOriginal = modifyDependency(gradleDependency, null);
          if (updatedOriginal != null) {
            int index = originals.indexOf(gradleDependency.getOriginal());
            if (index >= 0) {
              originals.set(index, updatedOriginal);
            }
          }
        }
      }
    }
  }

  private String modifyPlugin(
      final GradlePlugin gradlePlugin, final GradleDefinition gradleDefinition) {
    String group = gradlePlugin.getGroup();
    Plugins plugin = this.pluginsMap.get(group);

    if (plugin == null) {
      // It is likely plugin information is not available in the local repository
      log.info("Plugin information missing in local repo: [ {} ]", group);
      // Save to mongo repository
      mongoRepoService.savePlugin(group, gradlePlugin.getVersion());
    }

    String latestVersion = "";
    if (plugin != null && !plugin.isSkipVersion()) {
      latestVersion = plugin.getVersion();
    }
    if (isRequiresUpdate(gradlePlugin.getVersion(), latestVersion)) {
      return gradleDefinition == null
          ? gradlePlugin.getOriginal().replace(gradlePlugin.getVersion(), latestVersion)
          : gradleDefinition.getOriginal().replace(gradleDefinition.getValue(), latestVersion);
    }

    return null;
  }

  private String modifyDependency(
      final GradleDependency gradleDependency, final GradleDefinition gradleDefinition) {
    String mavenId = gradleDependency.getGroup() + ":" + gradleDependency.getArtifact();
    Dependencies dependency = this.dependenciesMap.get(mavenId);

    if (dependency == null) {
      // It is likely dependency information is not available in the local repository
      log.info("Dependency information missing in local repo: [ {} ]", mavenId);
      // Save to mongo repository
      mongoRepoService.saveDependency(mavenId, gradleDependency.getVersion());
    }

    String latestVersion = "";
    if (dependency != null && !dependency.isSkipVersion()) {
      latestVersion = dependency.getLatestVersion();
    }
    if (isRequiresUpdate(gradleDependency.getVersion(), latestVersion)) {
      return gradleDefinition == null
          ? gradleDependency.getOriginal().replace(gradleDependency.getVersion(), latestVersion)
          : gradleDefinition.getOriginal().replace(gradleDefinition.getValue(), latestVersion);
    }

    return null;
  }

  private void modifyJavaBlock(final List<String> originals) {
    final String currentJavaVersionMajor = extractOldVersion(originals);

    if (currentJavaVersionMajor.equals(this.latestJavaVersionMajor)) {
      return;
    } else if (parseIntSafe(currentJavaVersionMajor) >= parseIntSafe(this.latestJavaVersionMajor)) {
      return;
    }

    String oldVersionString = "JavaVersion.VERSION_" + currentJavaVersionMajor;
    String newVersionString = "JavaVersion.VERSION_" + this.latestJavaVersionMajor;
    String oldOfString = "JavaLanguageVersion.of(" + currentJavaVersionMajor + ")";
    String newOfString = "JavaLanguageVersion.of(" + this.latestJavaVersionMajor + ")";

    for (int i = 0; i < originals.size(); i++) {
      String line = originals.get(i);
      if (line.contains(oldVersionString)) {
        line = line.replace(oldVersionString, newVersionString);
        originals.set(i, line);
      } else if (line.contains(oldOfString)) {
        line = line.replace(oldOfString, newOfString);
        originals.set(i, line);
      }
    }
  }

  private String extractOldVersion(final List<String> javaLines) {
    for (String line : javaLines) {
      // Match "VERSION_X"
      Matcher versionMatcher = Pattern.compile(GRADLE_JAVA_VERSION_REGEX_1).matcher(line);
      if (versionMatcher.find()) {
        return versionMatcher.group(1);
      }

      // Match "of(X)"
      Matcher ofMatcher = Pattern.compile(GRADLE_JAVA_VERSION_REGEX_2).matcher(line);
      if (ofMatcher.find()) {
        return ofMatcher.group(1);
      }
    }

    return this.latestJavaVersionMajor;
  }

  /*
   * GRADLE WRAPPER UPDATE
   */

  private void executeGradleWrapperUpdate() {
    // this check is done when repository object is created
    // adding here as backup
    if (!isRequiresUpdate(this.repository.getCurrentGradleVersion(), this.latestGradleVersion)) {
      return;
    }

    Path wrapperPath =
        Path.of(repository.getRepoPath().toString().concat(GRADLE_WRAPPER_PROPERTIES));
    List<String> gradleWrapperContent = updateGradleWrapperProperties(wrapperPath);

    boolean isWriteToFile = writeGradleWrapperPropertiesToFile(wrapperPath, gradleWrapperContent);
    if (isWriteToFile) {
      this.isExecuteScriptRequired = true;
    } else {
      log.debug(
          "Gradle Wrapper Properties Changes Not Written to File: [ {} ]",
          this.repository.getRepoPath());
    }
  }

  private List<String> updateGradleWrapperProperties(final Path wrapperPath) {
    try {
      List<String> updatedWrapperProperties = new ArrayList<>();
      List<String> wrapperProperties = Files.readAllLines(wrapperPath);

      for (String wrapperProperty : wrapperProperties) {
        if (wrapperProperty.startsWith("distributionUrl")) {
          String updatedDistributionUrl =
              updateDistributionUrl(
                  wrapperProperty,
                  this.repository.getCurrentGradleVersion(),
                  this.latestGradleVersion);
          updatedWrapperProperties.add(updatedDistributionUrl);
        } else {
          updatedWrapperProperties.add(wrapperProperty);
        }
      }

      return updatedWrapperProperties;
    } catch (IOException e) {
      log.error("Error reading gradle-wrapper.properties: [ {} ]", repository);
    }
    return Collections.emptyList();
  }

  private String updateDistributionUrl(
      final String distributionUrl,
      final String currentGradleVersion,
      final String latestGradleVersion) {
    return distributionUrl.replace(currentGradleVersion, latestGradleVersion);
  }

  private boolean writeGradleWrapperPropertiesToFile(
      final Path gradleWrapperPropertiesPath, final List<String> gradleWrapperPropertiesContent) {
    log.debug("Writing to gradle-wrapper.properties file: [ {} ]", gradleWrapperPropertiesPath);
    return writeToFile(gradleWrapperPropertiesPath, gradleWrapperPropertiesContent);
  }

  // suppressing sonarlint rule for interrupting thread
  @SuppressWarnings("java:S2142")
  private void join(final Thread thread) {
    try {
      thread.join();
    } catch (InterruptedException ex) {
      log.error("Exception Join Thread", ex);
    }
  }
}
