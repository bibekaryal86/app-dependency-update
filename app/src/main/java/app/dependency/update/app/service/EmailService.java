package app.dependency.update.app.service;

import static app.dependency.update.app.util.CommonUtils.getSystemEnvProperty;
import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.util.ConstantUtils;
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

  private final String logHome;
  private final String emailSenderEmail;
  private final MailjetClient mailjetClient;

  public EmailService() {
    this.logHome = getSystemEnvProperty(PARAM_REPO_HOME, "").concat("/logs/app-dependency-update");
    this.emailSenderEmail = getSystemEnvProperty(ENV_MJ_EMAIL_ADDRESS, "");
    this.mailjetClient =
        new MailjetClient(
            ClientOptions.builder()
                .apiKey(getSystemEnvProperty(ENV_MJ_APIKEY_PUBLIC, ""))
                .apiSecretKey(getSystemEnvProperty(ENV_MJ_APIKEY_PRIVATE, ""))
                .build());
  }

  private void sendEmail(String text) {
    log.info("Sending Email...");

    try {
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

      MailjetResponse response = this.mailjetClient.post(request);

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
    Path path = Path.of(this.logHome + ConstantUtils.PATH_DELIMITER + "app-dependency-update.log");
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
