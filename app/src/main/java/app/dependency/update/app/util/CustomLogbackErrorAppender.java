package app.dependency.update.app.util;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

public class CustomLogbackErrorAppender extends AppenderBase<ILoggingEvent> {

  @Override
  protected void append(ILoggingEvent eventObject) {
    if (eventObject.getLevel().toString().equals("ERROR")) {
      ProcessUtils.setErrorsOrExceptions(true);
    }
  }
}
