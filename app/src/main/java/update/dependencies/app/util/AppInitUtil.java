package update.dependencies.app.util;

import static update.dependencies.app.util.Util.PARAM_REPO_HOME;
import static update.dependencies.app.util.Util.SCRIPTS_FOLDER;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import update.dependencies.App;
import update.dependencies.app.exception.UpdateDependenciesRuntimeException;
import update.dependencies.app.model.ScriptFile;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AppInitUtil {

  public static Map<String, String> makeArgsMap(String[] args) {
    log.info("Args Before Conversion: {}", Arrays.asList(args));
    Map<String, String> map = new HashMap<>();

    for (String arg : args) {
      try {
        String[] argArray = arg.split("=");
        map.put(argArray[0], argArray[1]);
      } catch (Exception ex) {
        throw new UpdateDependenciesRuntimeException("Invalid Param: " + arg, ex);
      }
    }

    if (map.get(PARAM_REPO_HOME) == null) {
      throw new UpdateDependenciesRuntimeException("repo_home parameter must be provided");
    }

    log.info("Args Map After Conversion: {}", map);
    return map;
  }

  public static List<ScriptFile> getScriptsInResources() {
    List<ScriptFile> scriptFiles;

    try {
      // get path of the current running JAR
      String jarPath =
          App.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();

      // file walks JAR
      URI uri = URI.create("jar:file:" + jarPath);
      try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
        try (Stream<Path> streamPath = Files.walk(fs.getPath(SCRIPTS_FOLDER), 1)) {
          scriptFiles =
              streamPath
                  .filter(Files::isRegularFile)
                  .map(path -> new ScriptFile(path.getFileName().toString()))
                  .collect(Collectors.toList());
        }
      }
    } catch (URISyntaxException | IOException ex) {
      throw new UpdateDependenciesRuntimeException("Error reading script files in resources", ex);
    }

    if (scriptFiles.isEmpty()) {
      throw new UpdateDependenciesRuntimeException("Script files not found in resources");
    }

    log.info("Script files: {}", scriptFiles);
    return scriptFiles;
  }
}
