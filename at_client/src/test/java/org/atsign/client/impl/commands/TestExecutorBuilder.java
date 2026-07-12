package org.atsign.client.impl.commands;

import static org.atsign.client.impl.common.Preconditions.checkTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.api.AtCommandExecutorContext;
import org.atsign.client.impl.util.JsonUtils;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

public class TestExecutorBuilder {

  private Map<Pattern, Object> mapping = new LinkedHashMap<>();

  public static TestExecutorBuilder builder() {
    return new TestExecutorBuilder();
  }

  public TestExecutorBuilder stub(String command, String response) {
    return stub(Pattern.compile(command), response);
  }

  public TestExecutorBuilder stub(Pattern command, String response) {
    mapping.put(command, response);
    return this;
  }

  public TestExecutorBuilder stub(String command, Function<Matcher, String> fn) {
    return stub(Pattern.compile(command), fn);
  }

  public TestExecutorBuilder stub(Pattern command, Function<Matcher, String> fn) {
    mapping.put(command, fn);
    return this;
  }

  public TestExecutorBuilder stub(String command, Exception ex) {
    return stub(Pattern.compile(command), ex);
  }

  public TestExecutorBuilder stubExecutionException(String command) {
    return stub(Pattern.compile(command), new ExecutionException("deliberate", null));
  }

  public TestExecutorBuilder stubLookupResponse(String command, String key, String value, Object... metadata) {
    return stub(Pattern.compile(command), createLookupResponse(key, value, metadata));
  }

  public TestExecutorBuilder stub(Pattern command, Exception ex) {
    mapping.put(command, ex);
    return this;
  }

  public AtCommandExecutor build() throws ExecutionException, InterruptedException {
    AtCommandExecutor mock = Mockito.mock(AtCommandExecutor.class);
    // an EMPTY context yields no challenge, so authentication sends its own from: (which the tests
    // stub) rather than reusing an initial from: challenge this mock never issued
    when(mock.getContext()).thenReturn(AtCommandExecutorContext.EMPTY);
    when(mock.sendSync(anyString())).thenAnswer((Answer<String>) invocation -> {
      String command = invocation.getArgument(0);
      for (Map.Entry<Pattern, Object> entry : mapping.entrySet()) {
        Matcher matcher = entry.getKey().matcher(command);
        if (matcher.matches()) {
          if (entry.getValue() instanceof Throwable) {
            throw (Throwable) entry.getValue();
          } else if (entry.getValue() instanceof Function) {
            return ((Function<Matcher, String>) entry.getValue()).apply(matcher);
          } else {
            return (String) entry.getValue();
          }
        }
      }
      throw new IllegalArgumentException("no stub for " + command);
    });
    return mock;
  }

  private static String createLookupResponse(String key, String value, Object... metadata) {
    checkTrue((metadata.length % 2) == 0, "expected name value pairs");
    Map<String, Object> map = new HashMap<>();
    for (int i = 0; i < metadata.length; i++) {
      map.put((String) metadata[i], metadata[++i]);
    }
    return String.format("data:{\"key\": \"%s\",\"data\": \"%s\",\"metaData\": %s}",
                         key, value, JsonUtils.writeValueAsString(map));
  }

}
