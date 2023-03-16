package update.dependencies.app.exception;

import java.io.IOException;

public class UpdateDependenciesIOException extends IOException {
  public UpdateDependenciesIOException(String message) {
    super(message);
  }

  public UpdateDependenciesIOException(String message, Throwable ex) {
    super(message, ex);
  }
}
