package app.dependency.update.app.model;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ScriptFile {
  private final String scriptFileName;
  private final String type;
  private final int step;

  public ScriptFile(String scriptFileName) {
    this.scriptFileName = scriptFileName;
    String[] sfnArray = scriptFileName.split("__");
    this.type = sfnArray[0];
    this.step = Integer.parseInt(sfnArray[1]);
  }
}
