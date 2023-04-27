package app.dependency.update.app.controller;

import static app.dependency.update.app.util.CommonUtils.*;
import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.service.ScriptFilesService;
import app.dependency.update.app.service.UpdateRepoService;
import app.dependency.update.app.service.UpdateRepoServiceOnDemand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/update-repo")
public class UpdateRepoController {

  private final UpdateRepoService updateRepoService;
  private final UpdateRepoServiceOnDemand updateRepoServiceOnDemand;
  private final ScriptFilesService scriptFilesService;

  public UpdateRepoController(
      final UpdateRepoService updateRepoService,
      final UpdateRepoServiceOnDemand updateRepoServiceOnDemand,
      final ScriptFilesService scriptFilesService) {
    this.updateRepoService = updateRepoService;
    this.updateRepoServiceOnDemand = updateRepoServiceOnDemand;
    this.scriptFilesService = scriptFilesService;
  }

  @Operation(summary = "On-demand Update Repos")
  @GetMapping(value = "/{updateType}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> updateRepo(
      @PathVariable final UpdateType updateType,
      @RequestParam(defaultValue = "false") final boolean isWrapperMerge,
      @Parameter(in = ParameterIn.QUERY, description = "YYYY-MM-DD") @RequestParam(required = false)
          final String branchDate) {
    if (updateType.equals(UpdateType.ALL)) {
      updateRepoServiceOnDemand.updateRepoOnDemand();
    } else if (updateType.equals(UpdateType.NPM_SNAPSHOT)) {
      if (isInvalidBranchDate(branchDate)) {
        return ResponseEntity.badRequest().body("{\"branchDate\": \"empty or invalid format\"}");
      }
      updateRepoServiceOnDemand.updateNpmSnapshot(
          String.format(BRANCH_UPDATE_DEPENDENCIES, branchDate));
    } else {
      scriptFilesService.deleteTempScriptFiles();
      scriptFilesService.createTempScriptFiles();
      updateRepoService.updateRepos(updateType, isWrapperMerge);
    }
    return ResponseEntity.accepted().body("{\"request\": \"submitted\"}");
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  private boolean isInvalidBranchDate(final String branchDate) {
    try {
      if (isEmpty(branchDate)) {
        return true;
      }

      LocalDate.parse(branchDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
      return false;
    } catch (DateTimeParseException e) {
      return true;
    }
  }
}