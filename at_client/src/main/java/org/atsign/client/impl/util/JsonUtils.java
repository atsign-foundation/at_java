package org.atsign.client.impl.util;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Utility class for JSON encoding / decoding
 */
public class JsonUtils {

  public static final ObjectMapper MAPPER = objectMapper(false);

  private static ObjectMapper objectMapper(boolean isStrict) {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, isStrict);
    SimpleModule module = new SimpleModule();
    module.addDeserializer(OffsetDateTime.class, new AtStringDateTimeDeserializer());
    mapper.registerModule(module);
    return mapper;
  }

  /**
   * Jackson serializer for DateTime strings
   */
  private static class AtStringDateTimeDeserializer extends StdDeserializer<OffsetDateTime> {

    protected AtStringDateTimeDeserializer() {
      super((Class<?>) null);
    }

    static final DateTimeFormatter dateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS'Z'").withZone(ZoneId.of("UTC"));

    @Override
    public OffsetDateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
        throws IOException {
      String dateString = jsonParser.getText();
      ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateString, dateTimeFormatter);
      return zonedDateTime.toOffsetDateTime();
    }
  }
}
