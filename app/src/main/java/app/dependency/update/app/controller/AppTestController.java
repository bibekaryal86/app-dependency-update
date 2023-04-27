package app.dependency.update.app.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppTestController {
  @Operation(
      summary = "Ping",
      description = "Send a simple Http Request to receive a simple Http Response")
  @CrossOrigin
  @GetMapping(value = "/tests/ping", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> pingTest() {
    return ResponseEntity.ok("{\"ping\": \"successful\"}");
  }
}
