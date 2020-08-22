package tally;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;

import com.google.inject.servlet.GuiceFilter;

public class Main {

  public static void main(String[] list) throws Exception {
    Server server = new Server(8080);

    // Create a servlet context and add the jersey servlet.
    ServletContextHandler sch = new ServletContextHandler(server, "/");

    // Add our Guice listener that includes our bindings
    sch.addEventListener(new ServletConfig());

    // Then add GuiceFilter and configure the server to
    // reroute all requests through this filter.
    sch.addFilter(GuiceFilter.class, "/*", null);

    // Must add DefaultServlet for embedded Jetty.
    // Failing to do this will cause 404 errors.
    // This is not needed if web.xml is used instead.
    sch.addServlet(DefaultServlet.class, "/");

    ContextHandler fileContext = new ContextHandler();
    fileContext.setContextPath("/files");

    ResourceHandler resource_handler = new ResourceHandler();
    resource_handler.setDirectoriesListed(true);
    resource_handler.setWelcomeFiles(new String[]{ "index.html" });

    // TODO: make it command line flag.
    resource_handler.setResourceBase("../..");
    fileContext.setHandler(resource_handler);

    HandlerList handlers = new HandlerList();
    handlers.setHandlers(new Handler[] { fileContext, sch, new DefaultHandler() });
    server.setHandler(handlers);

    server.start();
    server.join();
  }
}
