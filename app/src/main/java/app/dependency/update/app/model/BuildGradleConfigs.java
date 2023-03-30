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
public class BuildGradleConfigs {
  private final List<String> originals;
  private final GradleConfigBlock plugins;
  private final GradleConfigBlock dependencies;
}
