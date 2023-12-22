package app.dependency.update.app.exception;

import java.io.IOException;

public class AppDependencyUpdateIOException extends IOException {
  public AppDependencyUpdateIOException(final String message, final Throwable ex) {
    super(message, ex);
  }
}
