package app.dependency.update.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppTestController {
  @Operation(
      summary = "Ping",
      description = "Send a simple Http Request to receive a simple Http Response")
  @CrossOrigin
  @GetMapping(value = "/tests/ping", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> ping() {
    return ResponseEntity.ok("{\"ping\": \"successful\"}");
  }

  @Operation(summary = "Reset", description = "Reset Pseudo Semaphore Lock")
  @GetMapping(value = "/tests/reset/{are_you_sure}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> reset(@PathVariable(name = "are_you_sure") boolean areYouSure) {
    if (areYouSure) {
      // TODO stop taskScheduler
      return ResponseEntity.ok("{\"reset\": \"successful\"}");
    } else {
      return ResponseEntity.badRequest().body("{\"wrong\": \"answer\"}");
    }
  }
}
