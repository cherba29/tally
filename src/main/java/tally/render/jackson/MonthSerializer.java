package tally.render.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import tally.core.Month;

public class MonthSerializer extends JsonSerializer<Month> {

  @Override
  public void serialize(Month value, JsonGenerator jgen,
                        SerializerProvider provider) throws IOException, JsonProcessingException {
    jgen.writeString(value.toString());
  }

  @Override
  public Class<Month> handledType() {
    return Month.class;
  }
}
