package app.dependency.update.app.util;

import java.io.IOException;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;

@NoArgsConstructor
public class InterceptorUtilsLoggingRestTemplate implements ClientHttpRequestInterceptor {
  private static final String EXCLUDED = "[EXCLUDED]";
  private final Logger requestLogger =
      LoggerFactory.getLogger("spring.web.client.MessageTracing.sent");
  private final Logger responseLogger =
      LoggerFactory.getLogger("spring.web.client.MessageTracing.received");

  @Override
  public @NonNull ClientHttpResponse intercept(
      @NonNull HttpRequest request, @NonNull byte[] body, ClientHttpRequestExecution execution)
      throws IOException {
    this.logRequest(request, body);
    long startTime = System.currentTimeMillis();

    ClientHttpResponse clientHttpResponse = execution.execute(request, body);

    long endTime = System.currentTimeMillis();
    this.logResponse(request, clientHttpResponse, endTime - startTime);

    return clientHttpResponse;
  }

  private void logRequest(HttpRequest request, byte[] body) {
    StringBuilder stringBuilder =
        new StringBuilder("Sending [")
            .append(request.getMethod())
            .append("] Request [")
            .append(request.getURI())
            .append("]");

    if (this.hasTextBody(request.getHeaders())) {
      stringBuilder.append(" [Headers] ").append(EXCLUDED);
    }

    if (body.length > 0) {
      stringBuilder.append(" [Body] ").append(EXCLUDED);
    }

    String requestLog = stringBuilder.toString();
    this.requestLogger.debug(requestLog);
  }

  private void logResponse(
      HttpRequest request, ClientHttpResponse clientHttpResponse, long durationInMs) {

    try {
      StringBuilder stringBuilder =
          new StringBuilder("Received [")
              .append(clientHttpResponse.getStatusCode().value())
              .append("] Response [")
              .append(request.getURI())
              .append("]");

      HttpHeaders httpHeaders = clientHttpResponse.getHeaders();
      long contentLength = httpHeaders.getContentLength();

      if (contentLength != 0L) {
        if (this.hasTextBody(httpHeaders)) {
          stringBuilder.append(" [Headers] ").append(EXCLUDED);
          stringBuilder.append(" [Body] ").append(EXCLUDED);
        } else {
          stringBuilder
              .append(" [Content Type] [ ")
              .append(httpHeaders.getContentType())
              .append(" ]");
          stringBuilder.append(" [Content Length] [ ").append(contentLength).append(" ]");
        }
      }

      stringBuilder.append(" [After] [ ").append(durationInMs).append(" ms]");
      String responseLog = stringBuilder.toString();
      this.responseLogger.debug(responseLog);
    } catch (IOException ex) {
      this.responseLogger.error(
          "Failed to Log Response for [ {} ] Request to [ {} ]",
          request.getMethod(),
          request.getURI(),
          ex);
    }
  }

  private boolean hasTextBody(HttpHeaders httpHeaders) {
    MediaType mediaType = httpHeaders.getContentType();
    if (mediaType == null) {
      return false;
    } else {
      return "text".equals(mediaType.getType())
          || "xml".equals(mediaType.getSubtype())
          || "json".equals(mediaType.getSubtype());
    }
  }
}
