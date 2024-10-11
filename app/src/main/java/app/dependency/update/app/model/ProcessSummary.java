package app.dependency.update.app.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessSummary {
  private String updateType;
  private int mongoPluginsToUpdate;
  private int mongoDependenciesToUpdate;
  private int mongoPackagesToUpdate;
  private int mongoNpmSkipsActive;
  private int totalPrCreatedCount;
  private int totalPrCreateErrorsCount;
  private int totalPrMergedCount;
  private List<ProcessedRepository> processedRepositories;
  private boolean isErrorsOrExceptions;
}
