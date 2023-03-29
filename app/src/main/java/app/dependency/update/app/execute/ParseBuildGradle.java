package app.dependency.update.app.execute;

import app.dependency.update.app.model.MavenDoc;
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
public class ParseBuildGradle {

  private final List<Repository> repositories;
  private final Map<String, String> argsMap;

  public ParseBuildGradle(final List<Repository> repositories, final Map<String, String> argsMap) {
    this.repositories = repositories;
    this.argsMap = argsMap;
  }

  private void setTempPluginsMap() {
    Map<String, String> map = new HashMap<>();
    map.put("io.freefair.lombok", "io.freefair.lombok.gradle.plugin");
    map.put("com.diffplug.spotless", "spotless-plugin-gradle");
    map.put("org.springframework.boot", "spring-boot-gradle-plugin");
    CommonUtil.setPluginsMap(map);
  }

  public void readBuildGradle(final Repository repository) {
    setTempPluginsMap();
    log.debug("{},{}", this.repositories, this.argsMap);
    Path buildGradlePath = Path.of("C:\\dev\\prj\\app-dependency-update\\app\\build.gradle_temp");
    // Path tmp = Path.of("C:\\dev\\prj\\app-dependency-update\\app\\build.gradle_Temp");
    try {
      List<String> allLines = Files.readAllLines(buildGradlePath);
      Map<String, MavenDoc> pluginsMap = getPluginsBlock(allLines);
      log.info("Plugins Map: [{}]", pluginsMap);
      Map<String, MavenDoc> dependenciesMap = getDependenciesBlock(allLines);
      log.info("Dependencies Map: [{}]", dependenciesMap);
      // Files.write(tmp, allLines, java.nio.charset.StandardCharsets.UTF_8);
    } catch (IOException e) {
      log.error("Error reading build.gradle: {}", repository);
    }
  }

  private Map<String, MavenDoc> getPluginsBlock(final List<String> allLines) {
    Map<String, MavenDoc> pluginsMap = new HashMap<>();
    int pluginsBeginPosition = allLines.indexOf("plugins {");

    if (pluginsBeginPosition >= 0) {
      for (int i = pluginsBeginPosition + 1; i < allLines.size(); i++) {
        String plugin = allLines.get(i);
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
        String group = pluginArray[1].replaceAll("'", "");
        String version = pluginArray[3].replaceAll("'", "");
        String artifact = CommonUtil.getPluginsMap().get(group);
        pluginsMap.put(plugin, MavenDoc.builder().g(group).a(artifact).v(version).build());
      }
    } else {
      log.info("No plugins in the project...");
    }

    return pluginsMap;
  }

  private Map<String, MavenDoc> getDependenciesBlock(final List<String> allLines) {
    Map<String, MavenDoc> dependenciesMap = new HashMap<>();
    int dependenciesBeginPosition = allLines.indexOf("dependencies {");

    if (dependenciesBeginPosition >= 0) {
      for (int i = dependenciesBeginPosition + 1; i < allLines.size(); i++) {
        String dependency = allLines.get(i);
        // check if this is the end of the block
        if (dependency.equals("}") && isEndOfABlock(allLines, i + 1)) {
          break;
        }
        // ignore comments, empty lines and anything else that is not dependency
        if (!isDependencyDeclaration(CommonUtil.leftTrim(dependency))) {
          continue;
        }
        // Examples from mvnrepository - Gradle: #1, Gradle (Short): #2, Gradle (Kotlin): #3
        // 1: implementation group: 'ch.qos.logback', name: 'logback-classic', version: '1.4.5'
        // 2: implementation 'com.google.code.gson:gson:2.10.1'
        // 3: implementation ('com.google.code.gson:gson:2.10.1')
        // 4: testImplementation('org.springframework.boot:spring-boot-starter-test:2.3.0.RELEASE')
        //      { exclude group: 'org.junit.vintage', module: 'junit-vintage-engine' }
        // 5: implementation('org.slf4j:slf4j-api') { version { strictly '1.7.2' }}
        if (dependency.contains("(") && dependency.contains(")")) {
          dependency = dependency.replace("(", " ").replace(")", " ");
        }

        String formattedDependency = getFormattedDependencyString(dependency);
        if (formattedDependency == null) {
          continue;
        }
        String[] formattedDependencyArray = formattedDependency.split(":");
        if (formattedDependencyArray.length != 3) {
          // From above examples - this matches #5, hence ignored
          continue;
        }
        dependenciesMap.put(dependency, getDependency(formattedDependencyArray));
      }
    } else {
      log.info("No dependencies in the project...");
    }

    return dependenciesMap;
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

  private String getFormattedDependencyString(final String dependency) {
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
        return group;
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
        return group + ":" + String.join(":", artifactVersion);
      }
    }

    return null;
  }

  private MavenDoc getDependency(final String[] formattedDependencyArray) {
    return MavenDoc.builder()
        .g(formattedDependencyArray[0])
        .a(formattedDependencyArray[1])
        .v(formattedDependencyArray[2])
        .build();
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
