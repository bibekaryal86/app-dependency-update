package nospring.service.skeleton.app.servlet;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class AppReset extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    // do all reset actions here
    response.setCharacterEncoding("utf-8");
    response.addHeader("Access-Control-Allow-Origin", "*");
    response.setContentType("application/json");
    response.setStatus(200);
    response.getWriter().print("{\"reset\": \"successful\"}");
  }
}
