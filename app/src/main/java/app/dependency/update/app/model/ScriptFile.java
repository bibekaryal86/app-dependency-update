package app.dependency.update.app.model;

import static app.dependency.update.app.util.CommonUtils.*;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ScriptFile {
  private final String scriptFileName;
  private final UpdateType type;

  public ScriptFile(String scriptFileName) {
    this.scriptFileName = scriptFileName;
    String[] sfnArray = scriptFileName.split("\\.");
    this.type = UpdateType.valueOf(sfnArray[0]);
  }
}
