package app.dependency.update.app.service;

import static app.dependency.update.app.util.CommonUtils.isCheckPreReleaseVersion;
import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.connector.GradleConnector;
import app.dependency.update.app.model.GradleReleaseResponse;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GradleRepoService {

  private final GradleConnector gradleConnector;

  public GradleRepoService(GradleConnector gradleConnector) {
    this.gradleConnector = gradleConnector;
  }

  public String getLatestGradleVersion() {
    List<GradleReleaseResponse> gradleReleaseResponses = gradleConnector.getGradleReleases();
    // get rid of draft and prerelease and sort by name descending
    Optional<GradleReleaseResponse> optionalLatestGradleRelease =
        gradleReleaseResponses.stream()
            .filter(
                gradleReleaseResponse ->
                    !(gradleReleaseResponse.isPrerelease() || gradleReleaseResponse.isDraft()))
            .max(Comparator.comparing(GradleReleaseResponse::getName));

    GradleReleaseResponse latestGradleRelease = optionalLatestGradleRelease.orElse(null);
    log.info("Latest Gradle Release: [ {} ]", latestGradleRelease);

    if (latestGradleRelease == null) {
      log.error("Latest Gradle Release Null Error...");
      return null;
    }

    return latestGradleRelease.getName();
  }

  public String getLatestGradlePlugin(final String group) {
    log.debug("Get Latest Gradle Plugin: [ {} ]", group);
    Document document = gradleConnector.getGradlePlugins(group);
    log.debug("Gradle Plugin Document: [ {} ] | [ {} ]", group, document);
    if (document != null) {
      Element versionElement = document.getElementsByClass("version-info").first();

      if (versionElement != null) {
        Element latestVersionElement = versionElement.selectFirst("h3");

        if (latestVersionElement != null) {
          String latestVersionText = latestVersionElement.text();
          return getLatestVersion(latestVersionText);
        } else {
          log.error("ERROR Latest Version Element is NULL: [ {} ]", group);
        }
      } else {
        log.error("ERROR Version Element is NULL: [ {} ]", group);
      }
    }
    return null;
  }

  private String getLatestVersion(final String latestVersionText) {
    String[] latestVersionTextArray = latestVersionText.split(" ");
    if (latestVersionTextArray.length == 3) {
      String version = latestVersionTextArray[1];
      if (!isCheckPreReleaseVersion(version)) {
        return version;
      }
    } else {
      log.error("ERROR Get Latest Gradle Plugin Version Wrong Length: [ {} ]", latestVersionText);
    }
    return null;
  }
}
