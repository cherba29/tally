package tally.render.jackson;

import java.io.IOException;
import java.io.OutputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import tally.json.JsonResponse;
import tally.render.ResponseRenderer;

public class JacksonRenderer implements ResponseRenderer {

  @Override
  public void render(JsonResponse response, OutputStream outputStream) throws IOException {
    ObjectMapper mapper = new ObjectMapper();  // can reuse, share globally.

    SimpleModule module = new SimpleModule();
    module.addSerializer(new DateSerializer());
    module.addSerializer(new BalanceSerializer());
    module.addSerializer(new MonthSerializer());
    module.addSerializer(new SummaryStatementSerializer());
    module.addSerializer(new TransactionSerializer());
    module.addSerializer(new TransactionStatementSerializer());
    mapper.registerModule(module);

    mapper.writerWithDefaultPrettyPrinter().writeValue(outputStream, response);
  }
}
