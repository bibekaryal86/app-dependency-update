package app.dependency.update.app.model;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ScriptFile {
  private final String scriptFileName;
  private final boolean isNpm;

  public ScriptFile(String scriptFileName) {
    this.scriptFileName = scriptFileName;
    this.isNpm = scriptFileName.startsWith("npm__");
  }
}
