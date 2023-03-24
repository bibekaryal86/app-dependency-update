package app.dependency.update.app.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public class MavenDoc {
  private final String id;
  private final String latestVersion;
}
