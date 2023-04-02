package app.dependency.update.app.execute;

import app.dependency.update.app.model.BuildGradleConfigs;
import app.dependency.update.app.model.GradleConfigBlock;
import app.dependency.update.app.model.GradleDefinition;
import app.dependency.update.app.model.GradleDependency;
import app.dependency.update.app.model.Repository;
import app.dependency.update.app.util.CommonUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReadBuildGradle {

  private final Repository repository;
  private final String gradleModule;

  public ReadBuildGradle(final Repository repository, final String gradleModule) {
    this.repository = repository;
    this.gradleModule = gradleModule;
  }

  public BuildGradleConfigs readBuildGradle() {
    Path buildGradlePath =
        Path.of(
            this.repository
                .getRepoPath()
                .toString()
                .concat(CommonUtil.PATH_DELIMITER)
                .concat(this.gradleModule)
                .concat(CommonUtil.PATH_DELIMITER)
                .concat(CommonUtil.BUILD_GRADLE));

    try {
      List<String> allLines = Files.readAllLines(buildGradlePath);
      GradleConfigBlock plugins = getPluginsBlock(allLines);
      GradleConfigBlock dependencies = getDependenciesBlock(allLines);
      return BuildGradleConfigs.builder()
          .buildGradlePath(buildGradlePath)
          .originals(allLines)
          .plugins(plugins)
          .dependencies(dependencies)
          .build();
    } catch (IOException e) {
      log.error(
          "Error reading build.gradle: [ {} ] [ {} ]",
          this.repository.getRepoName(),
          this.gradleModule);
    }
    return null;
  }

  // suppressing sonarlint rule to not use more than break or continue statement
  // suppressing sonarlint rule for cognitive complexity of method too high
  @SuppressWarnings({"java:S135", "java:S3776"})
  private GradleConfigBlock getPluginsBlock(final List<String> allLines) {
    List<GradleDependency> plugins = new ArrayList<>();
    int pluginsBeginPosition = allLines.indexOf("plugins {");

    if (pluginsBeginPosition >= 0) {
      for (int i = pluginsBeginPosition + 1; i < allLines.size(); i++) {
        final String plugin = allLines.get(i);
        // check if this is the end of the block
        if (plugin.equals("}") && isEndOfABlock(allLines, i + 1)) {
          break;
        }
        // ignore comments, new lines and plugins that don't have version
        if (CommonUtil.leftTrim(plugin).startsWith("//") || !plugin.contains("version")) {
          continue;
        }
        // Example:    id 'io.freefair.lombok' version '6.6.3'
        String[] pluginArray = plugin.trim().split(" ");
        if (pluginArray.length != 4) {
          continue;
        }
        String group = pluginArray[1].replace("'", "");
        String version = pluginArray[3].replace("'", "");
        String artifact = CommonUtil.getPluginsMap().get(group);

        if (CommonUtil.isEmpty(artifact)) {
          // It is likely plugin information is not available in the local repository
          // Do not throw error, log the event and continue updating others
          log.error("Plugin information missing in local repo: [ {} ]", group);
          continue;
        }

        plugins.add(
            GradleDependency.builder()
                .original(plugin)
                .group(group)
                .artifact(artifact)
                .version(version)
                .build());
      }
    } else {
      log.info("No plugins in the project...");
    }

    return GradleConfigBlock.builder().dependencies(plugins).build();
  }

  // suppressing sonarlint rule for cognitive complexity of method too high
  @SuppressWarnings("java:S3776")
  private GradleConfigBlock getDependenciesBlock(final List<String> allLines) {
    List<GradleDefinition> gradleDefinitions = new ArrayList<>();
    List<GradleDependency> gradleDependencies = new ArrayList<>();

    int dependenciesBeginPosition = allLines.indexOf("dependencies {");

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
        if (isDependencyDeclaration(CommonUtil.leftTrim(dependency))) {
          if (dependency.contains("(") && dependency.contains(")")) {
            dependency = dependency.replace("(", " ").replace(")", " ");
          }

          GradleDependency gradleDependency = getGradleDependency(dependency, original);
          if (gradleDependency != null) {
            gradleDependencies.add(gradleDependency);
          }
        } else if (isDefinitionDeclaration(CommonUtil.leftTrim(dependency))) {
          GradleDefinition gradleDefinition = getGradleDefinition(dependency);
          if (gradleDefinition != null) {
            gradleDefinitions.add(gradleDefinition);
          }
        }
      }
    } else {
      log.info("No dependencies in the project...");
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
            "testRuntimeOnly");
    return dependencyConfigurations.stream().anyMatch(dependency::startsWith);
  }

  private boolean isDefinitionDeclaration(final String dependency) {
    return dependency.startsWith("def");
  }

  private GradleDependency getGradleDependency(final String dependency, final String original) {
    // Get between `'` or `"`
    Pattern pattern;
    if (dependency.contains("'") && !dependency.contains("\"")) {
      pattern =
          Pattern.compile(String.format(CommonUtil.GRADLE_BUILD_DEPENDENCIES_REGEX, "'", "'"));
    } else if (!dependency.contains("'") && dependency.contains("\"")) {
      pattern =
          Pattern.compile(String.format(CommonUtil.GRADLE_BUILD_DEPENDENCIES_REGEX, "\"", "\""));
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
    Pattern pattern =
        Pattern.compile(String.format(CommonUtil.GRADLE_BUILD_DEPENDENCIES_REGEX, "\"", "\""));
    Matcher matcher = pattern.matcher(dependency);

    if (matcher.find()) {
      String value = matcher.group();
      pattern = Pattern.compile(CommonUtil.GRADLE_BUILD_DEFINITION_REGEX);
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

  private boolean isEndOfABlock(final List<String> allLines, final int positionPlusOne) {
    // assumption: only one empty line between blocks and at the end of file

    // check 1: if this is the end of file and nothing exists after
    if (isDoesNotExist(allLines, positionPlusOne)) {
      return true;
    }
    // check 2: if this is the end of file and only next line is empty line
    if (allLines.get(positionPlusOne).trim().equals("")
        && isDoesNotExist(allLines, positionPlusOne + 1)) {
      return true;
    }
    // check 3: check against beginning of another block (Eg: repositories {)
    Pattern pattern = Pattern.compile(CommonUtil.GRADLE_BUILD_BLOCK_END_REGEX);
    Matcher matcher;
    if (allLines.get(positionPlusOne).trim().equals("")) {
      matcher = pattern.matcher(allLines.get(positionPlusOne + 1));
    } else {
      // though assumed, still check if there is no empty lines between blocks
      matcher = pattern.matcher(allLines.get(positionPlusOne));
    }
    return matcher.find();
  }

  private boolean isDoesNotExist(final List<String> allLines, final int positionPlusOne) {
    try {
      allLines.get(positionPlusOne);
      return false;
    } catch (IndexOutOfBoundsException ignored) {
      return true;
    }
  }
}
