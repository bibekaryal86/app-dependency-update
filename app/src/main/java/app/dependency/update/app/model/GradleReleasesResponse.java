package app.dependency.update.app.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public class GradleReleasesResponse {
  private final String name;
  private final boolean draft;
  private final boolean prerelease;
}
