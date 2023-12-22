package app.dependency.update.app.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@SuppressWarnings("ClassCanBeRecord")
@Getter
@Builder
@ToString
@AllArgsConstructor
public class GradleConfigBlock {
  private final List<GradleDefinition> definitions;
  private final List<GradleDependency> dependencies;
  private final List<GradlePlugin> plugins;
}
