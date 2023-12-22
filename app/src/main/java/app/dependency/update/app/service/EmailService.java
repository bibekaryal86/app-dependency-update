package app.dependency.update.app.service;

import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.util.AppInitDataUtils;
import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.resource.Emailv31;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

  private MailjetClient mailjetClient() {
    return new MailjetClient(
        ClientOptions.builder()
            .apiKey(AppInitDataUtils.appInitData().getArgsMap().get(ENV_MAILJET_PUBLIC_KEY))
            .apiSecretKey(AppInitDataUtils.appInitData().getArgsMap().get(ENV_MAILJET_PRIVATE_KEY))
            .build());
  }

  private void sendEmail(final String text) {
    log.info("Sending Email...");

    try {
      String emailSenderEmail =
          AppInitDataUtils.appInitData().getArgsMap().get(ENV_MAILJET_EMAIL_ADDRESS);

      MailjetRequest request =
          new MailjetRequest(Emailv31.resource)
              .property(
                  Emailv31.MESSAGES,
                  new JSONArray()
                      .put(
                          new JSONObject()
                              .put(
                                  Emailv31.Message.FROM,
                                  new JSONObject()
                                      .put("Email", emailSenderEmail)
                                      .put("Name", "MailJet--" + emailSenderEmail))
                              .put(
                                  Emailv31.Message.TO,
                                  new JSONArray()
                                      .put(
                                          new JSONObject()
                                              .put("Email", emailSenderEmail)
                                              .put("Name", "MailJet--" + emailSenderEmail)))
                              .put(
                                  Emailv31.Message.SUBJECT,
                                  text.contains("ERROR")
                                      ? "**ERROR** App Dependency Update Logs"
                                      : "App Dependency Update Logs")
                              .put(Emailv31.Message.TEXTPART, text)
                              .put(Emailv31.Message.CUSTOMID, UUID.randomUUID().toString())));

      MailjetResponse response = mailjetClient().post(request);

      if (response.getStatus() == 200) {
        log.info("Send Email Response Success...");
      } else {
        log.info("Send Email Response Failure:  [ {} ]", response.getData());
      }
    } catch (Exception ex) {
      log.error("Send Email Error...", ex);
    }
  }

  private String getLogFileContent() throws IOException {
    String logHome =
        AppInitDataUtils.appInitData()
            .getArgsMap()
            .get(ENV_REPO_NAME)
            .concat("/logs/app-dependency-update");
    Path path = Path.of(logHome + PATH_DELIMITER + "app-dependency-update.log");
    return Files.readString(path);
  }

  public void sendLogEmail() {
    try {
      sendEmail(getLogFileContent());
    } catch (Exception ex) {
      log.error("Send Log Email Error...", ex);
    }
  }
}
