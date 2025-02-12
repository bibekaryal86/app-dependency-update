package app.dependency.update.app.service;

import app.dependency.update.app.model.LatestVersion;
import app.dependency.update.app.model.LatestVersionGithubActions;
import app.dependency.update.app.model.LatestVersionLanguages;
import app.dependency.update.app.model.LatestVersionServers;
import app.dependency.update.app.model.LatestVersionTools;
import app.dependency.update.app.model.LatestVersionsModel;
import app.dependency.update.app.model.entities.LatestVersionsEntity;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LatestVersionsService {

  private final MongoRepoService mongoRepoService;
  private final GithubActionsService githubActionsService;
  private final GradleRepoService gradleRepoService;
  private final JavaService javaService;
  private final NginxService nginxService;
  private final NodeService nodeService;
  private final PythonService pythonService;
  private final GcpService gcpService;
  private final FlywayService flywayService;

  public LatestVersionsModel getLatestVersions() {
    log.debug("Get Latest Versions...");

    Optional<LatestVersionsEntity> mostRecentLatestVersionsOptional =
        mongoRepoService.getMostRecentLatestVersionsEntity();

    if (mostRecentLatestVersionsOptional.isEmpty()) {
      return null;
    }

    Map<String, String> latestGcpRuntimes = gcpService.getLatestGcpRuntimes();
    LatestVersionsEntity latestVersionsEntity = mostRecentLatestVersionsOptional.get();

    // languages
    LatestVersion python =
        getLatestVersionPython(latestVersionsEntity.getPython(), latestGcpRuntimes.get("python"));
    LatestVersion node =
        getLatestVersionNode(latestVersionsEntity.getNode(), latestGcpRuntimes.get("nodejs"));
    LatestVersion java =
        getLatestVersionJava(latestVersionsEntity.getJava(), latestGcpRuntimes.get("java"));
    // actions
    LatestVersion codeql = getLatestVersionGithubCodeql(latestVersionsEntity.getCodeql());
    LatestVersion setupPython =
        getLatestVersionGithubSetupPython(latestVersionsEntity.getSetupPython());
    LatestVersion setupNode = getLatestVersionGithubSetupNode(latestVersionsEntity.getSetupNode());
    LatestVersion setupGradle =
        getLatestVersionGithubSetupGradle(latestVersionsEntity.getSetupGradle());
    LatestVersion setupJava = getLatestVersionGithubSetupJava(latestVersionsEntity.getSetupJava());
    LatestVersion checkout = getLatestVersionGithubCheckout(latestVersionsEntity.getCheckout());
    // tools
    LatestVersion gradle =
        getLatestVersionGradle(latestVersionsEntity.getGradle(), java.getVersionMajor());
    LatestVersion flyway = getLatestVersionFlyway(latestVersionsEntity.getFlyway());
    // servers
    LatestVersion nginx = getLatestVersionNginx(latestVersionsEntity.getNginx());

    LatestVersionsModel latestVersionsModel =
        LatestVersionsModel.builder()
            .latestVersionServers(LatestVersionServers.builder().nginx(nginx).build())
            .latestVersionTools(LatestVersionTools.builder().gradle(gradle).flyway(flyway).build())
            .latestVersionGithubActions(
                LatestVersionGithubActions.builder()
                    .checkout(checkout)
                    .setupJava(setupJava)
                    .setupGradle(setupGradle)
                    .setupNode(setupNode)
                    .setupPython(setupPython)
                    .codeql(codeql)
                    .build())
            .latestVersionLanguages(
                LatestVersionLanguages.builder().java(java).node(node).python(python).build())
            .build();

    if (checkAndSaveLatestVersionEntity(latestVersionsEntity, latestVersionsModel)) {
      mongoRepoService.saveLatestVersions(latestVersionsModel);
    }

    return latestVersionsModel;
  }

  private LatestVersion getLatestVersionNginx(final LatestVersion latestVersionInMongo) {
    try {
      return nginxService.getLatestNginxVersion(latestVersionInMongo.getVersionDocker());
    } catch (Exception ex) {
      log.error("Get Latest Version Nginx...", ex);
    }
    return latestVersionInMongo;
  }

  private LatestVersion getLatestVersionGradle(
      final LatestVersion latestVersionInMongo, final String latestJavaVersionMajor) {
    try {
      return gradleRepoService.getLatestGradleVersion(
          latestJavaVersionMajor, latestVersionInMongo.getVersionDocker());
    } catch (Exception ex) {
      log.error("Get Latest Version Gradle...", ex);
    }
    return latestVersionInMongo;
  }

  private LatestVersion getLatestVersionGithubCheckout(final LatestVersion latestVersionInMongo) {
    try {
      return githubActionsService.getLatestCheckout();
    } catch (Exception ex) {
      log.error("Get Latest Version Github Checkout...", ex);
    }
    return latestVersionInMongo;
  }

  private LatestVersion getLatestVersionGithubSetupJava(final LatestVersion latestVersionInMongo) {
    try {
      return githubActionsService.getLatestSetupJava();
    } catch (Exception ex) {
      log.error("Get Latest Version Github Setup Java...", ex);
    }
    return latestVersionInMongo;
  }

  private LatestVersion getLatestVersionGithubSetupGradle(
      final LatestVersion latestVersionInMongo) {
    try {
      return githubActionsService.getLatestSetupGradle();
    } catch (Exception ex) {
      log.error("Get Latest Version Github Setup Gradle...", ex);
    }
    return latestVersionInMongo;
  }

  private LatestVersion getLatestVersionGithubSetupNode(final LatestVersion latestVersionInMongo) {
    try {
      return githubActionsService.getLatestSetupNode();
    } catch (Exception ex) {
      log.error("Get Latest Version Github Setup Node...", ex);
    }
    return latestVersionInMongo;
  }

  private LatestVersion getLatestVersionGithubSetupPython(
      final LatestVersion latestVersionInMongo) {
    try {
      return githubActionsService.getLatestSetupPython();
    } catch (Exception ex) {
      log.error("Get Latest Version Github Setup Python...", ex);
    }
    return latestVersionInMongo;
  }

  private LatestVersion getLatestVersionGithubCodeql(final LatestVersion latestVersionInMongo) {
    try {
      return githubActionsService.getLatestCodeql();
    } catch (Exception ex) {
      log.error("Get Latest Version Github Codeql...", ex);
    }
    return latestVersionInMongo;
  }

  private LatestVersion getLatestVersionFlyway(final LatestVersion latestVersionInMongo) {
    try {
      return flywayService.getLatestFlywayVersion();
    } catch (Exception ex) {
      log.error("Get Latest Version Github Flyway...", ex);
    }
    return latestVersionInMongo;
  }

  private LatestVersion getLatestVersionJava(
      final LatestVersion latestVersionInMongo, final String latestGcpRuntimeVersion) {
    try {
      return javaService.getLatestJavaVersion(
          latestGcpRuntimeVersion, latestVersionInMongo.getVersionDocker());
    } catch (Exception ex) {
      log.error("Get Latest Version Java...", ex);
    }
    return latestVersionInMongo;
  }

  private LatestVersion getLatestVersionNode(
      final LatestVersion latestVersionInMongo, final String latestGcpRuntimeVersion) {
    try {
      return nodeService.getLatestNodeVersion(
          latestGcpRuntimeVersion, latestVersionInMongo.getVersionDocker());
    } catch (Exception ex) {
      log.error("Get Latest Version Node...", ex);
    }
    return latestVersionInMongo;
  }

  private LatestVersion getLatestVersionPython(
      final LatestVersion latestVersionInMongo, final String latestGcpRuntimeVersion) {
    try {
      return pythonService.getLatestPythonVersion(
          latestGcpRuntimeVersion, latestVersionInMongo.getVersionDocker());
    } catch (Exception ex) {
      log.error("Get Latest Version Python...", ex);
    }
    return latestVersionInMongo;
  }

  private boolean checkAndSaveLatestVersionEntity(
      final LatestVersionsEntity latestVersionsEntity,
      final LatestVersionsModel latestVersionsModel) {
    if (!latestVersionsEntity
        .getNginx()
        .getVersionActual()
        .equals(latestVersionsModel.getLatestVersionServers().getNginx().getVersionActual())) {
      return true;
    }
    if (!latestVersionsEntity
        .getGradle()
        .getVersionActual()
        .equals(latestVersionsModel.getLatestVersionTools().getGradle().getVersionActual())) {
      return true;
    }
    if (!latestVersionsEntity
        .getCheckout()
        .getVersionActual()
        .equals(
            latestVersionsModel.getLatestVersionGithubActions().getCheckout().getVersionActual())) {
      return true;
    }
    if (!latestVersionsEntity
        .getSetupJava()
        .getVersionActual()
        .equals(
            latestVersionsModel
                .getLatestVersionGithubActions()
                .getSetupJava()
                .getVersionActual())) {
      return true;
    }
    if (!latestVersionsEntity
        .getSetupGradle()
        .getVersionActual()
        .equals(
            latestVersionsModel
                .getLatestVersionGithubActions()
                .getSetupGradle()
                .getVersionActual())) {
      return true;
    }
    if (!latestVersionsEntity
        .getSetupNode()
        .getVersionActual()
        .equals(
            latestVersionsModel
                .getLatestVersionGithubActions()
                .getSetupNode()
                .getVersionActual())) {
      return true;
    }
    if (!latestVersionsEntity
        .getSetupPython()
        .getVersionActual()
        .equals(
            latestVersionsModel
                .getLatestVersionGithubActions()
                .getSetupPython()
                .getVersionActual())) {
      return true;
    }
    if (!latestVersionsEntity
        .getCodeql()
        .getVersionActual()
        .equals(
            latestVersionsModel.getLatestVersionGithubActions().getCodeql().getVersionActual())) {
      return true;
    }
    if (!latestVersionsEntity
        .getJava()
        .getVersionActual()
        .equals(latestVersionsModel.getLatestVersionLanguages().getJava().getVersionActual())) {
      return true;
    }
    if (!latestVersionsEntity
        .getNode()
        .getVersionActual()
        .equals(latestVersionsModel.getLatestVersionLanguages().getNode().getVersionActual())) {
      return true;
    }
    return !latestVersionsEntity
        .getPython()
        .getVersionActual()
        .equals(latestVersionsModel.getLatestVersionLanguages().getPython().getVersionActual());
  }
}
