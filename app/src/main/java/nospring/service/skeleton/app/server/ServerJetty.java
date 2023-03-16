package nospring.service.skeleton.app.server;

import static nospring.service.skeleton.app.util.Util.*;

import jakarta.servlet.DispatcherType;
import java.util.EnumSet;
import nospring.service.skeleton.app.filter.ServletFilter;
import nospring.service.skeleton.app.servlet.AppPing;
import nospring.service.skeleton.app.servlet.AppReset;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public class ServerJetty {

  public void start() throws Exception {
    QueuedThreadPool threadPool =
        new QueuedThreadPool(SERVER_MAX_THREADS, SERVER_MIN_THREADS, SERVER_IDLE_TIMEOUT);
    Server server = new Server(threadPool);

    try (ServerConnector connector = new ServerConnector(server)) {
      String port = getSystemEnvProperty(SERVER_PORT);
      connector.setPort(port == null ? 8080 : Integer.parseInt(port));
      server.setConnectors(new Connector[] {connector});
    }

    server.setHandler(getServletHandler());
    server.start();
  }

  private ServletHandler getServletHandler() {
    ServletHandler servletHandler = new ServletHandler();
    servletHandler.addFilterWithMapping(
        ServletFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));

    servletHandler.addServletWithMapping(AppPing.class, CONTEXT_PATH + "/tests/ping");
    servletHandler.addServletWithMapping(AppReset.class, CONTEXT_PATH + "/tests/reset");

    return servletHandler;
  }
}
