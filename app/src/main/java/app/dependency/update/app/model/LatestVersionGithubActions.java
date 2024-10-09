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
public class LatestVersionGithubActions {
  private LatestVersion checkout;
  private LatestVersion setupJava;
  private LatestVersion setupGradle;
  private LatestVersion setupNode;
  private LatestVersion setupPython;
  private LatestVersion codeql;
}
