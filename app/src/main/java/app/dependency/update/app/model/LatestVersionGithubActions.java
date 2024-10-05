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
  private String checkout;
  private String setupJava;
  private String setupGradle;
  private String setupNode;
  private String setupPython;
  private String codeqlInit;
  private String codeqlAutobuild;
  private String codeqlAnalyze;
}
