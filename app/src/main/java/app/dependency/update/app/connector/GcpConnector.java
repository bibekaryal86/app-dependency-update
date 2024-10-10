package app.dependency.update.app.connector;

import static app.dependency.update.app.util.ConstantUtils.GCP_RUNTIME_SUPPORT_ENDPOINT;

import app.dependency.update.app.util.ProcessUtils;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GcpConnector {

  public Map<String, String> getLatestGcpRuntimes() {
    List<String> validKeys = List.of("java", "nodejs", "python");
    Map<String, String> latestRuntimesMap = new HashMap<>();
    final Document document = getGcpRuntimeSupportSchedule();

    if (document == null) {
      ProcessUtils.setExceptionCaught(true);
      log.error("GCP Runtimes lookup Document is null...");
      return latestRuntimesMap;
    }

    Elements tables = document.select("table");
    for (Element table : tables) {
      latestRuntimesMap.putAll(parseRuntimeTables(table));
    }

    if (latestRuntimesMap.isEmpty()) {
      ProcessUtils.setExceptionCaught(true);
      log.error("Latest GCP Runtimes Map is Empty...");
    } else {
      log.info("Latest GCP Runtimes Map: {}]", latestRuntimesMap);
      return latestRuntimesMap.entrySet().stream()
          .filter(entry -> validKeys.contains(entry.getKey()))
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    return latestRuntimesMap;
  }

  private Document getGcpRuntimeSupportSchedule() {
    try {
      return Jsoup.connect(GCP_RUNTIME_SUPPORT_ENDPOINT).get();
    } catch (IOException ex) {
      ProcessUtils.setExceptionCaught(true);
      log.error("ERROR Get GCP Runtime Support Schedule...", ex);
    }
    return null;
  }

  private Map<String, String> parseRuntimeTables(final Element table) {
    Element headerRow = table.selectFirst("thead tr");

    if (headerRow == null) {
      ProcessUtils.setExceptionCaught(true);
      log.error("GCP Runtime Tables, HeaderRow Not Found...");
      return Collections.emptyMap();
    }

    Elements headers = headerRow.select("th");
    int runtimeIdIndex = getRuntimeIdIndex(headers);

    if (runtimeIdIndex == -1) {
      ProcessUtils.setExceptionCaught(true);
      log.error("GCP Runtime Tables, Runtime ID Header Not Found...");
      return Collections.emptyMap();
    }

    Elements rows = table.select("tbody tr");

    for (Element row : rows) {
      Element runtimeIdCell = row.select("td").get(runtimeIdIndex);
      Element codeElement = runtimeIdCell.selectFirst("code");

      if (codeElement == null) {
        ProcessUtils.setExceptionCaught(true);
        log.error("GCP Runtime Tables, Code Element Not Found...");
        return Collections.emptyMap();
      }

      String runtimeId = codeElement.text();
      return parseRuntimeId(runtimeId);
    }

    return Collections.emptyMap();
  }

  private int getRuntimeIdIndex(final Elements headers) {
    int runtimeIdIndex = -1;
    for (int i = 0; i < headers.size(); i++) {
      if (headers.get(i).text().equalsIgnoreCase("Runtime ID")) {
        runtimeIdIndex = i;
        break;
      }
    }
    return runtimeIdIndex;
  }

  private Map<String, String> parseRuntimeId(final String runtimeId) {
    String alphaPart = runtimeId.replaceAll("[^A-Za-z]", "");
    String numericPart = runtimeId.replaceAll("[^0-9]", "");
    return Map.of(alphaPart, numericPart);
  }
}
