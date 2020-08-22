package tally.render.jackson;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class DateSerializer extends JsonSerializer<Date> {
  private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

  @Override
  public void serialize(Date value, JsonGenerator jgen,
                        SerializerProvider provider) throws IOException, JsonProcessingException {
    jgen.writeString(DATE_FORMAT.format(value));
  }

  @Override
  public Class<Date> handledType() {
    return Date.class;
  }
}