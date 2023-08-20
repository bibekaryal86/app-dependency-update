package app.dependency.update.app.controller;

import static app.dependency.update.app.util.CommonUtils.*;

import app.dependency.update.app.service.AppInitDataService;
import app.dependency.update.app.service.MavenRepoService;
import app.dependency.update.app.service.UpdateRepoService;
import app.dependency.update.app.util.CommonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppTestController {

  private final UpdateRepoService updateRepoService;
  private final AppInitDataService appInitDataService;
  private final MavenRepoService mavenRepoService;

  public AppTestController(
      final UpdateRepoService updateRepoService,
      final AppInitDataService appInitDataService,
      final MavenRepoService mavenRepoService) {
    this.updateRepoService = updateRepoService;
    this.appInitDataService = appInitDataService;
    this.mavenRepoService = mavenRepoService;
  }

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
        "{\"repos\": "
            + new ObjectMapper().writeValueAsString(CommonUtils.getRepositoriesWithPrError())
            + "}");
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
          appInitDataService.clearAppInitData();
          mavenRepoService.clearPluginsMap();
          mavenRepoService.clearDependenciesMap();
          appInitDataService.appInitData();
          mavenRepoService.pluginsMap();
          mavenRepoService.dependenciesMap();
        }
        case APP_INIT_DATA -> {
          appInitDataService.clearAppInitData();
          appInitDataService.appInitData();
        }
        case PLUGINS_MAP -> {
          mavenRepoService.clearPluginsMap();
          mavenRepoService.pluginsMap();
        }
        case DEPENDENCIES_MAP -> {
          mavenRepoService.clearDependenciesMap();
          mavenRepoService.dependenciesMap();
        }
      }

      CommonUtils.resetRepositoriesWithPrError();
      return ResponseEntity.ok("{\"reset\": \"successful\"}");
    } else {
      return ResponseEntity.badRequest().body("{\"wrong\": \"answer\"}");
    }
  }
}
