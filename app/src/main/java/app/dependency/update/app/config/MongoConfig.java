package app.dependency.update.app.config;

import static app.dependency.update.app.util.CommonUtils.*;
import static app.dependency.update.app.util.ConstantUtils.*;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

@Configuration
public class MongoConfig {

  @Bean
  public MongoClient mongoClient() {
    String username = getSystemEnvProperty(ENV_MONGO_USERNAME);
    String password = getSystemEnvProperty(ENV_MONGO_PASSWORD);
    return MongoClients.create(
        MongoClientSettings.builder()
            .applyConnectionString(
                new ConnectionString(String.format(MONGODB_CONNECTION_STRING, username, password)))
            .build());
  }

  @Bean
  public MongoDatabaseFactory mongoDatabaseFactory(final MongoClient mongoClient) {
    return new SimpleMongoClientDatabaseFactory(mongoClient, MONGODB_DATABASE_NAME);
  }

  @Bean
  public MongoTemplate mongoTemplate(final MongoDatabaseFactory mongoDatabaseFactory) {
    return new MongoTemplate(mongoDatabaseFactory);
  }
}
