package org.atsign.client.connection.protocol;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atsign.client.connection.api.AtClientConnection;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

public class TestConnectionBuilder {

  private Map<Pattern, Object> mapping = new LinkedHashMap<>();

  public static TestConnectionBuilder builder() {
    return new TestConnectionBuilder();
  }

  public TestConnectionBuilder stub(String command, String response) {
    return stub(Pattern.compile(command), response);
  }

  public TestConnectionBuilder stub(Pattern command, String response) {
    mapping.put(command, response);
    return this;
  }

  public TestConnectionBuilder stub(String command, Function<Matcher, String> fn) {
    return stub(Pattern.compile(command), fn);
  }

  public TestConnectionBuilder stub(Pattern command, Function<Matcher, String> fn) {
    mapping.put(command, fn);
    return this;
  }

  public TestConnectionBuilder stub(String command, Exception ex) {
    return stub(Pattern.compile(command), ex);
  }

  public TestConnectionBuilder stubExecutionException(String command) {
    return stub(Pattern.compile(command), new ExecutionException("deliberate", null));
  }

  public TestConnectionBuilder stub(Pattern command, Exception ex) {
    mapping.put(command, ex);
    return this;
  }

  public AtClientConnection build() throws ExecutionException, InterruptedException {
    AtClientConnection connection = Mockito.mock(AtClientConnection.class);
    when(connection.sendSync(anyString())).thenAnswer((Answer<String>) invocation -> {
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
    return connection;
  }
}
