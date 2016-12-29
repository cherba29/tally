package tally;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;
import com.google.inject.Inject;

@Path("/budget")
public class BudgetHandler {
  private static final Logger logger = LoggerFactory.getLogger(BudgetHandler.class);

  private final Processor processor;

  @Inject
  public BudgetHandler(Processor processor) {
    this.processor = processor;
  }

  @GET
  @Produces(APPLICATION_JSON)
  public String get(@QueryParam("dir") String dir) {
    Stopwatch stopwatch = new Stopwatch();
    stopwatch.start();
    logger.info("Loading from " + dir);
    String responseText;
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      processor.processBudgetAt(dir, bos);
      responseText = bos.toString("UTF-8");
    } catch (Exception e) {
      logger.error("got", e);
      StringWriter stringWriter = new StringWriter();
      e.printStackTrace(new PrintWriter(stringWriter));
      responseText = stringWriter.toString();
    } finally {
      logger.info("Done");
    }
    logger.info("Done in " + stopwatch.elapsedMillis() + "ms, size " + responseText.length());
    return responseText;
  }
}
