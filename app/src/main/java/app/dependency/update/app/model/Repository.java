package app.dependency.update.app.model;

import static app.dependency.update.app.util.CommonUtils.*;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class Repository {
  private final Path repoPath;
  private final UpdateType type;
  private final String repoName;
  private final String latestGradleVersion;
  private final String currentGradleVersion;
  private final List<String> gradleModules;

  public Repository(Path repoPath, UpdateType type) {
    this.repoPath = repoPath;
    this.type = type;
    this.repoName = repoPath.getFileName().toString();
    this.latestGradleVersion = "";
    this.currentGradleVersion = "";
    this.gradleModules = Collections.emptyList();
  }

  public Repository(Path repoPath, UpdateType type, List<String> gradleModules) {
    this.repoPath = repoPath;
    this.type = type;
    this.repoName = repoPath.getFileName().toString();
    this.latestGradleVersion = "";
    this.currentGradleVersion = "";
    this.gradleModules = gradleModules;
  }

  public Repository(
      Path repoPath,
      UpdateType type,
      List<String> gradleModules,
      String latestGradleVersion,
      String currentGradleVersion) {
    this.repoPath = repoPath;
    this.type = type;
    this.gradleModules = gradleModules;
    this.repoName = repoPath.getFileName().toString();
    this.latestGradleVersion = latestGradleVersion;
    this.currentGradleVersion = currentGradleVersion;
  }
}
