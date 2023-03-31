package app.dependency.update.app.execute;

import app.dependency.update.app.model.BuildGradleConfigs;
import app.dependency.update.app.model.GradleConfigBlock;
import app.dependency.update.app.model.GradleDefinition;
import app.dependency.update.app.model.GradleDependency;
import app.dependency.update.app.util.CommonUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ModifyBuildGradle {

  private final BuildGradleConfigs buildGradleConfigs;

  public ModifyBuildGradle(final BuildGradleConfigs buildGradleConfigs) {
    this.buildGradleConfigs = buildGradleConfigs;
  }

  public List<String> modifyBuildGradle() {
    final List<String> originals = new ArrayList<>(this.buildGradleConfigs.getOriginals());

    final GradleConfigBlock pluginsBlock = this.buildGradleConfigs.getPlugins();
    modifyConfigurations(pluginsBlock, originals);

    final GradleConfigBlock dependenciesBlock = this.buildGradleConfigs.getDependencies();
    modifyConfigurations(dependenciesBlock, originals);

    if (originals.equals(this.buildGradleConfigs.getOriginals())) {
      log.info("Nothing to Update: [{}]", this.buildGradleConfigs.getBuildGradlePath());
      return null;
    } else {
      return originals;
    }
  }

  // suppressing sonarlint rule for cognitive complexity of method too high
  @SuppressWarnings("java:S3776")
  private void modifyConfigurations(
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

            String updatedOriginal =
                modifyConfiguration(modifiedGradleDependency, gradleDefinition);

            if (updatedOriginal != null) {
              int index = originals.indexOf(gradleDefinition.getOriginal());
              if (index >= 0) {
                originals.set(index, updatedOriginal);
                updatedDefinitions.add(definitionName);
              }
            }
          }
        } else {
          String updatedOriginal = modifyConfiguration(gradleDependency, null);
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

  private String modifyConfiguration(
      final GradleDependency gradleDependency, final GradleDefinition gradleDefinition) {
    String latestVersion =
        new MavenRepo()
            .getLatestVersion(
                gradleDependency.getGroup(),
                gradleDependency.getArtifact(),
                gradleDefinition == null
                    ? gradleDependency.getVersion()
                    : gradleDefinition.getValue(),
                false);

    if (CommonUtil.isRequiresUpdate(gradleDependency.getVersion(), latestVersion)) {
      return gradleDefinition == null
          ? gradleDependency.getOriginal().replace(gradleDependency.getVersion(), latestVersion)
          : gradleDefinition.getOriginal().replace(gradleDefinition.getValue(), latestVersion);
    }

    return null;
  }
}
