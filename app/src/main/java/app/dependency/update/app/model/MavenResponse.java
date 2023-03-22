package app.dependency.update.app.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public class MavenResponse {
  private final List<MavenDocs> docs;
}
