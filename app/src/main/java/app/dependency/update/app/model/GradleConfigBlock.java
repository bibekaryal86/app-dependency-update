package app.dependency.update.app.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
@AllArgsConstructor
public class GradleConfigBlock {
  private final List<GradleDefinition> definitions;
  private final List<GradleDependency> dependencies;
}