package org.atsign.client.cli;

import static org.atsign.client.util.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atsign.client.api.AtKeys;
import org.atsign.client.api.impl.connections.AtRootConnection;
import org.atsign.client.api.impl.connections.AtSecondaryConnection;
import org.atsign.client.api.impl.events.SimpleAtEventBus;
import org.atsign.client.util.AuthUtil;
import org.atsign.client.util.KeysUtil;
import org.atsign.client.util.TypedString;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.common.exceptions.AtSecondaryNotFoundException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Option;

public abstract class AbstractCli<T extends AbstractCli<T>> {

  protected static final Pattern DATA_JSON_NON_EMPTY_MAP = Pattern.compile("data:(\\{.+})");
  protected static final Pattern DATA_JSON_MAP = Pattern.compile("data:(\\{.*})");
  protected static final Pattern DATA_JSON_NO_EMPTY_LIST = Pattern.compile("data:(\\[.+])");
  protected static final Pattern DATA_INT = Pattern.compile("data:\\d+");
  public static final Pattern DATA_NON_WHITESPACE = Pattern.compile("data:(\\S+)");

  protected String rootUrl = "root.atsign.org";
  protected AtSign atSign;
  protected File keysFile;
  protected int connectionRetries = 1;
  private boolean verbose = false;

  protected abstract T self();

  public T setVerbose(boolean isVerbose) {
    this.verbose = isVerbose;
    return self();
  }

  public T setVerbose() {
    return setVerbose(true);
  }

  @Option(names = {"-r", "--root"}, paramLabel = "HOST:PORT",
      description = "atDirectory (aka root) server domain (e.g., root.atsign.org)")
  public T setRootUrl(String rootUrl) {
    this.rootUrl = rootUrl;
    return self();
  }

  @Option(names = {"-a", "--atsign"}, description = "the atsign e.g. @colin", paramLabel = "ATSIGN",
      converter = AtSignConverter.class)
  public T setAtSign(AtSign atSign) {
    this.atSign = atSign;
    return self();
  }

  @Option(names = {"-k", "--keys"}, paramLabel = "PATH", description = "path to atKeys file to use / create")
  public T setKeysFile(String path) {
    this.keysFile = new File(path);
    return self();
  }

  protected static File checkNotExists(File f) {
    if (f.exists()) {
      throw new IllegalArgumentException(f.getPath() + " would be overwritten");
    }
    return f;
  }

  protected static File checkExists(File f) {
    if (!f.exists()) {
      throw new IllegalArgumentException(f.getPath() + " not found");
    }
    return f;
  }

  protected static File getAtKeysFile(File keysFile, AtSign atSign) {
    return keysFile != null ? keysFile : KeysUtil.getKeysFile(atSign);
  }

  protected static void checkAtServerMatchesAtSign(AtSecondaryConnection connection, AtSign atSign) throws IOException {
    if (!matchDataJsonList(connection.executeCommand("scan")).contains("signing_publickey" + atSign)) {
      // TODO: understand precisely what this means (observed in Dart SDK)
      throw new IllegalStateException("TBC");
    }
  }

