package nospring.service.skeleton.app.util;

import static nospring.service.skeleton.app.util.Util.getGson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nospring.service.skeleton.app.exception.CustomRuntimeException;
import org.eclipse.jetty.http.HttpMethod;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ConnectorUtil {

  private static HttpClient getHttpClient() {
    return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).build();
  }

  private static URI getUri(String endpoint) {
    return URI.create(endpoint);
  }

  private static HttpRequest.BodyPublisher getPOST(Object object) {
    return HttpRequest.BodyPublishers.ofString(getGson().toJson(object));
  }

  private static HttpRequest getHttpRequestBuilder(
      String endpoint, HttpMethod httpMethod, Object bodyObject, Map<String, String> headers) {
    HttpRequest.Builder httpRequestBuilder =
        HttpRequest.newBuilder().uri(getUri(endpoint)).header("Content-Type", "application/json");

    if (headers != null && !headers.isEmpty()) {
      for (Map.Entry<String, String> entry : headers.entrySet()) {
        httpRequestBuilder = httpRequestBuilder.header(entry.getKey(), entry.getValue());
      }
    }

    if (httpMethod == HttpMethod.POST) {
      httpRequestBuilder = httpRequestBuilder.POST(getPOST(bodyObject));
    } else if (httpMethod == HttpMethod.PUT) {
      httpRequestBuilder = httpRequestBuilder.PUT(getPOST(bodyObject));
    } else if (httpMethod == HttpMethod.DELETE) {
      httpRequestBuilder = httpRequestBuilder.DELETE();
    } else if (httpMethod == HttpMethod.GET) {
      httpRequestBuilder = httpRequestBuilder.GET();
    }

    return httpRequestBuilder.build();
  }

  private static HttpResponse<String> sendHttpRequest(HttpRequest httpRequest)
      throws IOException, InterruptedException {
    return getHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());
  }

  public static Object sendHttpRequest(
      String endpoint,
      HttpMethod httpMethod,
      Object bodyObject,
      Map<String, String> headers,
      Class<?> clazz) {
    try {
      log.info(
          "HTTP Request Sent::: Endpoint: [ {} ], Method: [ {} ], Headers: [ {} ], Body: [ {} ]",
          endpoint,
          httpMethod,
          headers == null ? 0 : headers.size(),
          bodyObject == null ? null : bodyObject.getClass().getName());

      HttpRequest httpRequest = getHttpRequestBuilder(endpoint, httpMethod, bodyObject, headers);
      HttpResponse<String> httpResponse = sendHttpRequest(httpRequest);

      log.info(
          "HTTP Response Received::: Endpoint: [ {} ], Status: [ {} ], Body: [ {} ]",
          endpoint,
          httpResponse.statusCode(),
          httpResponse.body() == null ? null : httpResponse.body().length());

      return getGson().fromJson(httpResponse.body(), clazz);
    } catch (InterruptedException ex) {
      log.error("Error in HttpClient Send: [ {} ] | [ {} ]", endpoint, httpMethod, ex);
      Thread.currentThread().interrupt();
    } catch (Exception ex) {
      log.error("Error in HttpClient Send: [ {} ] | [ {} ] ", endpoint, httpMethod, ex);
    }

    throw new CustomRuntimeException("HTTP ERROR");
  }
}
