package tally.render.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import tally.core.Balance;

public class BalanceSerializer extends JsonSerializer<Balance> {

  @Override
  public void serialize(Balance value, JsonGenerator jgen,
                        SerializerProvider provider) throws IOException, JsonProcessingException {
    jgen.writeStartObject();
    jgen.writeNumberField("amount", value.getAmount());
    jgen.writeStringField("type", value.getType().name());
    jgen.writeObjectField("date", value.getDate());
    jgen.writeEndObject();
  }

  @Override
  public Class<Balance> handledType() {
    return Balance.class;
  }
}
