package app.dependency.update.app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@SuppressWarnings("ClassCanBeRecord")
@Getter
@Builder
@ToString
@AllArgsConstructor
public class GradlePlugin {
  private final String original;
  private final String group;
  private final String version;
  private final boolean skipVersion;
}
