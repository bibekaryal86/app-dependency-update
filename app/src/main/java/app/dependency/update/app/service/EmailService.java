package app.dependency.update.app.service;

import static app.dependency.update.app.util.CommonUtils.isEmpty;
import static app.dependency.update.app.util.ConstantUtils.*;

import app.dependency.update.app.util.AppInitDataUtils;
import app.dependency.update.app.util.ProcessUtils;
import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.resource.Emailv31;
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

  public void sendEmail(
      final String subject,
      final String text,
      final String html,
      final String attachmentFileName,
      final String attachment) {
    log.info("Sending Email...");

    try {
      String emailSenderEmail =
          AppInitDataUtils.appInitData().getArgsMap().get(ENV_MAILJET_EMAIL_ADDRESS);

      JSONObject message =
          new JSONObject()
              .put(Emailv31.Message.CUSTOMID, UUID.randomUUID().toString())
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
              .put(Emailv31.Message.SUBJECT, subject);

      if (!isEmpty(text)) {
        message.put(Emailv31.Message.TEXTPART, text);
      }

      if (!isEmpty(html)) {
        message.put(Emailv31.Message.HTMLPART, html);
      }

      if (!isEmpty(attachmentFileName) && !isEmpty(attachment)) {
        message.put(
            Emailv31.Message.ATTACHMENTS,
            new JSONArray()
                .put(
                    new JSONObject()
                        .put("ContentType", "text/plain")
                        .put("Filename", attachmentFileName)
                        .put("Base64Content", attachment)));
      }

      MailjetRequest request =
          new MailjetRequest(Emailv31.resource)
              .property(Emailv31.MESSAGES, new JSONArray().put(message));

      MailjetResponse response = mailjetClient().post(request);

      if (response.getStatus() == 200) {
        log.info("Send Email Response Success...");
      } else {
        log.info("Send Email Response Failure:  [ {} ]", response.getData());
      }
    } catch (Exception ex) {
      ProcessUtils.setErrorsOrExceptions(true);
      log.error("Send Email Error...", ex);
    }
  }
}
