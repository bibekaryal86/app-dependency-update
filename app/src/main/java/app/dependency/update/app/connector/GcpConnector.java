package app.dependency.update.app.connector;

import static app.dependency.update.app.util.ConstantUtils.GCP_RUNTIME_SUPPORT_ENDPOINT;

import app.dependency.update.app.util.ProcessUtils;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GcpConnector {

  public Document getGcpRuntimeSupportSchedule() {
    try {
      return Jsoup.connect(GCP_RUNTIME_SUPPORT_ENDPOINT).get();
    } catch (IOException ex) {
      ProcessUtils.setErrorsOrExceptions(true);
      log.error("ERROR Get GCP Runtime Support Schedule...", ex);
    }
    return null;
  }
}
