package app.dependency.update.app.util;

import app.dependency.update.app.model.MavenSearchResponse;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MavenRepoUtil {
  private static final Map<String, String> mavenVersionMap = new HashMap<>();

  public static Map<String, String> getMavenVersionMap() {
    return mavenVersionMap;
  }

  public static String getLatestMavenVersion(String group, String artifact, String currentVersion) {
    String mavenId = group + ":" + artifact;

    if (getMavenVersionMap().get(mavenId) != null) {
      return getMavenVersionMap().get(mavenId);
    }

    MavenSearchResponse mavenSearchResponse = getMavenSearchResponse(group, artifact);
    log.info("Maven Search Response: [{}:{}], {}", group, artifact, mavenSearchResponse);

    if (mavenSearchResponse != null
        && mavenSearchResponse.getResponse() != null
        && !CommonUtil.isEmpty(mavenSearchResponse.getResponse().getDocs())) {
      // there could be more than one matching response, but that is highly unlikely
      // also highly likely that the build will fair after PR in that scenario
      // so get the first element in the list
      mavenVersionMap.put(
          mavenId, mavenSearchResponse.getResponse().getDocs().get(0).getLatestVersion());
      return mavenSearchResponse.getResponse().getDocs().get(0).getLatestVersion();
    }

    mavenVersionMap.put(mavenId, currentVersion);
    return currentVersion;
  }

  private static MavenSearchResponse getMavenSearchResponse(String group, String artifact) {
    return (MavenSearchResponse)
        ConnectorUtil.sendHttpRequest(
            String.format(CommonUtil.MAVEN_SEARCH_ENDPOINT, group, artifact),
            CommonUtil.HttpMethod.GET,
            null,
            null,
            null,
            MavenSearchResponse.class);
  }
}
