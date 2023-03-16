package app.dependency.update.app.exception;

import java.io.IOException;

public class AppDependencyUpdateIOException extends IOException {
  public AppDependencyUpdateIOException(String message) {
    super(message);
  }

  public AppDependencyUpdateIOException(String message, Throwable ex) {
    super(message, ex);
  }
}
