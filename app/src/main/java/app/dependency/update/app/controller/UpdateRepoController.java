package app.dependency.update.app.controller;

import static app.dependency.update.app.util.CommonUtils.*;
import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.service.UpdateRepoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/update-repo")
public class UpdateRepoController {

  private final UpdateRepoService updateRepoService;

  public UpdateRepoController(final UpdateRepoService updateRepoService) {
    this.updateRepoService = updateRepoService;
  }

  @Operation(summary = "On-demand Update Repos")
  @PostMapping(value = "/{updateType}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> updateRepo(
      @PathVariable final UpdateType updateType,
      @Parameter(in = ParameterIn.QUERY, description = "Recreate Caches")
          @RequestParam(required = false, defaultValue = "false")
          final boolean isRecreateCaches,
      @Parameter(in = ParameterIn.QUERY, description = "Recreate Script Files")
          @RequestParam(required = false, defaultValue = "false")
          final boolean isRecreateScriptFiles,
      @Parameter(in = ParameterIn.QUERY, description = "Create PR for a Branch")
          @RequestParam(required = false, defaultValue = "false")
          final boolean isForceCreatePr,
      @Parameter(
              in = ParameterIn.QUERY,
              description = "For Delete Branches, Update Dependencies only")
          @RequestParam(required = false, defaultValue = "true")
          final boolean isDeleteUpdateDependenciesOnly,
      @Parameter(in = ParameterIn.QUERY, description = "For NPM Snapshots, YYYY-MM-DD")
          @RequestParam(required = false)
          final String branchDate,
      @Parameter(in = ParameterIn.QUERY, description = "For Gradle Spotless Apply")
          @RequestParam(required = false)
          final String repoName) {
    if (updateRepoService.isTaskRunning()) {
      return ResponseEntity.unprocessableEntity().body("{\"process\": \"already running\"}");
    } else {
      if (checkInvalidBranchDate(branchDate, updateType)) {
        return ResponseEntity.badRequest().body("{\"branchDate\": \"empty or invalid format\"}");
      }

      String branchName = String.format(BRANCH_UPDATE_DEPENDENCIES, branchDate);
      updateRepoService.updateRepos(
          isRecreateCaches,
          isRecreateScriptFiles,
          branchName,
          repoName,
          updateType,
          isForceCreatePr,
          isDeleteUpdateDependenciesOnly,
          checkDependenciesUpdate(updateType));
    }
    return ResponseEntity.accepted().body("{\"request\": \"submitted\"}");
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  private boolean checkInvalidBranchDate(final String branchDate, final UpdateType updateType) {
    if (updateType.equals(UpdateType.NPM_SNAPSHOT)
        || updateType.equals(UpdateType.GITHUB_PR_CREATE)
        || updateType.equals(UpdateType.GRADLE_SPOTLESS)) {
      try {
        if (isEmpty(branchDate)) {
          return true;
        }
        LocalDate.parse(branchDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return false;
      } catch (DateTimeParseException e) {
        return true;
      }
    } else {
      return false;
    }
  }
}
