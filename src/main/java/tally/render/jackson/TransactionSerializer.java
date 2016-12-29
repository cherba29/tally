package tally.render.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import tally.statement.Transaction;

public class TransactionSerializer  extends JsonSerializer<Transaction> {

  @Override
  public void serialize(Transaction value, JsonGenerator jgen,
                        SerializerProvider provider) throws IOException, JsonProcessingException {
    jgen.writeStartObject();
    jgen.writeStringField("toAccountName", value.getAccount().getName());
    jgen.writeBooleanField("isExpense", value.isExpense());
    jgen.writeBooleanField("isIncome", value.isIncome());
    jgen.writeStringField("description", value.getDescription());
    jgen.writeObjectField("balance", value.getBalance());
    Long balanceFromStart = value.getBalanceFromStart();
    if (balanceFromStart != null) {
      jgen.writeNumberField("balanceFromStart", balanceFromStart);
    }
    Long balanceFromEnd = value.getBalanceFromEnd();
    if (balanceFromEnd != null) {
      jgen.writeNumberField("balanceFromEnd", balanceFromEnd);
    }
    jgen.writeEndObject();
  }

  @Override
  public Class<Transaction> handledType() {
    return Transaction.class;
  }
}
