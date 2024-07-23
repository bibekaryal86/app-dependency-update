package app.dependency.update.app.model.entities;

import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.model.ProcessSummary;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = MONGODB_COLLECTION_PROCESS_SUMMARIES)
public class ProcessSummaries extends ProcessSummary {
  @MongoId private ObjectId id;
  private LocalDateTime updateDateTime;
}
