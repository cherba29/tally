package tally.render.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import tally.statement.Statement;
import tally.statement.SummaryStatement;

public class SummaryStatementSerializer extends JsonSerializer<SummaryStatement> {
  @Override
  public void serialize(SummaryStatement value, JsonGenerator jgen,
                        SerializerProvider provider) throws IOException, JsonProcessingException {
    jgen.writeStartObject();
    StatementSerializer.writeProperties(value, jgen);
    if (!value.isClosed()) {
      jgen.writeArrayFieldStart("accounts");
      for (Statement stmt : value.getStatements()) {
        jgen.writeString(stmt.getName());
      }
      jgen.writeEndArray();
    }
    jgen.writeEndObject();
  }


  @Override
  public Class<SummaryStatement> handledType() {
    return SummaryStatement.class;
  }
}
