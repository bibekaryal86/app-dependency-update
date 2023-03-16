package app.dependency.update.app.exception;

public class AppDependencyUpdateRuntimeException extends RuntimeException {
  public AppDependencyUpdateRuntimeException(String message) {
    super(message);
  }

  public AppDependencyUpdateRuntimeException(String message, Throwable ex) {
    super(message, ex);
  }
}
