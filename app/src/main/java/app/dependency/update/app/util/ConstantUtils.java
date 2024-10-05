package app.dependency.update.app.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConstantUtils {

  // provided at runtime
  public static final String SERVER_PORT = "PORT";
  public static final String ENV_REPO_NAME = "repo_home";
  public static final String ENV_MONGO_USERNAME = "mongo_user";
  public static final String ENV_MONGO_PASSWORD = "mongo_pwd";
  public static final String ENV_SEND_EMAIL = "send_email";
  public static final String ENV_MAILJET_PUBLIC_KEY = "mj_public";
  public static final String ENV_MAILJET_PRIVATE_KEY = "mj_private";
  public static final String ENV_MAILJET_EMAIL_ADDRESS = "mj_email";

  // others
  public static final String PATH_DELIMITER = "/";
  public static final String GRADLE_WRAPPER_REGEX = "(?<=\\-)(.*?)(?=\\-)";
  public static final String GRADLE_BUILD_BLOCK_END_REGEX = "([a-z]+\\s\\{)";
  public static final String GRADLE_BUILD_DEPENDENCIES_REGEX = "(?<=\\%s)(.*?)(?=\\%s)";
  public static final String GRADLE_BUILD_DEFINITION_REGEX = "\\w+\\s+\\w+";
  public static final String PYTHON_PYPROJECT_TOML_BUILDTOOLS_REGEX = "'(.*?)'";
  public static final String COMMAND_PATH = PATH_DELIMITER + "bin" + PATH_DELIMITER + "bash";
  public static final String COMMAND_WINDOWS = "cmd.exe";
  public static final String SCRIPTS_DIRECTORY = "scripts";
  public static final String CHMOD_COMMAND = "chmod +x ";
  public static final String JAVA_SYSTEM_TMPDIR = System.getProperty("java.io.tmpdir");
  public static final String APP_MAIN_MODULE = "app";
  public static final String GRADLE_WRAPPER_PROPERTIES =
      "/gradle/wrapper/gradle-wrapper.properties";
  public static final String BUILD_GRADLE = "build.gradle";
  public static final String PYPROJECT_TOML = "pyproject.toml";
  public static final String MONGODB_DATABASE_NAME = "repository";
  public static final String MONGODB_COLLECTION_DEPENDENCIES = "dependencies";
  public static final String MONGODB_COLLECTION_PLUGINS = "plugins";
  public static final String MONGODB_COLLECTION_PACKAGES = "packages";
  public static final String MONGODB_COLLECTION_NPMSKIPS = "npm_skips";
  public static final String MONGODB_COLLECTION_PROCESS_SUMMARIES = "process_summaries";
  public static final String BRANCH_UPDATE_DEPENDENCIES = "update_dependencies_%s";

  // endpoints
  public static final String MONGODB_CONNECTION_STRING =
      "mongodb+srv://%s:%s@cluster0.anwaeio.mongodb.net/?retryWrites=true&w=majority";
  public static final String JAVA_RELEASES_ENDPOINT =
      "https://api.adoptium.net/v3/info/release_versions?heap_size=normal&image_type=jdk&lts=true&page=0&page_size=50&project=jdk&release_type=ga&semver=false&sort_method=DEFAULT&sort_order=DESC&vendor=eclipse";
  public static final String PYTHON_RELEASES_ENDPOINT =
      "https://api.github.com/repos/python/cpython/tags";
  public static final String NODE_RELEASES_ENDPOINT = "https://nodejs.org/dist/index.json";
  public static final String GRADLE_RELEASES_ENDPOINT =
      "https://api.github.com/repos/gradle/gradle/releases";
  public static final String GRADLE_PLUGINS_ENDPOINT = "https://plugins.gradle.org/plugin/%s";
  public static final String MAVEN_SEARCH_ENDPOINT =
      "https://search.maven.org/solrsearch/select?core=gav&rows=5&wt=json&q=g:%s+AND+a:%s";
  public static final String PYPI_SEARCH_ENDPOINT = "https://pypi.org/pypi/%s/json";
}
