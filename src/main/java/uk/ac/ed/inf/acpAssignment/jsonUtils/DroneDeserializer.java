package uk.ac.ed.inf.acpAssignment.jsonUtils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.math.BigDecimal;

public class DroneDeserializer extends JsonDeserializer<BigDecimal> {

  @Override
  public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    String value = p.getText();
    try {
      return new BigDecimal(value);
    } catch (NumberFormatException e) {
      // treat as zero as per spec
      return BigDecimal.ZERO;
    }
  }
}
