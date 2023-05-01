package app.dependency.update.app.controller;

import app.dependency.update.app.service.UpdateRepoService;
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

  public AppTestController(final UpdateRepoService updateRepoService) {
    this.updateRepoService = updateRepoService;
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
  @GetMapping(value = "/tests/check/{are_you_sure}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> reset(@PathVariable(name = "are_you_sure") boolean areYouSure) {
    if (areYouSure) {
      return ResponseEntity.ok("{\"shutdown\": " + updateRepoService.isTaskRunning() + "}");
    } else {
      return ResponseEntity.badRequest().body("{\"wrong\": \"answer\"}");
    }
  }
}
