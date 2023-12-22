package app.dependency.update.app.repository;

import app.dependency.update.app.model.dto.Plugins;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PluginsRepository extends MongoRepository<Plugins, String> {}
