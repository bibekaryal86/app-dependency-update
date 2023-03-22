package app.dependency.update.app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonProperty;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MongoDependencies {
  @BsonProperty private String id;
  private String latestVersion;
}