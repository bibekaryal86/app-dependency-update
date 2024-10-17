package app.dependency.update.app.model.entities;

import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.model.LatestVersion;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = MONGODB_COLLECTION_LATEST_VERSIONS)
public class LatestVersionsEntity {
  @MongoId private ObjectId id;
  private LocalDateTime updateDateTime;

  // servers
  private LatestVersion nginx;
  // tools
  private LatestVersion gradle;
  private LatestVersion flyway;
  // actions
  private LatestVersion checkout;
  private LatestVersion setupJava;
  private LatestVersion setupGradle;
  private LatestVersion setupNode;
  private LatestVersion setupPython;
  private LatestVersion codeql;
  // languages
  private LatestVersion java;
  private LatestVersion node;
  private LatestVersion python;
}
