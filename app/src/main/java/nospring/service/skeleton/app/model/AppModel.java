package nospring.service.skeleton.app.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppModel {
  private String varOne;
  private Integer varTwo;
  private boolean varThree;
}
