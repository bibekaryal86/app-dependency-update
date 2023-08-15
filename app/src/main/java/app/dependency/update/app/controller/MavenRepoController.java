package app.dependency.update.app.controller;

import static app.dependency.update.app.util.CommonUtils.*;

import app.dependency.update.app.model.MongoDependencies;
import app.dependency.update.app.model.MongoPlugins;
import app.dependency.update.app.model.dto.Dependencies;
import app.dependency.update.app.model.dto.Plugins;
import app.dependency.update.app.service.MavenRepoService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.BeanUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/maven-repo")
public class MavenRepoController {

  private final MavenRepoService mavenRepoService;

  public MavenRepoController(final MavenRepoService mavenRepoService) {
    this.mavenRepoService = mavenRepoService;
  }

  @Operation(summary = "Get Plugins in Mongo Repository")
  @GetMapping(value = "/plugins", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<MongoPlugins>> getPlugins() {
    List<Plugins> plugins = mavenRepoService.pluginsMap().values().stream().toList();
    List<MongoPlugins> mongoPlugins =
        plugins.stream()
            .map(
                plugin ->
                    MongoPlugins.builder()
                        .group(plugin.getGroup())
                        .artifact(plugin.getArtifact())
                        .build())
            .toList();
    return ResponseEntity.ok(mongoPlugins);
  }

  @Operation(summary = "Save Plugin in Mongo Repository")
  @PostMapping(value = "/plugins", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> savePlugin(@RequestBody final MongoPlugins mongoPlugins) {
    if (mongoPlugins == null
        || isEmpty(mongoPlugins.getGroup())
        || isEmpty(mongoPlugins.getArtifact())) {
      return ResponseEntity.badRequest()
          .body("{\"save\": \"unsuccessful\", \"message\": \"Missing Input\"}");
    }

    try {
      Plugins plugin = mavenRepoService.pluginsMap().get(mongoPlugins.getGroup());
      if (plugin == null) {
        plugin = Plugins.builder().build();
      }
      BeanUtils.copyProperties(mongoPlugins, plugin);
      mavenRepoService.savePlugin(plugin);
      return ResponseEntity.ok("{\"save\": \"successful\"}");
    } catch (Exception ex) {
      return ResponseEntity.status(500)
          .body(
              "{\"save\": \"unsuccessful\", \"message\": \""
                  + ex.getMessage().replace("\"", "'")
                  + "\"}");
    }
  }

  @Operation(summary = "Get Dependencies in Mongo Repository")
  @GetMapping(value = "/dependencies", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<MongoDependencies>> getDependencies(
      @RequestParam(required = false) final String mavenId) {
    List<Dependencies> dependencies = mavenRepoService.dependenciesMap().values().stream().toList();
    List<MongoDependencies> mongoDependencies =
        dependencies.stream()
            .filter(dependency -> mavenId == null || mavenId.equals(dependency.getMavenId()))
            .map(
                dependency ->
                    MongoDependencies.builder()
                        .mavenId(dependency.getMavenId())
                        .latestVersion(dependency.getLatestVersion())
                        .skipVersion(dependency.isSkipVersion())
                        .build())
            .toList();
    return ResponseEntity.ok(mongoDependencies);
  }

  @Operation(summary = "Save Dependency in Mongo Repository")
  @PostMapping(value = "/dependencies", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> saveDependency(
      @RequestBody final MongoDependencies mongoDependencies) {
    if (mongoDependencies == null
        || isEmpty(mongoDependencies.getMavenId())
        || isEmpty(mongoDependencies.getLatestVersion())) {
      return ResponseEntity.badRequest()
          .body("{\"save\": \"unsuccessful\", \"message\": \"Missing Input\"}");
    }

    try {
      Dependencies dependency =
          mavenRepoService.dependenciesMap().get(mongoDependencies.getMavenId());
      if (dependency == null) {
        dependency = Dependencies.builder().build();
      }
      BeanUtils.copyProperties(mongoDependencies, dependency);
      mavenRepoService.saveDependency(dependency);
      return ResponseEntity.ok("{\"save\": \"successful\"}");
    } catch (Exception ex) {
      return ResponseEntity.status(500)
          .body(
              "{\"save\": \"unsuccessful\", \"message\": \""
                  + ex.getMessage().replace("\"", "'")
                  + "\"}");
    }
  }

  @Operation(summary = "On-demand Update Maven Dependencies in Mongo")
  @PostMapping(value = "/update", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> updateMavenDependenciesInMongo() {
    CompletableFuture.runAsync(mavenRepoService::updateDependenciesInMongo);
    return ResponseEntity.accepted().body("{\"request\": \"submitted\"}");
  }
}
