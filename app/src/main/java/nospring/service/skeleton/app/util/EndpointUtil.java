package nospring.service.skeleton.app.util;

import static nospring.service.skeleton.app.util.Util.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nospring.service.skeleton.app.exception.CustomRuntimeException;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EndpointUtil {

  private static Map<String, String> theEndpointMap = null;

  private static final String ENDPOINT_BASE_DEVELOPMENT = "http://localhost:8080";
  private static final String ENDPOINT_BASE_DOCKER = "http://docker-endpoint-base";
  private static final String ENDPOINT_BASE_PRODUCTION = "https://something.somewhere.com";

  private static final String ENDPOINT_ONE = "/endpoint_one/endpoint/one";
  private static final String ENDPOINT_TWO = "/endpoint_two/endpoint/two";
  private static final String ENDPOINT_THREE = "/endpoint_three/endpoint/three";

  private static Map<String, String> setEndpointMap() {
    Map<String, String> endpointMap = new HashMap<>();
    String profile = getSystemEnvProperty(PROFILE);
    String endpointBase;

    if (!hasText(profile)) {
      throw new CustomRuntimeException("PROFILE NOT SET AT RUNTIME");
    }

    if ("development".equals(profile)) {
      endpointBase = ENDPOINT_BASE_DEVELOPMENT;
    } else if ("docker".equals(profile)) {
      endpointBase = ENDPOINT_BASE_DOCKER;
    } else {
      endpointBase = ENDPOINT_BASE_PRODUCTION;
    }

    endpointMap.put("endpointOne", endpointBase.concat(ENDPOINT_ONE));
    endpointMap.put("endpointTwo", endpointBase.concat(ENDPOINT_TWO));
    endpointMap.put("endpointThree", endpointBase.concat(ENDPOINT_THREE));

    theEndpointMap = new HashMap<>();
    theEndpointMap.putAll(endpointMap);

    return endpointMap;
  }

  public static Map<String, String> endpointMap() {
    return Objects.requireNonNullElseGet(theEndpointMap, EndpointUtil::setEndpointMap);
  }
}
