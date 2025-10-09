package org.atsign.cucumber.steps;

import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import org.atsign.client.api.AtClient;
import org.atsign.client.api.AtEvents;
import org.atsign.client.util.KeysUtil;
import org.atsign.common.AtException;
import org.atsign.common.AtSign;
import org.atsign.virtualenv.VirtualEnv;
import org.junit.AssumptionViolatedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import java.io.File;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.atsign.cucumber.helpers.Helpers.isHostPortReachable;

public class AtClientContext {

  private static final Logger LOGGER = LoggerFactory.getLogger(AtClientContext.class);

  private static final Set<AtEvents.AtEventType> ALL_EVENT_TYPES = Collections.unmodifiableSet(
      new HashSet<>(Arrays.asList(AtEvents.AtEventType.values()))
  );

  private  static final Map<String, Boolean> ROOT_SERVERS = new HashMap<>();

  private String rootHostAndPort = null;

  private boolean isVerbose;

  private long ttl = SECONDS.toMillis(5);

  private Map<AtSign, AtClient> clients = new HashMap<>();

  private Map<AtSign, AtClientEventListener> listeners = new HashMap<>();

  private AtSign currentAtSign;

  public long getKeyTtl() {
    return ttl;
  }

  public AtSign getCurrentAtSign() {
    if (currentAtSign == null) {
      throw new IllegalArgumentException("the is no current atsign");
    }
    return currentAtSign;
  }

  @After(order = Integer.MAX_VALUE)
  public void teardown() {
    int count = 0;
    for (Map.Entry<AtSign, AtClientEventListener> entry : listeners.entrySet()) {
      count += entry.getValue().clear();
    }
    LOGGER.info("discarded {} events", count);
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

  @Given("atsign keys suffix is {word}")
  public void setClientAtSignKeySuffix(String s) {
    KeysUtil.keysFileSuffix = s;
  }

  @Given("verbose logging is {word}")
  public void setLogging(String onOrOff) throws AtException {
    isVerbose = onOrOff.equalsIgnoreCase("on") || onOrOff.equalsIgnoreCase("true");
  }

  @Given("AtClient for {atsign}")
  public void createAtClient(AtSign atSign) throws AtException {
    currentAtSign = getAtClient(atSign, false).getAtSign();
  }

  @Given("AtClient and startMonitor for {atsign}")
  public void createAtClientWithMonitor(AtSign atSign) throws AtException {
    currentAtSign = getAtClient(atSign, true).getAtSign();
  }

  @Given("{atsign} AtClient startMonitor")
  public void startAtClientMonitor(AtSign atSign) throws AtException {
    AtClient atClient = getAtClient(atSign);
    if (!atClient.isMonitorRunning()) {
      atClient.startMonitor();
    }
  }

  @Given("AtClient startMonitor")
  public void startAtClientMonitor() throws AtException {
    startAtClientMonitor(currentAtSign);
  }

  @Given("{atsign} AtClient stopMonitor")
  public void stopAtClientMonitor(AtSign atSign) throws AtException {
    AtClient atClient = getAtClient(atSign);
    if (atClient.isMonitorRunning()) {
      atClient.stopMonitor();
      Awaitility.await().atMost(2, SECONDS).until(() -> !atClient.isMonitorRunning());
      LOGGER.info("discarded {} events", listeners.get(atSign).clear());
    }
  }

  @Given("AtClient stopMonitor")
  public void stopAtClientMonitor() throws AtException {
    stopAtClientMonitor(currentAtSign);
  }

  public AtClient getAtClient(AtSign atSign) throws AtException {
    return getAtClient(atSign, false);
  }

  public AtClient getAtClient(AtSign atSign, boolean monitor) throws AtException {
    AtClient atClient = clients.get(atSign);
    if (atClient == null) {
      if (rootHostAndPort == null) {
        throw new IllegalArgumentException("root host and port not set");
      }
      atClient = AtClient.withRemoteSecondary(rootHostAndPort, atSign, isVerbose);
      AtClientEventListener listener = new AtClientEventListener(atSign);
      atClient.addEventListener(listener, ALL_EVENT_TYPES);
      clients.put(atSign, atClient);
      listeners.put(atSign, listener);
      if (monitor) {
        atClient.startMonitor();
      }
    }
    return atClient;
  }

  public List<Map<String, String>> getEventData(AtSign atSign) throws Exception {
    return listeners.get(atSign).events.stream()
        .map(e -> e.asMapOfStrings())
        .collect(Collectors.toList());
  }

  public List<Map<String, String>> getEventData(AtSign atSign, AtEvents.AtEventType eventType) throws Exception {
    return listeners.get(atSign).events.stream()
        .filter(e -> e.eventType == eventType)
        .map(e -> e.asMapOfStrings())
        .collect(Collectors.toList());
  }

  public void clearEvents(AtSign atSign) {
    listeners.get(atSign).clear();
  }

  private static class AtClientEventListener implements AtEvents.AtEventListener {

    private final AtSign atSign;
    private final List<AtClientEvent> events = new CopyOnWriteArrayList<>();

    public AtClientEventListener(AtSign atSign) {
      this.atSign = atSign;
    }

    @Override
    public void handleEvent(AtEvents.AtEventType eventType, Map<String, Object> eventData) {
      LOGGER.info("{} received {} : {}", atSign, eventType, eventData);
      events.add(new AtClientEvent(eventType, eventData));
    }

    private int clear() {
      int count = events.size();
      events.clear();
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

  private static AssumptionViolatedException createRootServerNotReachableAssumptionFailure(String rootHostAndPort) {
    String message = String.format("%s not reachable", rootHostAndPort);
    if (rootHostAndPort.contains("vip.ve")) {
      message = message + String.format(" (are you running the virtualenv? see test class %s)", VirtualEnv.class);
    }
    return new AssumptionViolatedException(message);
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
      throw new AssumptionViolatedException(message);
    }
  }
}
