package app.dependency.update.app.service;

import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.connector.GithubActionsConnector;
import app.dependency.update.app.model.GithubActionsReleaseResponse;
import app.dependency.update.app.model.LatestVersion;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GithubActionsService {

  private final GithubActionsConnector githubActionsConnector;

  public GithubActionsService(GithubActionsConnector githubActionsConnector) {
    this.githubActionsConnector = githubActionsConnector;
  }

  public LatestVersion getLatestCheckout() {
    return getLatestGithubActions("actions", "checkout");
  }

  public LatestVersion getLatestSetupJava() {
    return getLatestGithubActions("actions", "setup-java");
  }

  public LatestVersion getLatestSetupNode() {
    return getLatestGithubActions("actions", "setup-node");
  }

  public LatestVersion getLatestSetupPython() {
    return getLatestGithubActions("actions", "setup-python");
  }

  public LatestVersion getLatestSetupGradle() {
    return getLatestGithubActions("gradle", "actions");
  }

  public LatestVersion getLatestCodeql() {
    return getLatestGithubActions("github", "codeql-action");
  }

  private LatestVersion getLatestGithubActions(final String owner, final String repo) {
    final String endpoint = String.format(GITHUB_ACTIONS_RELEASES_ENDPOINT, owner, repo);
    List<GithubActionsReleaseResponse> githubActionsReleaseResponses =
        githubActionsConnector.getGithubActionsReleases(endpoint);

    Optional<GithubActionsReleaseResponse> optionalLatestGithubActionRelease =
        githubActionsReleaseResponses.stream()
            .filter(
                githubActionReleaseResponse ->
                    !(githubActionReleaseResponse.isPrerelease()
                        || githubActionReleaseResponse.isDraft()))
            .findFirst();

    GithubActionsReleaseResponse latestGithubActionResponse =
        optionalLatestGithubActionRelease.orElse(null);
    log.info("Latest Github Action [{}/{}] Release: [{}]", owner, repo, latestGithubActionResponse);

    if (latestGithubActionResponse == null) {
      log.error("Latest Github Action Release Null Error for [{}] / [{}]...", owner, repo);
      return null;
    }

    final String versionActual = latestGithubActionResponse.getTagName();
    final String versionFull = getVersionFull(versionActual);
    final String versionMajor = getVersionMajor(versionFull);

    return LatestVersion.builder()
        .versionActual(versionActual)
        .versionFull(versionFull)
        .versionMajor(versionMajor)
        .build();
  }

  /**
   * @param versionActual eg: v4.2.0 or codeql-bundle-v2.19.1
   * @return eg: 4.2.0 or 2.19.1
   */
  private String getVersionFull(final String versionActual) {
    return versionActual.replaceAll("[^0-9.]", "");
  }

  /**
   * @param versionFull eg: 4.2.0 or 2.19.1
   * @return eg: 4 or 2
   */
  private String getVersionMajor(final String versionFull) {
    return versionFull.trim().split("\\.")[0];
  }
}
