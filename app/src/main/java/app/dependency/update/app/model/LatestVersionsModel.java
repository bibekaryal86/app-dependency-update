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
public class LatestVersionsModel {
  private LatestVersionServers latestVersionServers;
  private LatestVersionTools latestVersionTools;
  private LatestVersionGithubActions latestVersionGithubActions;
  private LatestVersionLanguages latestVersionLanguages;
}
