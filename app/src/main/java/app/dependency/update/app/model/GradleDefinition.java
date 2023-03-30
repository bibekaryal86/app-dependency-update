package app.dependency.update.app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
@AllArgsConstructor
public class GradleDefinition {
  private final String original;
  private final String name;
  private final String value;
}
