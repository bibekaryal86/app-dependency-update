package app.dependency.update.app.repository;

import app.dependency.update.app.model.entities.ProcessSummaries;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessSummariesRepository extends MongoRepository<ProcessSummaries, String> {
  // find by updateType with pagination
  Page<ProcessSummaries> findByUpdateType(String updateType, Pageable pageable);

  // find by updateDateTime for a given date (hence between start and end of day)
  @Query("{ 'updateDateTime' : { $gte: ?0, $lt: ?1 } }")
  List<ProcessSummaries> findByUpdateDate(LocalDateTime startOfDay, LocalDateTime endOfDay);

  // find by updateType and updateDateTime
  @Query("{ 'updateType': ?0, 'updateDateTime' : { $gte: ?1, $lt: ?2 } }")
  List<ProcessSummaries> findByUpdateTypeAndUpdateDate(
      String updateType, LocalDateTime startOfDay, LocalDateTime endOfDay);
}
