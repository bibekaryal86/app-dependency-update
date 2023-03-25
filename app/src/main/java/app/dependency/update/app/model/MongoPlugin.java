package app.dependency.update.app.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MongoPlugin {
  private String group;
  private String artifact;
}
