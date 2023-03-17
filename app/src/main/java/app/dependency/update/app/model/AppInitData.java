package app.dependency.update.app.model;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AppInitData {
  private final Map<String, String> argsMap;
  private final List<ScriptFile> scriptFiles;
  private final List<Repository> repositories;
}
