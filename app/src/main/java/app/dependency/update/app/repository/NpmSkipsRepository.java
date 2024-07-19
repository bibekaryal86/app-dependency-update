package app.dependency.update.app.repository;

import app.dependency.update.app.model.entities.NpmSkips;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NpmSkipsRepository extends MongoRepository<NpmSkips, String> {}
