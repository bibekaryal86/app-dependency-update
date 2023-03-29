package app.dependency.update.app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
@AllArgsConstructor
public class MavenDoc {
  private final String g;
  private final String a;
  private final String v;
}
