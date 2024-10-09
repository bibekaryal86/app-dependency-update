package app.dependency.update.app.controller;

import static app.dependency.update.app.util.CommonUtils.*;
import static app.dependency.update.app.util.ProcessUtils.*;

import app.dependency.update.app.service.MongoRepoService;
import app.dependency.update.app.service.UpdateRepoService;
import app.dependency.update.app.util.AppInitDataUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AppTestController {

  private final UpdateRepoService updateRepoService;
  private final MongoRepoService mongoRepoService;

  @Operation(
      summary = "Ping",
      description = "Send a simple Http Request to receive a simple Http Response")
  @CrossOrigin
  @GetMapping(value = "/tests/ping", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> ping() {
    return ResponseEntity.ok("{\"ping\": \"successful\"}");
  }

  @Operation(summary = "Check Running Tasks", description = "Checks if tasks currently running")
  @GetMapping(value = "/tests/check", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> check() {
    return ResponseEntity.ok("{\"running\": " + updateRepoService.isTaskRunning() + "}");
  }

  @Operation(
      summary = "Check PR Create Errors",
      description = "Returns list of repositories with errors when creating PR")
  @GetMapping(value = "/tests/pr-create-errors", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> errors() throws JsonProcessingException {
    return ResponseEntity.ok(
        "{\"repos\": " + new ObjectMapper().writeValueAsString(getRepositoriesWithPrError()) + "}");
  }

  @Operation(summary = "Reset Caches", description = "Clears and sets caches")
  @GetMapping(
      value = "/tests/reset/{are_you_sure}/{cacheType}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> reset(
      @PathVariable(name = "are_you_sure") final boolean areYouSure,
      @PathVariable final CacheType cacheType) {
    if (areYouSure) {
      switch (cacheType) {
        case ALL -> {
          AppInitDataUtils.clearAppInitData();
          mongoRepoService.clearPluginsMap();
          mongoRepoService.clearDependenciesMap();
          AppInitDataUtils.appInitData();
          mongoRepoService.pluginsMap();
          mongoRepoService.dependenciesMap();
        }
        case APP_INIT_DATA -> {
          AppInitDataUtils.clearAppInitData();
          AppInitDataUtils.appInitData();
        }
        case PLUGINS_MAP -> {
          mongoRepoService.clearPluginsMap();
          mongoRepoService.pluginsMap();
        }
        case DEPENDENCIES_MAP -> {
          mongoRepoService.clearDependenciesMap();
          mongoRepoService.dependenciesMap();
        }
      }

      resetRepositoriesWithPrError();
      resetProcessedRepositoriesAndSummary();
      return ResponseEntity.ok("{\"reset\": \"successful\"}");
    } else {
      return ResponseEntity.badRequest().body("{\"wrong\": \"answer\"}");
    }
  }

  @Operation(summary = "Change Log Level", description = "Change Log Levels at Runtime")
  @GetMapping(value = "/tests/log/level", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> changeLogLevel(@RequestParam final LogLevelChange level) {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    Logger logger = loggerContext.getLogger("root");
    Level levelBefore = logger.getLevel();
    logger.setLevel(Level.toLevel(level.name()));
    Level levelAfter = logger.getLevel();
    return ResponseEntity.ok(
        String.format(
            "{\"logLevelBefore\":\"%s\",\"logLevelAfter\":\"%s\"}", levelBefore, levelAfter));
  }
}
