package org.atsign.common;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.atsign.client.api.Secondary.Response;

import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
public class ResponseTransformers {
  static final ObjectMapper mapper = new ObjectMapper();

  /// Transforms the data from type T to type V
  public interface ResponseTransformer<T, V> {
    V transform(T value);
  }

  public static class ScanResponseTransformer implements ResponseTransformer<Response, List<String>> {

    private final Predicate<String> filter;

    public ScanResponseTransformer(Predicate<String> filter) {
      this.filter = filter;
    }

    public ScanResponseTransformer() {
      this(k -> true);
    }

    @Override
    public List<String> transform(Response value) {

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

  public static class NotifyResponseTransformer implements ResponseTransformer<Response, String> {
    @Override
    public String transform(Response value) {
      throw new RuntimeException("Not Implemented");
    }
  }


  public static class NotificationStatusResponseTransformer
      implements ResponseTransformer<Response, NotificationStatus> {
    @Override
    public NotificationStatus transform(Response value) {
      throw new RuntimeException("Not Implemented");
    }
  }
}
