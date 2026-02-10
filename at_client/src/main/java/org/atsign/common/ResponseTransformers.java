package org.atsign.common;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.atsign.client.api.Secondary.Response;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Parent class for transformers used to convert {@link Response} into fully decoded class instances
 */
@Slf4j
public class ResponseTransformers {
  static final ObjectMapper mapper = new ObjectMapper();

  /**
   * Transformer for scan command responses
   */
  public static class ScanResponseTransformer implements Function<Response, List<String>> {

    private final Predicate<String> filter;

    public ScanResponseTransformer(Predicate<String> filter) {
      this.filter = filter;
    }

    public ScanResponseTransformer() {
      this(k -> true);
    }

    @Override
    public List<String> apply(Response value) {

      if (value.getRawDataResponse() == null || value.getRawDataResponse().isEmpty()) {
        return null;
      }

      try {
        List<String> keys = mapper.readerForListOf(String.class).readValue(value.getRawDataResponse());
        return keys.stream().filter(filter).collect(Collectors.toList());
      } catch (Exception e) {
        log.error("unexpected exception", e);
        return null;
      }
    }

  }

  /**
   * Transformer for notify command responses
   */
  public static class NotifyResponseTransformer implements Function<Response, String> {
    @Override
    public String apply(Response value) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Transformer for notify (status) command responses
   */
  public static class NotificationStatusResponseTransformer implements Function<Response, NotificationStatus> {
    @Override
    public NotificationStatus apply(Response value) {
      throw new UnsupportedOperationException();
    }
  }
}