  protected static void deleteKey(AtSecondaryConnection connection, String key) {
    try {
      match(connection.executeCommand("delete:" + key), DATA_INT);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected static void authenticateWithApkam(AtSecondaryConnection connection, AtSign atSign, AtKeys keys)
      throws AtException, IOException {
    new AuthUtil().authenticateWithPkam(connection, atSign, keys);
  }

  protected AtSecondaryConnection createAtSecondaryConnection(AtSign atSign,
                                                              String rootUrl,
                                                              int retries)
      throws Exception {
    checkNotNull(atSign, "atsign not set");
    checkNotNull(rootUrl, "root server endpoint not set");

    String secondaryUrl = resolveSecondaryUrl(atSign, rootUrl, retries);
    AtSecondaryConnection conn =
        new AtSecondaryConnection(new SimpleAtEventBus(), atSign, secondaryUrl, null, false, verbose);
    int retriesRemaining = retries;
    Exception ex;
    do {
      try {
        conn.connect();
        return conn;
      } catch (Exception e) {
        ex = e;
        Thread.sleep(2000);
      }
    } while (retriesRemaining-- > 0);
    throw ex;
  }

  protected static String resolveSecondaryUrl(AtSign atSign, String rootUrl, int retries) throws Exception {
    int retriesRemaining = retries;
    Exception ex;
    do {
      try {
        return new AtRootConnection(rootUrl).lookupAtSign(atSign);
      } catch (AtSecondaryNotFoundException e) {
        ex = e;
        Thread.sleep(1000);
      }
    } while (retriesRemaining-- > 0);
    throw ex;
  }

  protected static String encodeKeyValuesAsJson(Object... nameValuePairs) throws Exception {
    return encodeAsJson(toObjectMap(nameValuePairs));
  }

  protected static Map<String, Object> toObjectMap(Object... nameValuePairs) {
    if ((nameValuePairs.length % 2) != 0) {
      throw new IllegalArgumentException("odd number of parameters");
    }
    Map<String, Object> map = new HashMap<>();
    for (int i = 0; i < nameValuePairs.length; i++) {
      String key = nameValuePairs[i].toString();
      Object value = nameValuePairs[++i];
      if (value instanceof TypedString) {
        map.put(key, value.toString());
      } else {
        map.put(key, value);
      }
    }
    return map;
  }

  protected static String encodeAsJson(Map<String, ?> map) throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    return objectMapper.writeValueAsString(map);
  }

  protected static Map<String, String> decodeJsonMapOfStrings(String json) {
    try {
      return new ObjectMapper().readValue(json, new TypeReference<Map<String, String>>() {});
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected static Map<String, Object> decodeJsonMapOfObjects(String json) {
    try {
      return new ObjectMapper().readValue(json, new TypeReference<Map<String, Object>>() {});
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected static List<Object> decodeJsonList(String json) {
    try {
      return new ObjectMapper().readValue(json, new TypeReference<List<Object>>() {});
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static List<String> decodeJsonListOfStrings(String json) {
    try {
      return new ObjectMapper().readValue(json, new TypeReference<List<String>>() {});
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected static String match(String input, Pattern pattern) {
    Matcher matcher = pattern.matcher(input);
    if (!matcher.matches()) {
      throw new RuntimeException("expected [" + pattern + "] but input was : " + input);
    }
    StringBuilder builder = new StringBuilder();
    if (matcher.groupCount() == 0) {
      builder.append(input);
    } else {
      for (int i = 1; i <= matcher.groupCount(); i++) {
        builder.append(matcher.group(i));
      }
    }
    return builder.toString();
  }

  protected static <T> T match(String input, Pattern pattern, Function<String, T> transformer) {
    return transformer.apply(match(input, pattern));
  }

  protected static String matchDataString(String input) {
    return match(input, DATA_NON_WHITESPACE, s -> s);
  }

  protected static int matchDataInt(String input) {
    return match(input, DATA_INT, Integer::parseInt);
  }

  protected static List<Object> matchDataJsonList(String input) {
    return match(input, DATA_JSON_NO_EMPTY_LIST, AbstractCli::decodeJsonList);
  }

  public static List<String> matchDataJsonListOfStrings(String input) {
    return match(input, DATA_JSON_NO_EMPTY_LIST, AbstractCli::decodeJsonListOfStrings);
  }

  protected static Map<String, String> matchDataJsonMapOfStrings(String input, boolean allowEmpty) {
    return match(input, allowEmpty ? DATA_JSON_MAP : DATA_JSON_NON_EMPTY_MAP, AbstractCli::decodeJsonMapOfStrings);
  }

  protected static Map<String, Object> matchDataJsonMapOfObjects(String input, boolean allowEmpty) {
    return match(input, allowEmpty ? DATA_JSON_MAP : DATA_JSON_NON_EMPTY_MAP, AbstractCli::decodeJsonMapOfObjects);
  }

  protected static Map<String, String> matchDataJsonMapOfStrings(String input) {
    return matchDataJsonMapOfStrings(input, false);
  }

  protected static Map<String, Object> matchDataJsonMapOfObjects(String input) {
    return matchDataJsonMapOfObjects(input, false);
  }

  protected static String ensureNotNull(String value, String defaultValue) {
    return value != null ? value : defaultValue;
  }

  static class AtSignConverter implements ITypeConverter<AtSign> {
    @Override
    public AtSign convert(String s) {
      return new AtSign(s);
    }
  }
}
