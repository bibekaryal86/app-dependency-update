package app.dependency.update.app.controller;

import static app.dependency.update.app.util.ConstantUtils.PARAM_REPO_HOME;
import static app.dependency.update.app.util.ConstantUtils.PATH_DELIMITER;

import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import app.dependency.update.app.service.AppInitDataService;
import app.dependency.update.app.service.LogStreamService;
import io.swagger.v3.oas.annotations.Operation;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LogStreamController {

  private final LogStreamService logStreamService;
  private final String logHome;

  public LogStreamController(
      final LogStreamService logStreamService, final AppInitDataService appInitDataService) {
    this.logStreamService = logStreamService;
    this.logHome =
        appInitDataService
            .appInitData()
            .getArgsMap()
            .get(PARAM_REPO_HOME)
            .concat("/logs/app-dependency-update");
  }

  @Operation(
      summary = "Get Log File Names",
      description = "Return a list of log files including current and archive log files")
  @GetMapping(value = "/logs/list", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<String>> getLogFileNames() {
    try {
      return ResponseEntity.ok(logStreamService.getLogFileNames(this.logHome));
    } catch (AppDependencyUpdateRuntimeException ex) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Collections.singletonList(ex.getMessage()));
    }
  }

  @Operation(summary = "Get Log File Content", description = "Return the content of a log file")
  @GetMapping(value = "/logs/log", produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity<String> getLogFileContent(
      @RequestParam(defaultValue = "app-dependency-update.log") final String fileName) {
    try {
      Path logPath = Path.of(this.logHome + PATH_DELIMITER + fileName);
      return ResponseEntity.ok(logStreamService.getLogFileContent(logPath));
    } catch (IOException ex) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getClass().getName());
    }
  }
}
