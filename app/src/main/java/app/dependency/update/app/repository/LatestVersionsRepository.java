package app.dependency.update.app.repository;

import app.dependency.update.app.model.entities.LatestVersionsEntity;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LatestVersionsRepository extends MongoRepository<LatestVersionsEntity, String> {
  // find the most recent based on updateDateTime
  Optional<LatestVersionsEntity> findFirstByOrderByUpdateDateTimeDesc();
}
