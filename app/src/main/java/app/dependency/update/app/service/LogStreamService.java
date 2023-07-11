package app.dependency.update.app.service;

import static app.dependency.update.app.util.CommonUtils.getSystemEnvProperty;
import static app.dependency.update.app.util.ConstantUtils.PARAM_REPO_HOME;

import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import app.dependency.update.app.util.ConstantUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LogStreamService {

  private final String logHome;

  public LogStreamService() {
    this.logHome = getSystemEnvProperty(PARAM_REPO_HOME, "").concat("/logs/app-dependency-update");
  }

  public List<String> getLogFileNames() {
    try (Stream<Path> pathStream = Files.walk(Paths.get(this.logHome))) {
      List<String> logFileNames =
          new ArrayList<>(
              pathStream.filter(Files::isRegularFile).toList().stream()
                  .map(path -> path.getFileName().toString())
                  .filter(path -> !path.equals("app-dependency-update.log"))
                  .sorted(Comparator.reverseOrder())
                  .toList());
      logFileNames.add(0, "app-dependency-update.log");
      return logFileNames;
    } catch (Exception ex) {
      throw new AppDependencyUpdateRuntimeException(
          "Logs not found in the repo path provided!", ex);
    }
  }

  public String getLogFileContent(final String fileName) throws IOException {
    Path path = Path.of(this.logHome + ConstantUtils.PATH_DELIMITER + fileName);
    return Files.readString(path);
  }
}
