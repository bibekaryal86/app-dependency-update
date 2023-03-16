package update.dependencies.app.model;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ScriptFile {
  private final String originalFileName;
  private final String scriptFileName;
  private final boolean isNpm;

  public ScriptFile(String originalFileName) {
    this.originalFileName = originalFileName;
    String[] origFileNameArray = originalFileName.split("___");
    this.scriptFileName = origFileNameArray[1];
    this.isNpm = origFileNameArray[0].equals("npm");
  }
}
