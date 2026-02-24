package org.atsign.cucumber.steps;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.atsign.client.connection.protocol.Responses.decodeJsonListOfStrings;
import static org.atsign.client.util.Preconditions.checkNotNull;
import static org.atsign.cucumber.helpers.Helpers.isHostPortReachable;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.atsign.client.api.AtClient;
import org.atsign.client.api.AtEvents;
import org.atsign.client.api.AtKeys;
import org.atsign.client.api.impl.clients.AtClients;
import org.atsign.client.util.EnrollmentId;
import org.atsign.client.util.KeysUtil;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.cucumber.helpers.AtDemoData;
import org.atsign.virtualenv.VirtualEnv;
import org.junit.jupiter.api.function.Executable;
import org.opentest4j.TestAbortedException;

import io.cucumber.java.After;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AtClientContext {

  private static final Set<AtEvents.AtEventType> ALL_EVENT_TYPES = Collections.unmodifiableSet(
                                                                                               new HashSet<>(Arrays
                                                                                                   .asList(AtEvents.AtEventType
                                                                                                       .values())));

  private static final Map<String, Boolean> ROOT_SERVERS = new HashMap<>();

  private static final boolean IS_VERBOSE_FORCE = System.getProperty("verbose", "").equalsIgnoreCase("true");

  private String rootHostAndPort = null;

  private boolean isVerbose;

  private long ttl = SECONDS.toMillis(5);

  private LinkedHashMap<QualifiedAtSign, AtClient> clients = new LinkedHashMap<>();

  private Map<QualifiedAtSign, AtClientEventListener> listeners = new HashMap<>();

  private QualifiedAtSign currentQualifiedAtSign;

  private String currentNamespace;

  private Exception expectedException;

  public long getKeyTtl() {
    return ttl;
  }

  @And("key ttl is {long} {timeunit}")
  public void setKeyTtl(long duration, TimeUnit unit) {
    ttl = unit.toMillis(duration);
  }

  public QualifiedAtSign getCurrentQualifiedAtSign() {
    return checkNotNull(currentQualifiedAtSign, "the is no current atsign, an atClient must be created first");
  }

  public String getRootHostAndPort() {
    return rootHostAndPort;
  }

  public void assertException(Executable command) {
    this.expectedException = assertThrows(Exception.class, command::execute);
  }

  private Exception getExpectedException() {
    return checkNotNull(expectedException, "no exception");
  }

  public boolean isVerbose() {
    return isVerbose || IS_VERBOSE_FORCE;
  }

  @After(order = Integer.MAX_VALUE)
  public void teardown() {
    clients.values().forEach(this::teardownKeysAndClose);
  }

  @Given("root server endpoint is {word}:{int}")
  public void setRootHostAndPort(String host, int port) throws AtException {
    rootHostAndPort = String.format("%s:%d", host, port);
  }

  @Given("root server is running")
  public void assumeRootServerIsRunning() throws AtException {
    boolean isRunning = ROOT_SERVERS.computeIfAbsent(rootHostAndPort, k -> isHostPortReachable(k, SECONDS.toMillis(2)));
    if (!isRunning) {
      throw createRootServerNotReachableAssumptionFailure(rootHostAndPort);
    }
  }

  @Given("atsign keys path is {path}")
  public void setClientAtSignKeyDir(File path) {
    checkKeysPathExists(path);
    KeysUtil.expectedKeysFilesLocation = path.getPath();
  }

  @Given("atsign keys path is at_demo_data package {path}")
  public void setClientAtSignKeyDirAsAtDemoDataDir(File path) {
    setClientAtSignKeyDir(AtDemoData.getDir(path));
  }

  @Given("atsign keys suffix is {word}")
  public void setClientAtSignKeySuffix(String s) {
    KeysUtil.keysFileSuffix = s;
  }

  @Given("verbose logging is {word}")
  public void setLogging(String onOrOff) throws AtException {
    isVerbose = onOrOff.equalsIgnoreCase("on") || onOrOff.equalsIgnoreCase("true");
  }

  @Given("AtClient with keys {path} for {atsign}")
  public void createCurrentAtClient(File keysFile, AtSign atSign) throws Exception {
    createCurrentAtClient(atSign, KeysUtil.loadKeys(resolveKeysFile(keysFile)), false);
  }

  public File resolveKeysFile(File keysFile) {
    if (keysFile.getParentFile() == null) {
      return new File(KeysUtil.expectedKeysFilesLocation, keysFile.getName());
    } else {
      return keysFile;
    }
  }

  private void createCurrentAtClient(AtSign atSign, AtKeys keys, boolean withMonitor) throws Exception {
    AtClient atClient = createAtClient(atSign, keys, withMonitor);
    currentQualifiedAtSign = new QualifiedAtSign(atClient.getAtSign(), getEnrollmentId(keys));
  }

  @Given("AtClient for {atsign}")
  public void createCurrentAtClient(AtSign atSign) throws Exception {
    createCurrentAtClient(atSign, KeysUtil.loadKeys(atSign), false);
  }

  @Given("AtClient is closed")
  public void closeCurrentAtClient() throws Exception {
    clients.remove(currentQualifiedAtSign).close();
  }

  @Given("namespace is set to {word}")
  public void setNamespace(String ns) {
    this.currentNamespace = ns;
  }

  @Given("namespace is unset")
  public void clearNamespace() {
    this.currentNamespace = null;
  }

  public String getNamespace() {
    return this.currentNamespace;
  }

  public boolean isNamespaceSet() {
    return this.currentNamespace != null;
  }

  @Given("AtClient with keys {path} fails for {atsign}")
  public void createAtClientExpectFail(File keysFile, AtSign atSign) {
    assertException(() -> createCurrentAtClient(keysFile, atSign));
  }

  @Given("AtClient fails for {atsign}")
  public void createAtClientExpectFail(AtSign atSign) {
    assertException(() -> createCurrentAtClient(atSign));
  }

  @Given("AtClient with keys {path} and startMonitor for {atsign}")
  public void createAtClientWithMonitor(File keysFile, AtSign atSign) throws Exception {
    createCurrentAtClient(atSign, KeysUtil.loadKeys(resolveKeysFile(keysFile)), true);
  }

  @Given("AtClient and startMonitor for {atsign}")
  public void createAtClientWithMonitor(AtSign atSign) throws Exception {
    createCurrentAtClient(atSign, KeysUtil.loadKeys(atSign), true);
  }

  @Given("{ordinal} {atsign} AtClient startMonitor")
  public void startAtClientMonitor(Integer ordinal, AtSign clientAtSign) throws Exception {
    AtClient atClient = lookupAtClient(clientAtSign, ordinal);
    startAtClientMonitor(atClient);
  }

  @Given("{atsign} AtClient startMonitor")
  public void startAtClientMonitor(AtSign clientAtSign) throws Exception {
    AtClient atClient = lookupOrCreateAtClient(clientAtSign);
    startAtClientMonitor(atClient);
  }

  @Given("AtClient startMonitor")
  public void startAtClientMonitor() throws Exception {
    AtClient atClient = lookupAtClient(currentQualifiedAtSign);
    startAtClientMonitor(atClient);
  }

  private void startAtClientMonitor(AtClient atClient) {
    if (!atClient.isMonitorRunning()) {
      atClient.startMonitor();
    }
  }

  @Given("{ordinal} {atsign} AtClient stopMonitor")
  public void stopAtClientMonitor(Integer ordinal, AtSign clientAtSign) throws Exception {
    AtClient atClient = lookupAtClient(clientAtSign, ordinal);
    stopAtClientMonitor(atClient);
  }

  @Given("{atsign} AtClient stopMonitor")
  public void stopAtClientMonitor(AtSign atSign) throws Exception {
    AtClient atClient = lookupOnlyAtClient(atSign);
    stopAtClientMonitor(atClient);
  }

  @Given("AtClient stopMonitor")
  public void stopAtClientMonitor() throws Exception {
    AtClient atClient = lookupAtClient(currentQualifiedAtSign);
    stopAtClientMonitor(atClient);
  }

  private void stopAtClientMonitor(AtClient atClient) {
    if (atClient.isMonitorRunning()) {
      atClient.stopMonitor();
      await().atMost(2, SECONDS).until(() -> !atClient.isMonitorRunning());
    }
  }

  @Then("exception was {exception}")
  public void assertExpectedExceptionClass(Class<AtException> expectedClass) throws AtException {
    try {
      if (expectedException.getCause() != null) {
        assertThat(expectedException.getCause().getClass(), typeCompatibleWith(expectedClass));
      } else {
        assertThat(expectedException.getClass(), typeCompatibleWith(expectedClass));
      }
    } catch (Throwable e) {
      expectedException.printStackTrace();
      throw e;
    }
  }

  @Then("exception message matches {string}")
  public void assertExpectedExceptionMessageMatches(String regex) throws AtException {
    assertThat(expectedException, notNullValue());
    Matcher matcher = Pattern.compile(regex).matcher(expectedException.getMessage());
    assertThat(expectedException.getMessage() + " does not match " + regex, matcher.find(), is(true));
  }

  @Then("exception was {exception} and message matches {string}")
  public void assertExpectedException(Class<AtException> expectedClass, String regex) throws AtException {
    assertExpectedExceptionClass(expectedClass);
    assertExpectedExceptionMessageMatches(regex);
  }

  @And("pause for {long} {timeunit}")
  public void pause(long duration, TimeUnit unit) throws Exception {
    log.debug("sleeping for {} {}", duration, unit);
    Thread.sleep(unit.toMillis(duration));
  }

  public AtClient lookupAtClient(QualifiedAtSign qualifiedAtSign) {
    return checkNotNull(clients.get(qualifiedAtSign), "no client has been created for " + qualifiedAtSign);
  }

  public AtClient lookupAtClient(AtSign atSign, int ordinal) {
    List<AtClient> list = lookupAtClients(atSign);
    if (list.size() < ordinal) {
      throw new IllegalArgumentException(list.size() + " AtClients for " + atSign);
    }
    return list.get(ordinal - 1);
  }

  public AtClient lookupOnlyAtClient(AtSign atSign) throws Exception {
    List<AtClient> list = lookupAtClients(atSign);
    if (list.size() > 1) {
      throw new IllegalArgumentException("multiple AtClients for " + atSign);
    }
    if (list.isEmpty()) {
      throw new IllegalArgumentException("no AtClients for " + atSign);
    }
    return list.get(0);
  }

  public AtClient lookupOrCreateAtClient(AtSign atSign) throws Exception {
    List<AtClient> list = lookupAtClients(atSign);
    if (list.size() > 1) {
      throw new IllegalArgumentException("multiple AtClients for " + atSign + " this is ambiguous");
    } else if (list.size() == 1) {
      return list.get(0);
    } else {
      return createAtClient(atSign, KeysUtil.loadKeys(atSign), false);
    }
  }

  private List<AtClient> lookupAtClients(AtSign atSign) {
    return clients.entrySet().stream()
        .filter(entry -> entry.getKey().getAtSign().equals(atSign))
        .map(Map.Entry::getValue)
        .collect(Collectors.toList());
  }

  private AtClient createAtClient(AtSign atSign, AtKeys keys, boolean withMonitor) throws Exception {
    QualifiedAtSign qualifiedAtSign = new QualifiedAtSign(atSign, getEnrollmentId(keys));
    if (clients.containsKey(qualifiedAtSign)) {
      throw new IllegalArgumentException("attempt to create another atClient for the same qualified atsign");
    }
    if (rootHostAndPort == null) {
      throw new IllegalArgumentException("root host and port not set");
    }
    AtClient atClient = AtClients.builder()
        .url(rootHostAndPort)
        .atSign(atSign)
        .keys(keys)
        .isVerbose(isVerbose())
        .build();
    AtClientEventListener listener = new AtClientEventListener(qualifiedAtSign);
    atClient.addEventListener(listener, ALL_EVENT_TYPES);
    clients.put(qualifiedAtSign, atClient);
    listeners.put(qualifiedAtSign, listener);
    if (withMonitor) {
      atClient.startMonitor();
      // wait for and old notifications to arrive and then clear listener
      Thread.sleep(SECONDS.toMillis(2));
      listener.clear();
    }
    return atClient;
  }

  private QualifiedAtSign lookupQualifiedAtSign(AtClient atClient) {
    return clients.entrySet().stream()
        .filter(entry -> entry.getValue() == atClient)
        .map(Map.Entry::getKey)
        .findFirst()
        .orElse(null);
  }

  private static EnrollmentId getEnrollmentId(AtKeys keys) {
    return keys.getEnrollmentId() != null ? keys.getEnrollmentId() : null;
  }

  public List<Map<String, String>> getEventData(AtClient atClient) throws Exception {
    return listeners.get(lookupQualifiedAtSign(atClient)).events.stream()
        .map(e -> e.asMapOfStrings())
        .collect(Collectors.toList());
  }

  public List<Map<String, String>> getEventData(AtClient atClient, AtEvents.AtEventType eventType) throws Exception {
    return listeners.get(lookupQualifiedAtSign(atClient)).events.stream()
        .filter(e -> e.eventType == eventType)
        .map(e -> e.asMapOfStrings())
        .collect(Collectors.toList());
  }

  public void clearEvents(AtClient atClient) {
    listeners.get(lookupQualifiedAtSign(atClient)).clear();
  }

  private static class AtClientEventListener implements AtEvents.AtEventListener {

    private final QualifiedAtSign atSign;
    private final List<AtClientEvent> events = new CopyOnWriteArrayList<>();

    public AtClientEventListener(QualifiedAtSign atSign) {
      this.atSign = atSign;
    }

    @Override
    public void handleEvent(AtEvents.AtEventType eventType, Map<String, Object> eventData) {
      log.debug("{} received {} : {}", atSign, eventType, eventData);
      events.add(new AtClientEvent(eventType, eventData));
    }

    private int clear() {
      int count = events.size();
      events.clear();
      log.debug("{} cleared {} events", atSign, count);
      return count;
    }
  }

  private static class AtClientEvent {
    final AtEvents.AtEventType eventType;
    final Map<String, Object> eventData;

    AtClientEvent(AtEvents.AtEventType eventType, Map<String, Object> eventData) {
      this.eventType = eventType;
      this.eventData = new HashMap<>(eventData);
    }

    Map<String, String> asMapOfStrings() {
      Map map = new HashMap();
      map.put("eventType", eventType.name());
      eventData.entrySet().stream()
          .filter(e -> e.getValue() != null)
          .forEach(e -> map.put(e.getKey(), e.getValue().toString()));
      return map;
    }
  }

  private static TestAbortedException createRootServerNotReachableAssumptionFailure(String rootHostAndPort) {
    String message = String.format("%s not reachable", rootHostAndPort);
    if (rootHostAndPort.contains("vip.ve")) {
      message = message + String.format(" (are you running the virtualenv? see test class %s)", VirtualEnv.class);
    }
    return new TestAbortedException(message);
  }

  private void checkKeysPathExists(File path) {
    String message = null;
    if (!path.exists()) {
      message = String.format("%s does not exist", path);
    }
    if (path.list().length < 1) {
      message = String.format("%s is empty", path);
    }
    if (message != null && path.getPath().startsWith("target")) {
      message = message + " (have you run mvn test-compile? this should download at_demo_data";
    }
    if (message != null) {
      throw new TestAbortedException(message);
    }
  }

  private void teardownKeysAndClose(AtClient client) {
    List<String> keys = scanNoThrow(client).stream()
        .filter(this::requiresTeardown)
        .collect(Collectors.toList());
    keys.forEach(k -> deleteKeyNoThrow(client, k));
    log.debug("teardown for {} deleted {}", lookupQualifiedAtSign(client), keys);
    try {
      client.close();
    } catch (IOException e) {
      log.debug("teardown close for {} threw exception : {}", lookupQualifiedAtSign(client), e.getMessage());
    }
  }

  private boolean requiresTeardown(String key) {
    if (key.contains(":signing_privatekey@")) {
      return false;
    }
    if (key.startsWith("public:pkaminstalled@")) {
      return false;
    }
    if (key.startsWith("public:publickey@")) {
      return false;
    }
    if (key.startsWith("public:signing_publickey@")) {
      return false;
    }
    return true;
  }

  private void deleteKeyNoThrow(AtClient client, String key) {
    try {
      client.executeCommand("delete:" + key, true);
    } catch (Exception e) {
      log.error("attempt to delete {} failed : {}", key, e.getMessage());
    }
  }

  private List<String> scanNoThrow(AtClient client) {
    try {
      String json = client.executeCommand("scan:showHidden:true .*", true).getRawDataResponse();
      return decodeJsonListOfStrings(json);
    } catch (Exception e) {
      log.error("failed to scan : {}", e.getMessage());
      return Collections.emptyList();
    }
  }
}
