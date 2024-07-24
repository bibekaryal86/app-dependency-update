package app.dependency.update.app.repository;

import app.dependency.update.app.model.entities.Packages;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PackagesRepository extends MongoRepository<Packages, String> {}
