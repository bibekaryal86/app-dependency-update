package app.dependency.update.app.repository;

import app.dependency.update.app.model.dto.Dependencies;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DependenciesRepository extends MongoRepository<Dependencies, String> {}
