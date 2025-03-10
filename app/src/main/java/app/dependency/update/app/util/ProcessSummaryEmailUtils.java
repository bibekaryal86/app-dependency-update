package app.dependency.update.app.util;

import app.dependency.update.app.model.ProcessSummary;
import app.dependency.update.app.model.ProcessedRepository;
import java.util.Comparator;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProcessSummaryEmailUtils {

  public static synchronized String getProcessSummaryContent(ProcessSummary processSummary) {
    List<ProcessedRepository> allProcessedRepositories = processSummary.getProcessedRepositories();
    List<ProcessedRepository> prCreatedAndMerged =
        processSummary.getProcessedRepositories().stream()
            .filter(
                processedRepository ->
                    processedRepository.isPrCreated() && processedRepository.isPrMerged())
            .sorted(Comparator.comparing(ProcessedRepository::getRepoName))
            .toList();
    List<ProcessedRepository> prCreatedNotMerged =
        processSummary.getProcessedRepositories().stream()
            .filter(
                processedRepository ->
                    processedRepository.isPrCreated() && !processedRepository.isPrMerged())
            .sorted(Comparator.comparing(ProcessedRepository::getRepoName))
            .toList();
    List<ProcessedRepository> prCreateError =
        processSummary.getProcessedRepositories().stream()
            .filter(ProcessedRepository::isPrCreateError)
            .sorted(Comparator.comparing(ProcessedRepository::getRepoName))
            .toList();

    StringBuilder html = new StringBuilder();
    html.append(
        """
              <html>
                <head>
                  <style>
                    th {
                        border-bottom: 2px solid #9e9e9e;
                        position: sticky;
                        top: 0;
                        background-color: lightgrey;
                      }
                    td {
                      padding: 5px;
                      text-align: left;
                      border-bottom: 1px solid #9e9e9e;
                    }
                    td:first-child {
                       text-align: left;
                     }
                     td:not(:first-child) {
                       text-align: center;
                     }
                  </style>
                </head>
                <body>
              """);

    html.append(
        """
              <p style='font-size: 14px; font-weight: bold;'>App Dependency Update Process Summary: %s</p>
              <table cellpadding='10' cellspacing='0' style='font-size: 12px; border-collapse: collapse;'>
                <tr>
                  <th>Item</th>
                  <th>Value</th>
                </tr>
                <tr>
                  <td>Mongo Plugins To Update</td>
                  <td>%d</td>
                </tr>
                <tr>
                  <td>Mongo Dependencies To Update</td>
                  <td>%d</td>
                </tr>
                <tr>
                  <td>Mongo Packages To Update</td>
                  <td>%d</td>
                </tr>
                <tr>
                  <td>Mongo NPM Skips Active</td>
                  <td>%d</td>
                </tr>
                <tr>
                  <td>Total PR Created Count</td>
                  <td>%d</td>
                </tr>
                <tr>
                  <td>Total PR Create Errors Count</td>
                  <td>%d</td>
                </tr>
                <tr>
                  <td>Total PR Merged Count</td>
                  <td>%d</td>
                </tr>
              </table>
            """
            .formatted(
                processSummary.getUpdateType(),
                processSummary.getMongoPluginsToUpdate(),
                processSummary.getMongoDependenciesToUpdate(),
                processSummary.getMongoPackagesToUpdate(),
                processSummary.getMongoNpmSkipsActive(),
                processSummary.getTotalPrCreatedCount(),
                processSummary.getTotalPrCreateErrorsCount(),
                processSummary.getTotalPrMergedCount()));

    html.append(
        """
                <br />
                <p style='font-size: 14px; font-weight: bold;%s'>Exception Caught in the Update Process: %s</p>
                <br />
              """
            .formatted(
                processSummary.isErrorsOrExceptions() ? " color: red;" : "",
                processSummary.isErrorsOrExceptions()));

    if (prCreatedAndMerged.isEmpty()) {
      html.append(
          """
                <br />
                <p style='font-size: 14px; font-weight: bold;'>Repositories with PR Created and Merged: N/A</p>
                <br />
              """);
    } else {
      html.append(
          """
                <br />
                <p style='font-size: 14px; font-weight: bold;'>Repositories with PR Created and Merged</p>
                <table border='1' cellpadding='10' cellspacing='0' style='border-collapse: collapse; width: 100%;'>
                  <tr>
                    <th>Repository</th>
                    <th>Type</th>
                    <th>PR Created</th>
                    <th>PR Create Error</th>
                    <th>PR Merged</th>
                  </tr>
              """);

      processedRepositoryTable(prCreatedAndMerged, html);
    }

    if (prCreatedNotMerged.isEmpty()) {
      html.append(
          """
                <br />
                <p style='font-size: 14px; font-weight: bold;'>Repositories with PR Created but NOT Merged: N/A</p>
                <br />
              """);
    } else {
      html.append(
          """
                <br />
                <p style='font-size: 14px; font-weight: bold;'>Repositories with PR Created but NOT Merged</p>
                <table border='1' cellpadding='10' cellspacing='0' style='border-collapse: collapse; width: 100%;'>
                  <tr>
                    <th>Repository</th>
                    <th>Type</th>
                    <th>PR Created</th>
                    <th>PR Create Error</th>
                    <th>PR Merged</th>
                  </tr>
              """);

      processedRepositoryTable(prCreatedNotMerged, html);
    }

    if (prCreateError.isEmpty()) {
      html.append(
          """
                <br />
                <p style='font-size: 14px; font-weight: bold;'>Repositories with PR Creation Error: N/A</p>
                <br />
              """);
    } else {
      html.append(
          """
                <br />
                <p style='font-size: 14px; font-weight: bold;'>Repositories with PR Creation Error</p>
                <table border='1' cellpadding='10' cellspacing='0' style='border-collapse: collapse; width: 100%;'>
                  <tr>
                    <th>Repository</th>
                    <th>Type</th>
                    <th>PR Created</th>
                    <th>PR Create Error</th>
                    <th>PR Merged</th>
                  </tr>
              """);

      processedRepositoryTable(prCreateError, html);
    }

    html.append(
        """
              <br />
              <p style='font-size: 14px; font-weight: bold;'>All Repositories</p>
              <table border='1' cellpadding='10' cellspacing='0' style='border-collapse: collapse; width: 100%;'>
                <tr>
                  <th>Repository</th>
                  <th>Type</th>
                  <th>PR Created</th>
                  <th>PR Create Error</th>
                  <th>PR Merged</th>
                </tr>
            """);

    processedRepositoryTable(allProcessedRepositories, html);

    html.append(
        """
          </body>
        </html>
        """);

    return html.toString();
  }

  private static void processedRepositoryTable(
      List<ProcessedRepository> processedRepositories, StringBuilder html) {
    for (ProcessedRepository processedRepository : processedRepositories) {
      html.append("<tr>");
      html.append("<td>").append(processedRepository.getRepoName()).append("</td>");
      html.append("<td>").append(processedRepository.getRepoType()).append("</td>");
      html.append("<td>").append(processedRepository.isPrCreated() ? "Y" : "N").append("</td>");
      html.append("<td>").append(processedRepository.isPrCreateError() ? "Y" : "N").append("</td>");
      html.append("<td>").append(processedRepository.isPrMerged() ? "Y" : "N").append("</td>");
      html.append("</tr>");
    }

    html.append(
        """
        </table>
      """);
  }
}
