package tally.render.jackson;

import java.io.IOException;
import java.math.BigDecimal;

import tally.statement.Statement;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class StatementSerializer extends JsonSerializer<Statement> {
  static void writeProperties(Statement value, JsonGenerator jgen) throws IOException {
    if (!value.isClosed()) {
      jgen.writeObjectField("startBalance", value.getStartBalance());
      jgen.writeObjectField("endBalance", value.getEndBalance());
      jgen.writeNumberField("addSub", value.getAddSub());
      Long change = value.getChange();
      if (change != null) {
        jgen.writeNumberField("change", change);
      }
      Double percentChange = value.getPercentChange();
      if (percentChange != null && !percentChange.isNaN() && !percentChange.isInfinite()) {
        jgen.writeNumberField(
            "percentChange",
            BigDecimal.valueOf(percentChange).setScale(2, BigDecimal.ROUND_HALF_UP));
      }
      Long unaccounted = value.getUnaccounted();
      if (unaccounted != null) {
        jgen.writeNumberField("unaccounted", unaccounted);
      }
      jgen.writeNumberField("income", value.getIncome());
      jgen.writeNumberField("totalTransfers", value.getTotalTransfers());
      jgen.writeNumberField("totalPayments", value.getTotalPayments());
      jgen.writeNumberField("inFlows", value.getInFlows());
      jgen.writeNumberField("outFlows", value.getOutFlows());
    } else {
      jgen.writeBooleanField("isClosed", true);
    }
  }

  @Override
  public void serialize(Statement value, JsonGenerator jgen,
                        SerializerProvider provider) throws IOException, JsonProcessingException {
    jgen.writeStartObject();
    writeProperties(value, jgen);
    jgen.writeEndObject();
  }
}