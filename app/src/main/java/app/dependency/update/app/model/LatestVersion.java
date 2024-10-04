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
public class LatestVersion {
  private String java;
  private String node;
  private String python;
  private String nginx;
  private String gradle;
  private String gaCheckout;
  private String gaSetupJava;
  private String gaSetupGradle;
  private String gaSetupNode;
  private String gaSetupPython;
  private String gaCodeqlInit;
  private String gaCodeqlAutobuild;
  private String gaCodeqlAnalyze;
}
