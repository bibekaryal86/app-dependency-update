package app.dependency.update.app.runnable;

import static app.dependency.update.app.util.CommonUtils.*;
import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import app.dependency.update.app.model.AppInitData;
import app.dependency.update.app.model.Repository;
import app.dependency.update.app.model.ScriptFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdateGradleWrapper {
  private final List<Repository> repositories;
  private final ScriptFile scriptFile;

  public UpdateGradleWrapper(final AppInitData appInitData, final String latestGradleVersion) {
    this.repositories =
        appInitData.getRepositories().stream()
            .filter(repository -> repository.getType().equals(UpdateType.GRADLE_DEPENDENCIES))
            .map(
                repository -> {
                  String currentVersion = getCurrentGradleVersionInRepo(repository);
                  if (isRequiresUpdate(currentVersion, latestGradleVersion)) {
                    return new Repository(
                        repository.getRepoPath(),
                        repository.getType(),
                        repository.getGradleModules(),
                        latestGradleVersion);
                  }
                  return null;
                })
            .filter(Objects::nonNull)
            .toList();
    this.scriptFile =
        appInitData.getScriptFiles().stream()
            .filter(sf -> sf.getType().equals(UpdateType.GRADLE_WRAPPER))
            .findFirst()
            .orElseThrow(
                () ->
                    new AppDependencyUpdateRuntimeException("Gradle Wrapper Script Not Found..."));
  }

  public void updateGradleWrapper() {
    this.repositories.forEach(this::executeUpdate);
  }

  private String getCurrentGradleVersionInRepo(final Repository repository) {
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

  private String parseDistributionUrlForGradleVersion(final String distributionUrl) {
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

  private void executeUpdate(final Repository repository) {
    log.info("Execute Gradle Wrapper Update on: [ {} ]", repository);
    List<String> arguments = new LinkedList<>();
    arguments.add(repository.getRepoPath().toString());
    arguments.add(String.format(BRANCH_UPDATE_WRAPPER, LocalDate.now()));
    arguments.add(repository.getGradleVersion());
    new ExecuteScriptFile(
            threadName(repository, scriptFile, this.getClass().getSimpleName()),
            this.scriptFile,
            arguments)
        .start();
  }
}
