package update.dependencies.app.exception;

public class UpdateDependenciesRuntimeException extends RuntimeException {
  public UpdateDependenciesRuntimeException(String message) {
    super(message);
  }

  public UpdateDependenciesRuntimeException(String message, Throwable ex) {
    super(message, ex);
  }
}
