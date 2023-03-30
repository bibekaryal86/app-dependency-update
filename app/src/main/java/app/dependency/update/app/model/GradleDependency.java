package app.dependency.update.app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
@AllArgsConstructor
public class GradleDependency {
  private final String original;
  private final String group;
  private final String artifact;
  private final String version;
}
