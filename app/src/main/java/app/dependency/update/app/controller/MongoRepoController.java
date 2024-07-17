package app.dependency.update.app.controller;

import static app.dependency.update.app.util.CommonUtils.*;

import app.dependency.update.app.model.MongoDependencies;
import app.dependency.update.app.model.MongoNpmSkips;
import app.dependency.update.app.model.MongoPackages;
import app.dependency.update.app.model.MongoPlugins;
import app.dependency.update.app.model.dto.Dependencies;
import app.dependency.update.app.model.dto.NpmSkips;
import app.dependency.update.app.model.dto.Packages;
import app.dependency.update.app.model.dto.Plugins;
import app.dependency.update.app.service.MongoRepoService;
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
@RequestMapping("/mongo-repo")
public class MongoRepoController {

  private final MongoRepoService mongoRepoService;
  private static final String SAVE_SUCCESSFUL = "{\"save\": \"successful\"}";
  private static final String SAVE_UNSUCCESSFUL = "{\"save\": \"unsuccessful\", \"message\": \"";
  private static final String SAVE_UNSUCCESSFUL_MISSING_INPUT =
      "{\"save\": \"unsuccessful\", \"message\": \"Missing Input\"}";

  public MongoRepoController(final MongoRepoService mongoRepoService) {
    this.mongoRepoService = mongoRepoService;
  }

