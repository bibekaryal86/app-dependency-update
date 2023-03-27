package app.dependency.update.app.exception;

public class AppDependencyUpdateRuntimeException extends RuntimeException {
  public AppDependencyUpdateRuntimeException(final String message) {
    super(message);
  }

  public AppDependencyUpdateRuntimeException(final String message, final Throwable ex) {
    super(message, ex);
  }
}
