package app.dependency.update.app.util;

import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import app.dependency.update.app.model.MongoDependencies;
import app.dependency.update.app.model.MongoPlugins;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.pojo.PojoCodecProvider;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MongoUtil {

  private static final String MONGODB_URL =
      "mongodb+srv://%s:%s@cluster0.anwaeio.mongodb.net/?retryWrites=true&w=majority";
  private static String username = null;
  private static String password = null;

  private static void initMongo() {
    if (username == null || password == null) {
      // this will not be null as it is checked during app initialization
      username = CommonUtil.getSystemEnvProperty(CommonUtil.ENV_MONGO_USERNAME, null);
      password = CommonUtil.getSystemEnvProperty(CommonUtil.ENV_MONGO_PASSWORD, null);
    }
  }

  public static List<MongoPlugins> retrievePlugins() {
    // for gradle plugins group:artifact are not available in build.gradle, only group is provided
    // since relatively low number of plugins used, save group:artifact in mongodb and retrieve
    // another option was to put in a properties file and read from it, but this seems better
    List<MongoPlugins> mongoPlugins = new ArrayList<>();
    try (MongoClient mongoClient = MongoClients.create(getMongoClientSettings())) {
      FindIterable<MongoPlugins> gradlePluginFindIterable =
          getMongoCollectionPlugins(mongoClient).find();
      gradlePluginFindIterable.forEach(mongoPlugins::add);
    } catch (Exception ex) {
      throw new AppDependencyUpdateRuntimeException("Error Retrieving Mongo Plugins List", ex);
    }
    return mongoPlugins;
  }

  public static List<MongoDependencies> retrieveDependencies() {
    // for gradle plugins group:artifact are not available in build.gradle, only group is provided
    // since relatively low number of plugins used, save group:artifact in mongodb and retrieve
    // another option was to put in a properties file and read from it, but this seems better
    List<MongoDependencies> mongoDependencies = new ArrayList<>();
    try (MongoClient mongoClient = MongoClients.create(getMongoClientSettings())) {
      FindIterable<MongoDependencies> gradlePluginFindIterable =
          getMongoCollectionDependencies(mongoClient).find();
      gradlePluginFindIterable.forEach(mongoDependencies::add);
    } catch (Exception ex) {
      throw new AppDependencyUpdateRuntimeException("Error Retrieving Mongo Dependencies List", ex);
    }
    return mongoDependencies;
  }

  private static MongoClientSettings getMongoClientSettings() {
    initMongo();
    return MongoClientSettings.builder()
        .applyConnectionString(new ConnectionString(String.format(MONGODB_URL, username, password)))
        .serverApi(ServerApi.builder().version(ServerApiVersion.V1).build())
        .codecRegistry(
            CodecRegistries.fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build())))
        .build();
  }

  private static MongoCollection<MongoPlugins> getMongoCollectionPlugins(MongoClient mongoClient) {
    return mongoClient
        .getDatabase(CommonUtil.MONGODB_DATABASE_NAME)
        .getCollection("plugins", MongoPlugins.class);
  }

  private static MongoCollection<MongoDependencies> getMongoCollectionDependencies(
      MongoClient mongoClient) {
    return mongoClient
        .getDatabase(CommonUtil.MONGODB_DATABASE_NAME)
        .getCollection("dependencies", MongoDependencies.class);
  }
}
