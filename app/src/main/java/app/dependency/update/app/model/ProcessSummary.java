package app.dependency.update.app.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
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
public class ProcessSummary {
  private int mongoPluginsToUpdate;
  private int mongoDependenciesToUpdate;
  private int mongoPackagesToUpdate;
  private int mongoNpmSkipsActive;
  private int npmRepositoriesUpdatedCount;
  private int gradleRepositoriesUpdatedCount;
  private int pythonRepositoriesUpdatedCount;
  private int npmRepositoriesMergedCount;
  private int gradleRepositoriesMergedCount;
  private int pythonRepositoriesMergedCount;
  private List<ProcessedRepository> processedRepositories;
}
