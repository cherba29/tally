package tally.render.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import tally.statement.Transaction;
import tally.statement.TransactionStatement;

public class TransactionStatementSerializer extends JsonSerializer<TransactionStatement> {

  @Override
  public void serialize(TransactionStatement value, JsonGenerator jgen,
                        SerializerProvider provider) throws IOException, JsonProcessingException {
    jgen.writeStartObject();
    StatementSerializer.writeProperties(value, jgen);
    if (!value.isClosed()) {
      jgen.writeBooleanField("isCovered", value.isCovered());
      jgen.writeBooleanField("isProjectedCovered", value.isProjectedCovered());
      jgen.writeBooleanField("hasProjectedTransfer", value.isHasProjectedTransfer());
      jgen.writeArrayFieldStart("transactions");
      for (Transaction transaction : value.getTransactions()) {
        jgen.writeObject(transaction);
      }
      jgen.writeEndArray();
    }
    jgen.writeEndObject();
  }

  @Override
  public Class<TransactionStatement> handledType() {
    return TransactionStatement.class;
  }
}
