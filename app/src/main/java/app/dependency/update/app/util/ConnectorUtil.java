package app.dependency.update.app.util;

import static app.dependency.update.app.util.CommonUtil.getGson;

import app.dependency.update.app.exception.AppDependencyUpdateRuntimeException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
      String endpoint, CommonUtil.HttpMethod httpMethod, Object bodyObject, Map<String, String> headers) {
    HttpRequest.Builder httpRequestBuilder =
        HttpRequest.newBuilder().uri(getUri(endpoint)).header("Content-Type", "application/json");

    if (headers != null && !headers.isEmpty()) {
      for (Map.Entry<String, String> entry : headers.entrySet()) {
        httpRequestBuilder = httpRequestBuilder.header(entry.getKey(), entry.getValue());
      }
    }

    if (httpMethod == CommonUtil.HttpMethod.POST) {
      httpRequestBuilder = httpRequestBuilder.POST(getPOST(bodyObject));
    } else if (httpMethod == CommonUtil.HttpMethod.PUT) {
      httpRequestBuilder = httpRequestBuilder.PUT(getPOST(bodyObject));
    } else if (httpMethod == CommonUtil.HttpMethod.DELETE) {
      httpRequestBuilder = httpRequestBuilder.DELETE();
    } else if (httpMethod == CommonUtil.HttpMethod.GET) {
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
      CommonUtil.HttpMethod httpMethod,
      Object bodyObject,
      Map<String, String> headers,
      Type type,
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

      if (type == null) {
        return getGson().fromJson(httpResponse.body(), clazz);
      } else {
        // if response is an array
        return getGson().fromJson(httpResponse.body(), type);
      }
    } catch (InterruptedException ex) {
      log.error("Error in HttpClient Send: [ {} ] | [ {} ]", endpoint, httpMethod, ex);
      Thread.currentThread().interrupt();
    } catch (Exception ex) {
      log.error("Error in HttpClient Send: [ {} ] | [ {} ] ", endpoint, httpMethod, ex);
    }

    throw new AppDependencyUpdateRuntimeException("Send Http Request Error");
  }
}
