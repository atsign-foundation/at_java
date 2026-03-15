package org.atsign.client.impl.util;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Utility class for JSON encoding / decoding
 */
public class JsonUtils {

  private static final ObjectMapper MAPPER = objectMapper(false);

  private static final ObjectWriter PRETTY_WRITER = objectMapper(false).writerWithDefaultPrettyPrinter();

  /**
   * Decode a JSON String to a class instance.
   *
   * @param json The JSON to decode.
   * @param klass The type to instantiate.
   * @return An instance that corresponds to the JSON.
   * @param <T> The type of the instance.
   */
  public static <T> T readValue(String json, Class<T> klass) {
    try {
      return MAPPER.readValue(json, klass);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Decode a JSON String to a class instance.
   *
   * @param json The JSON to decode.
   * @param typeRef The type to instantiate.
   * @return An instance that corresponds to the JSON.
   * @param <T> The type of the instance.
   */
  public static <T> T readValue(String json, TypeReference<T> typeRef) {
    try {
      return MAPPER.readValue(json, typeRef);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Encode an Object to JSON.
   *
   * @param o The object to encode.
   * @return The JSON representation of the Object.
   */

  public static String writeValueAsString(Object o) {
    return writeValueAsString(o, false);
  }

  /**
   * Encode an Object to JSON.
   *
   * @param o The object to encode.
   * @param pretty If true then return multi-line indented JSON.
   * @return The JSON representation of the Object.
   */
  public static String writeValueAsString(Object o, boolean pretty) {
    try {
      return pretty ? PRETTY_WRITER.writeValueAsString(o) : MAPPER.writeValueAsString(o);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

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
