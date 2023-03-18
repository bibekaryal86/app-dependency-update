package app.dependency.update.app.model;

import java.nio.file.Path;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class Repository {
  private final Path repoPath;
  private final String type;
  private final String repoName;

  public Repository(Path repoPath, String type) {
    this.repoPath = repoPath;
    this.type = type;
    this.repoName = repoPath.getFileName().toString();
  }
}