  @Operation(summary = "Get Plugins in Mongo Repository")
  @GetMapping(value = "/plugins", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<MongoPlugins>> getPlugins() {
    List<Plugins> plugins = mongoRepoService.pluginsMap().values().stream().toList();
    List<MongoPlugins> mongoPlugins =
        plugins.stream()
            .map(
                plugin ->
                    MongoPlugins.builder()
                        .group(plugin.getGroup())
                        .version(plugin.getVersion())
                        .skipVersion(plugin.isSkipVersion())
                        .build())
            .toList();
    return ResponseEntity.ok(mongoPlugins);
  }

  @Operation(summary = "Save Plugin in Mongo Repository")
  @PostMapping(value = "/plugins", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> savePlugin(@RequestBody final MongoPlugins mongoPlugins) {
    if (mongoPlugins == null
        || isEmpty(mongoPlugins.getGroup())
        || isEmpty(mongoPlugins.getVersion())) {
      return ResponseEntity.badRequest().body(SAVE_UNSUCCESSFUL_MISSING_INPUT);
    }

    try {
      Plugins plugin = mongoRepoService.pluginsMap().get(mongoPlugins.getGroup());
      if (plugin == null) {
        plugin = Plugins.builder().build();
      }
      BeanUtils.copyProperties(mongoPlugins, plugin);
      mongoRepoService.savePlugin(plugin);
      return ResponseEntity.ok(SAVE_SUCCESSFUL);
    } catch (Exception ex) {
      return ResponseEntity.status(500)
          .body(SAVE_UNSUCCESSFUL + ex.getMessage().replace("\"", "'") + "\"}");
    }
  }

  @Operation(summary = "Get Dependencies in Mongo Repository")
  @GetMapping(value = "/dependencies", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<MongoDependencies>> getDependencies(
      @RequestParam(required = false) final String mavenId) {
    List<Dependencies> dependencies = mongoRepoService.dependenciesMap().values().stream().toList();
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
      return ResponseEntity.badRequest().body(SAVE_UNSUCCESSFUL);
    }

    try {
      Dependencies dependency =
          mongoRepoService.dependenciesMap().get(mongoDependencies.getMavenId());
      if (dependency == null) {
        dependency = Dependencies.builder().build();
      }
      BeanUtils.copyProperties(mongoDependencies, dependency);
      mongoRepoService.saveDependency(dependency);
      return ResponseEntity.ok(SAVE_SUCCESSFUL);
    } catch (Exception ex) {
      return ResponseEntity.status(500)
          .body(SAVE_UNSUCCESSFUL + ex.getMessage().replace("\"", "'") + "\"}");
    }
  }

  @Operation(summary = "Get Python Packages in Mongo Repository")
  @GetMapping(value = "/packages", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<MongoPackages>> getPackages() {
    List<Packages> packages = mongoRepoService.packagesMap().values().stream().toList();
    List<MongoPackages> mongoPackages =
        packages.stream()
            .map(
                onePackage ->
                    MongoPackages.builder()
                        .name(onePackage.getName())
                        .version(onePackage.getVersion())
                        .skipVersion(onePackage.isSkipVersion())
                        .build())
            .toList();
    return ResponseEntity.ok(mongoPackages);
  }

  @Operation(summary = "Save Python Package in Mongo Repository")
  @PostMapping(value = "/packages", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> savePackage(@RequestBody final MongoPackages mongoPackages) {
    if (mongoPackages == null
        || isEmpty(mongoPackages.getName())
        || isEmpty(mongoPackages.getVersion())) {
      return ResponseEntity.badRequest().body(SAVE_UNSUCCESSFUL_MISSING_INPUT);
    }

    try {
      Packages onePackage = mongoRepoService.packagesMap().get(mongoPackages.getName());
      if (onePackage == null) {
        onePackage = Packages.builder().build();
      }
      BeanUtils.copyProperties(mongoPackages, onePackage);
      mongoRepoService.savePackage(onePackage);
      return ResponseEntity.ok(SAVE_SUCCESSFUL);
    } catch (Exception ex) {
      return ResponseEntity.status(500)
          .body(SAVE_UNSUCCESSFUL + ex.getMessage().replace("\"", "'") + "\"}");
    }
  }

  @Operation(summary = "Get Skipped NPM Dependencies in Mongo Repository")
  @GetMapping(value = "/npm_skips", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<MongoNpmSkips>> getNpmSkips() {
    List<NpmSkips> npmSkips = mongoRepoService.npmSkipsMap().values().stream().toList();
    List<MongoNpmSkips> mongoNpmSkips =
            npmSkips.stream()
                    .map(
                            npmSkip ->
                                    MongoNpmSkips.builder()
                                            .name(npmSkip.getName())
                                            .version(npmSkip.getVersion())
                                            .isActive(npmSkip.isActive())
                                            .build())
                    .toList();
    return ResponseEntity.ok(mongoNpmSkips);
  }

  @Operation(summary = "Save NPM Dependencies to Skip in Mongo Repository")
  @PostMapping(value = "/npm_skips", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> saveNpmSkip(@RequestBody final MongoNpmSkips mongoNpmSkips) {
    if (mongoNpmSkips == null
            || isEmpty(mongoNpmSkips.getName())
            || isEmpty(mongoNpmSkips.getVersion())) {
      return ResponseEntity.badRequest().body(SAVE_UNSUCCESSFUL_MISSING_INPUT);
    }

    try {
      NpmSkips npmSkip = mongoRepoService.npmSkipsMap().get(mongoNpmSkips.getName());
      if (npmSkip == null) {
        npmSkip = NpmSkips.builder().build();
      }
      BeanUtils.copyProperties(mongoNpmSkips, npmSkip);
      mongoRepoService.saveNpmSkip(npmSkip);
      return ResponseEntity.ok(SAVE_SUCCESSFUL);
    } catch (Exception ex) {
      return ResponseEntity.status(500)
              .body(SAVE_UNSUCCESSFUL + ex.getMessage().replace("\"", "'") + "\"}");
    }
  }

  @Operation(summary = "On-demand Update Dependencies and Plugins Repo in Mongo")
  @PostMapping(value = "/update", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> updateMavenRepoInMongo() {
    CompletableFuture.runAsync(() -> mongoRepoService.updateDependenciesInMongo(mongoRepoService.dependenciesMap()));
    CompletableFuture.runAsync(() -> mongoRepoService.updatePluginsInMongo(mongoRepoService.pluginsMap()));
    CompletableFuture.runAsync(() -> mongoRepoService.updatePackagesInMongo(mongoRepoService.packagesMap()));
    return ResponseEntity.accepted().body("{\"request\": \"submitted\"}");
  }
}
