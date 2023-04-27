/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package app.dependency.update;

import static app.dependency.update.app.util.CommonUtils.*;
import static app.dependency.update.app.util.ConstantUtils.*;

import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableMongoRepositories
public class App {

  public static void main(String[] args) {
    log.info("Begin app-dependency-update initialization...");
    SpringApplication app = new SpringApplication(App.class);
    app.setDefaultProperties(
        Collections.singletonMap("server.port", getSystemEnvProperty(SERVER_PORT, "8888")));
    app.run(args);
    log.info("End app-dependency-update initialization...");
  }
}
