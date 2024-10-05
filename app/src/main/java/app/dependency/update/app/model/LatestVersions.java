package app.dependency.update.app.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LatestVersions {
  private LatestVersionRuntimes latestVersionRuntimes;
  private LatestVersionBuildTools latestVersionBuildTools;
  private LatestVersionGcp latestVersionGcp;
  private LatestVersionDocker latestVersionDocker;
  private LatestVersionGithubActions latestVersionsGithubActions;
}
