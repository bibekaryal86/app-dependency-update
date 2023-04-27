package app.dependency.update.app.config;

import app.dependency.update.app.util.InterceptorUtilsLoggingRestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Configuration
public class RestTemplateConfig {

  @Bean
  public RestTemplate restTemplate() {
    RestTemplate restTemplate =
        new RestTemplate(new BufferingClientHttpRequestFactory(clientHttpRequestFactory()));
    restTemplate.getInterceptors().add(new InterceptorUtilsLoggingRestTemplate());
    return restTemplate;
  }

  private ClientHttpRequestFactory clientHttpRequestFactory() {
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    SocketConfig socketConfig =
        SocketConfig.copy(SocketConfig.DEFAULT).setSoTimeout(Timeout.ofSeconds(15)).build();
    HttpClientConnectionManager httpClientConnectionManager =
        PoolingHttpClientConnectionManagerBuilder.create()
            .setDefaultSocketConfig(socketConfig)
            .build();
    HttpClient httpClient =
        HttpClientBuilder.create().setConnectionManager(httpClientConnectionManager).build();
    factory.setHttpClient(httpClient);
    factory.setConnectTimeout(15000);
    return factory;
  }
}
