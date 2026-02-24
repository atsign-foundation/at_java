package org.atsign.client.connection.netty;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.atsign.client.connection.protocol.AtExceptions.throwOnReadyException;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.atsign.client.connection.api.AtClientConnection;
import org.atsign.client.connection.api.AtEndpointSupplier;
import org.atsign.client.connection.common.SimpleReconnectStrategy;
import org.atsign.client.connection.netty.NettyAtClientConnection.NettyAtClientConnectionBuilder;
import org.atsign.client.connection.protocol.AtExceptions;
import org.atsign.common.exceptions.AtSecondaryConnectException;
import org.atsign.common.exceptions.AtSecondaryNotFoundException;
import org.atsign.common.exceptions.AtTimeoutException;
import org.atsign.common.exceptions.AtUnauthenticatedException;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("unchecked")
@Slf4j
class NettyAtClientConnectionTest {

  private TestServer server;
  private TestEndPointSupplier endPointSupplier;
  private NettyAtClientConnectionBuilder connectionBuilder;
  private TestReconnectStrategy reconnectStrategy;

  @BeforeEach
  public void setup() throws Exception {
    server = new TestServer();
    stubTestServerConnectAndResponseBehaviour(server);
    endPointSupplier = new TestEndPointSupplier();
    connectionBuilder = NettyAtClientConnection.builder()
        .endpoint(endPointSupplier)
        .sslContext(server.getClientSslContext());
    reconnectStrategy = TestReconnectStrategy.testBuilder()
        .maxReconnectRetries(2)
        .resolveEndpointFrequency(1)
        .reconnectPauseMillis(100)
        .build();
  }

  @AfterEach
  public void teardown() throws Exception {
    server.close();
  }

  @Test
  void testConnectSucceed() throws Exception {
    try (NettyAtClientConnection connection = connectionBuilder.build()) {
      assertThat(endPointSupplier.invocationCount, equalTo(1));
    }
  }

  @Test
  void testBuilderOnReadyIsInvokedOnConnection() throws Exception {
    Consumer<AtClientConnection> consumer = mock(Consumer.class);
    connectionBuilder.onReady(consumer);
    try (NettyAtClientConnection connection = connectionBuilder.build()) {
      verify(consumer).accept(eq(connection));
    }
  }

  @Test
  void testBuilderOnReadyIsInvokedOnReconnect() throws Exception {
    Consumer<AtClientConnection> consumer = mock(Consumer.class);
    connectionBuilder.onReady(consumer).reconnect(reconnectStrategy);
    try (NettyAtClientConnection connection = connectionBuilder.build()) {
      await().until(connection::isReady);
      server.closeClientSocket();
      await().until(connection::isReady);
      server.closeClientSocket();
      await().until(connection::isReady);

      verify(consumer, times(3)).accept(eq(connection));
    }
  }

  @Test
  void testOnReadyIsInvokedIfReady() throws Exception {
    Consumer<AtClientConnection> consumer = mock(Consumer.class);
    try (NettyAtClientConnection connection = connectionBuilder.build()) {
      await().until(connection::isReady);
      connection.onReady(consumer);

      verify(consumer, timeout(SECONDS.toMillis(1))).accept(eq(connection));
    }
  }

  @Test
  void testOnReadyWhenConnectionIsEstablishedButBeforeConnectionIsReady() throws Exception {
    Consumer<AtClientConnection> consumer = mock(Consumer.class);
    CountDownLatch latch = new CountDownLatch(1);
    stubTestServerConnectAndResponseBehaviourWithConnectLatch(server, latch);
    try (NettyAtClientConnection connection = connectionBuilder.build()) {
      connection.onReady(consumer);
      latch.countDown();

      verify(consumer, timeout(SECONDS.toMillis(1))).accept(eq(connection));
    }
  }

  @Test
  void testOnReadyBeforeConnectionIsBound() throws Exception {
    Consumer<AtClientConnection> consumer = mock(Consumer.class);
    server.closeServerSocket();
    connectionBuilder.reconnect(reconnectStrategy);
    try (NettyAtClientConnection connection = connectionBuilder.build()) {
      connection.onReady(consumer);
      server.newServerSocket();

      verify(consumer, timeout(SECONDS.toMillis(1))).accept(eq(connection));
    }
  }

  @Test
  void testOnReadyBeforeConnectionIsAccepted() throws Exception {
    Consumer<AtClientConnection> consumer = mock(Consumer.class);
    server.closeServerSocket();
    server.newServerSocketWithoutAccept();
    connectionBuilder.reconnect(reconnectStrategy);
    try (NettyAtClientConnection connection = connectionBuilder.build()) {
      connection.onReady(consumer);
      server.accept();

      verify(consumer, timeout(SECONDS.toMillis(1))).accept(eq(connection));
    }
  }

  @Test
  void testOnReadyWhilstReadying() throws Exception {
    Consumer<AtClientConnection> consumer = mock(Consumer.class);
    CountDownLatch latch = new CountDownLatch(1);
    stubTestServerConnectAndResponseBehaviourWithRequestLatch(server, latch);
    connectionBuilder.onReady(AtExceptions.throwOnReadyException(c -> c.sendSync("request")));
    try (NettyAtClientConnection connection = connectionBuilder.build()) {
      connection.onReady(consumer);
      latch.countDown();

      verify(consumer, timeout(SECONDS.toMillis(1))).accept(eq(connection));
    }
  }

  @Test
  void testOnReadyNetworkExceptionsShouldNotBeFatalForTheConnection() throws Exception {
    AtomicInteger requestCount = new AtomicInteger();
    server.setRequestHandler(s -> {
      if (s == null || requestCount.incrementAndGet() > 1) {
        testServerResponse(server, s);
      } else {
        // disconnect on first request
        server.closeClientSocket();
      }
    });
    connectionBuilder.reconnect(reconnectStrategy).onReady(throwOnReadyException(c -> c.sendSync("request")));
    try (NettyAtClientConnection connection = connectionBuilder.build()) {
      assertThat(connection.sendSync("request2"), equalTo("response2"));
    }
  }

  @Test
  void testOnReadyAtExceptionsShouldBeFatalForTheConnection() throws Exception {
    connectionBuilder.reconnect(reconnectStrategy).onReady(
                                                           throwOnReadyException(c -> {
                                                             throw new AtUnauthenticatedException("deliberate");
                                                           }));
    Exception ex = assertThrows(Exception.class, () -> connectionBuilder.build());
    assertThat(ex.getMessage(), containsString("deliberate"));
  }

  @Test
  void testSendSync() throws Exception {
    try (AtClientConnection connection = connectionBuilder.build()) {
      assertThat(connection.sendSync("request"), equalTo("response"));
    }
  }

  @Test
  void testSendSyncAfterClosesThrowsException() throws Exception {
    try (AtClientConnection connection = connectionBuilder.build()) {
      connection.close();
      Exception ex = assertThrows(Exception.class, () -> connection.sendSync("request"));
      assertThat(ex.getMessage(), containsString("connection closed"));
    }
  }

  @Test
  void testSendSyncFromOnReady() throws Exception {
    List<String> responses = new CopyOnWriteArrayList<>();
    connectionBuilder.onReady(c -> {
      try {
        responses.add(c.sendSync("request1"));
        responses.add(c.sendSync("request2"));
      } catch (ExecutionException | InterruptedException e) {
        throw new RuntimeException(e);
      }
    });
    try (NettyAtClientConnection connection = connectionBuilder.build()) {
      await().until(connection::isReady);
      assertThat(responses, contains("response1", "response2"));
    }
  }

  @Test
  void testSendSyncFromConsumerTriggersException() throws Exception {
    stubTestServerConnectAndAndAutomaticNotification(server);
    try (AtClientConnection connection = connectionBuilder.build()) {
      AtomicReference<Exception> ex = new AtomicReference<>();
      connection.sendSync("monitor", s -> {
        try {
          connection.sendSync("request");
        } catch (Exception e) {
          ex.set(e);
        }
      });
      await().until(() -> ex.get() != null);
      assertThat(ex.get().getMessage(), containsString("send on netty event thread is prohibited"));
    }
  }

  @Test
  void testSend() throws Exception {
    try (AtClientConnection connection = connectionBuilder.build()) {
      assertThat(connection.send("request").get(), equalTo("response"));
    }
  }

  @Test
  void testSendFromConsumerTriggersException() throws Exception {
    stubTestServerConnectAndAndAutomaticNotification(server);
    try (AtClientConnection connection = connectionBuilder.build()) {
      AtomicReference<Exception> ex = new AtomicReference<>();
      connection.sendSync("monitor", s -> {
        try {
          connection.send("request");
        } catch (Exception e) {
          ex.set(e);
        }
      });
      await().until(() -> ex.get() != null);
      assertThat(ex.get().getMessage(), containsString("send on netty event thread is prohibited"));
    }
  }

  @Test
  void testSendFromOnReadyTriggersException() throws Exception {
    connectionBuilder.onReady(c -> c.send("request"));
    Exception ex = assertThrows(Exception.class, () -> connectionBuilder.build());
    assertThat(ex.getMessage(), containsString("onReady is prohibited from invoking send, use sendSync"));
  }

  @Test
  void testSendMultipleTimesWorksWhenNumberOfCommandsIsLessThanOrEqualToTheQueueLimit() throws Exception {
    connectionBuilder.queueLimit(3);
    try (AtClientConnection connection = connectionBuilder.build()) {
      CompletableFuture<String> future1 = connection.send("request1");
      CompletableFuture<String> future2 = connection.send("request2");
      CompletableFuture<String> future3 = connection.send("request3");
      assertThat(future1.get(), equalTo("response1"));
      assertThat(future2.get(), equalTo("response2"));
      assertThat(future3.get(), equalTo("response3"));
    }
  }

  @Test
  void testSendMultipleTimesWillCompleteExceptionallyWhenQueueLimitIsBreached() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    stubTestServerConnectAndResponseBehaviourWithRequestLatch(server, latch);
    connectionBuilder.queueLimit(1);
    try (AtClientConnection connection = connectionBuilder.build()) {
      CompletableFuture<String> future1 = connection.send("request1");
      CompletableFuture<String> future2 = connection.send("request2");
      CompletableFuture<String> future3 = connection.send("request3");
      latch.countDown();
      assertThat(future1.get(), equalTo("response1"));
      assertThat(future2.get(), equalTo("response2"));
      ExecutionException ex = assertThrows(ExecutionException.class, future3::get);
      assertThat(ex.getCause(), instanceOf(AtTimeoutException.class));
      assertThat(ex.getMessage(), containsString("queue is full"));
    }
  }

  @Test
  void testSendWithFuture() throws Exception {
    try (AtClientConnection connection = connectionBuilder.build()) {
      CompletableFuture<String> future = new CompletableFuture<>();
      connection.send("request", future);
      assertThat(future.get(), equalTo("response"));
    }
  }

  @Test
  void testSendSyncWithConsumer() throws Exception {
    try (AtClientConnection connection = connectionBuilder.build()) {
      List<String> list = new CopyOnWriteArrayList<>();
      Consumer<String> consumer = list::add;
      connection.sendSync("monitor", consumer);
      await().until(() -> "monitor".equals(server.poll()));
      server.writeAndFlush("notification:one\n", "notification:two\n", "notification:three\n");
      await().until(() -> list.size() == 3);
      assertThat(list, contains("notification:one", "notification:two", "notification:three"));
    }
  }

  @Test
  void testSendSyncWithConsumerFromConsumerTriggersException() throws Exception {
    stubTestServerConnectAndAndAutomaticNotification(server);
    try (AtClientConnection connection = connectionBuilder.build()) {
      AtomicReference<Exception> ex = new AtomicReference<>();
      connection.sendSync("monitor", s -> {
        try {
          connection.sendSync("request", response -> {
          });
        } catch (Exception e) {
          ex.set(e);
        }
      });
      await().until(() -> ex.get() != null);
      assertThat(ex.get().getMessage(), containsString("send on netty event thread is prohibited"));
    }
  }

  @Test
  void testSendWithConsumerFromConsumerTriggersException() throws Exception {
    stubTestServerConnectAndAndAutomaticNotification(server);
    try (AtClientConnection connection = connectionBuilder.build()) {
      AtomicReference<Exception> ex = new AtomicReference<>();
      connection.sendSync("monitor", s -> {
        try {
          connection.send("request", response -> {
          }, new CompletableFuture<>());
        } catch (Exception e) {
          ex.set(e);
        }
      });
      await().until(() -> ex.get() != null);
      assertThat(ex.get().getMessage(), containsString("send on netty event thread is prohibited"));
    }
  }

  @Test
  void testSendSyncWithConsumerFromOnReady() throws Exception {
    Consumer<String> consumer = mock(Consumer.class);
    stubTestServerConnectAndAndAutomaticNotification(server);
    connectionBuilder.onReady(c -> {
      try {
        c.sendSync("monitor", consumer);
      } catch (ExecutionException | InterruptedException e) {
        throw new RuntimeException(e);
      }
    });
    try (NettyAtClientConnection connection = connectionBuilder.build()) {
      await().until(connection::isReady);
      verify(consumer).accept("notification:xyz");
    }
  }

  @Test
  void testSendWithConsumerFromOnReadyTriggersException() throws Exception {
    connectionBuilder.onReady(c -> c.send("monitor", s -> {
    }, new CompletableFuture<>()));
    Exception ex = assertThrows(Exception.class, () -> connectionBuilder.build());
    assertThat(ex.getMessage(), containsString("onReady is prohibited from invoking send, use sendSync"));
  }

  @Test
  void testSendSyncWithConsumerAndFuture() throws Exception {
    try (AtClientConnection connection = connectionBuilder.build()) {
      CompletableFuture<Void> future = new CompletableFuture<>();
      List<String> list = new CopyOnWriteArrayList<>();
      Consumer<String> consumer = list::add;
      connection.send("monitor", consumer, future);
      future.get();
      await().until(() -> "monitor".equals(server.poll()));
      server.writeAndFlush("notification:one\n", "notification:two\n", "notification:three\n");
      await().until(() -> list.size() == 3);
      assertThat(list, contains("notification:one", "notification:two", "notification:three"));
    }
  }

  @Test
  void testConnectFail() {
    server.closeServerSocket();
    Exception ex = assertThrows(Exception.class, () -> connectionBuilder.build());
    assertThat(ex.getMessage(), containsString("Connection refused"));
  }

  @Test
  void testConnectEndpointProviderException() {
    connectionBuilder.endpoint(() -> {
      throw new AtSecondaryNotFoundException("deliberate");
    });
    Exception ex = assertThrows(Exception.class, () -> connectionBuilder.build());
    assertThat(ex.getMessage(), containsString("deliberate"));
  }

  @Test
  void testConnectRetryFail() throws Exception {
    connectionBuilder.reconnect(reconnectStrategy).timeoutMillis(500L);
    server.closeServerSocket();
    try (NettyAtClientConnection connection = connectionBuilder.build()) {
      ExecutionException ex = assertThrows(ExecutionException.class, () -> connection.sendSync("request"));
      assertThat(ex.getMessage(), containsString("reconnect retries exceeded"));
      assertThat(reconnectStrategy.connectFailCount, equalTo(3));
      assertThat(reconnectStrategy.connectException.getMessage(), containsString("Connection refused"));
    }
  }

  @Test
  void testConnectRetryFailTimeout() throws Exception {
    reconnectStrategy.reconnectNoLimit();
    connectionBuilder.reconnect(reconnectStrategy).timeoutMillis(500L);
    server.closeServerSocket();
    try (AtClientConnection connection = connectionBuilder.build()) {
      ExecutionException ex = assertThrows(ExecutionException.class, () -> connection.sendSync("request"));
      assertThat(ex.getMessage(), containsString("timed out (in queue)"));
    }
  }

  @Test
  void testConnectRetrySucceed() throws Exception {
    connectionBuilder.reconnect(reconnectStrategy).timeoutMillis(500L);
    server.closeServerSocket();
    try (AtClientConnection connection = connectionBuilder.build()) {
      server.newServerSocket();
      assertThat(connection.sendSync("request"), equalTo("response"));
      assertThat(reconnectStrategy.connectFailCount, greaterThanOrEqualTo(1));
      assertThat(reconnectStrategy.connectException.getMessage(), containsString("Connection refused"));
    }
  }

  @Test
  void testReconnectAfterSocketDisconnect() throws Exception {
    connectionBuilder.reconnect(reconnectStrategy).timeoutMillis(500L);
    try (AtClientConnection connection = connectionBuilder.build()) {
      server.closeClientSocket();
      assertThat(connection.sendSync("request"), equalTo("response"));
      assertThat(reconnectStrategy.disconnectCount, greaterThanOrEqualTo(1));
      assertThat(reconnectStrategy.disconnectException.getMessage(), containsString("connection closed"));
      assertThat(endPointSupplier.invocationCount, equalTo(1));
    }
  }

  @Test
  void testReconnectAndResendPendingAfterSocketDisconnect() throws Exception {
    connectionBuilder.reconnect(reconnectStrategy).timeoutMillis(500L);
    stubTestServerToCloseClientSocketOnRequest(server);
    try (AtClientConnection connection = connectionBuilder.build()) {
      assertThat(connection.sendSync("request"), equalTo("response"));
    }
  }

  @Test
  void testReconnectAndSendQueueAfterSocketDisconnectTriggersPendingRequestToTimeout() throws Exception {
    TestClock clock = new TestClock();
    connectionBuilder.reconnect(reconnectStrategy).timeoutMillis(500L).clock(clock).queueLimit(2);
    CountDownLatch latch = new CountDownLatch(1);
    stubTestServerToCloseClientSocketOnRequest(server, latch, clock, 500L);
    try (NettyAtClientConnection connection = connectionBuilder.build()) {
      CompletableFuture<String> future = connection.send("request");
      await().until(() -> connection.getPendingSize() == 1);
      clock.advance(1);
      CompletableFuture<String> future2 = connection.send("request2");
      await().until(() -> connection.getQueuedSize() == 1);
      log.info("setting latch");
      latch.countDown();
      ExecutionException ex = assertThrows(ExecutionException.class, future::get);
      assertThat(ex.getCause(), instanceOf(AtTimeoutException.class));
      assertThat(future2.get(), equalTo("response2"));
    }
  }

  @Test
  void testReconnectAfterSocketAndSocketServerDisconnect() throws Exception {
    connectionBuilder.reconnect(reconnectStrategy);
    try (AtClientConnection connection = connectionBuilder.build()) {
      server.closeServerSocket();
      CompletableFuture<String> future = connection.send("request");
      server.newServerSocket();
      await().until(future::isDone);
      assertThat(future.get(), equalTo("response"));
      assertThat(reconnectStrategy.disconnectCount, greaterThanOrEqualTo(1));
      assertThat(reconnectStrategy.disconnectException.getMessage(), containsString("connection closed"));
    }
  }

  @Test
  void testReconnectTimeout() throws Exception {
    connectionBuilder.reconnect(reconnectStrategy).timeoutMillis(500L);
    ExecutionException ex = assertThrows(ExecutionException.class, () -> {
      try (AtClientConnection connection = connectionBuilder.build()) {
        server.closeServerSocket();
        newSingleThreadScheduledExecutor().schedule(() -> server.newServerSocket(), 5, SECONDS);
        connection.sendSync("request");
      }
    });
    assertThat(ex.getCause(), instanceOf(AtSecondaryConnectException.class));
    assertThat(ex.getMessage(), containsString("reconnect retries exceeded"));
  }

  @Test
  void testReconnectAfterSocketAndSocketServerDisconnectAndAcceptPause() throws Exception {
    connectionBuilder.reconnect(reconnectStrategy).timeoutMillis(5000L);
    try (AtClientConnection connection = connectionBuilder.build()) {
      server.closeServerSocket();
      server.newServerSocketWithoutAccept();
      sleep(SECONDS.toMillis(1));
      server.accept();
      assertThat(connection.sendSync("request"), equalTo("response"));
      assertThat(reconnectStrategy.disconnectCount, greaterThanOrEqualTo(1));
      assertThat(reconnectStrategy.disconnectException.getMessage(), containsString("connection closed"));
      assertThat(endPointSupplier.invocationCount, equalTo(1));
    }
  }

  @Test
  void testReconnectAfterSocketAndSocketServerDisconnectAndMove() throws Exception {
    connectionBuilder.reconnect(reconnectStrategy);
    try (AtClientConnection connection = connectionBuilder.build()) {
      server.closeServerSocket();
      CompletableFuture<String> future = connection.send("request");
      server.newServerSocketNewPort();
      await().until(future::isDone);
      assertThat(future.get(), equalTo("response"));
      assertThat(reconnectStrategy.disconnectCount, greaterThanOrEqualTo(1));
      assertThat(reconnectStrategy.disconnectException.getMessage(), containsString("connection closed"));
      assertThat(endPointSupplier.invocationCount, equalTo(2));
    }
  }

  @Test
  void testCloseCompletesPendingCommands() throws Exception {
    connectionBuilder.queueLimit(2);
    CountDownLatch latch = new CountDownLatch(1);
    stubTestServerConnectAndResponseBehaviourWithRequestLatch(server, latch);
    try (NettyAtClientConnection connection = connectionBuilder.build()) {
      CompletableFuture<String> future1 = connection.send("request1");
      CompletableFuture<String> future2 = connection.send("request2");
      await().until(() -> connection.getPendingSize() == 1 && connection.getQueuedSize() == 1);
      connection.close();
      Exception ex = assertThrows(Exception.class, future1::get);
      assertThat(ex.getMessage(), containsString("connection clos"));
      ex = assertThrows(Exception.class, future2::get);
      assertThat(ex.getMessage(), containsString("connection clos"));
    } finally {
      latch.countDown();
    }
  }

  @Test
  void testConsumerExceptionsAreLogged() throws Exception {
    stubTestServerConnectAndAndAutomaticNotification(server);
    Logger logger = mock(Logger.class);
    connectionBuilder.log(logger);
    try (AtClientConnection connection = connectionBuilder.build()) {
      connection.sendSync("monitor", s -> {
        throw new RuntimeException("deliberate");
      });
      ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
      verify(logger).error(eq("exception in handler"), captor.capture());
      assertThat(captor.getValue().getMessage(), containsString("deliberate"));
    }
  }

  @Test
  void testHeartbeating() throws Exception {
    connectionBuilder.heartbeatMillis(250L);
    try (AtClientConnection connection = connectionBuilder.build()) {
      await().until(() -> server.peek() != null);
      assertThat(server.poll(), equalTo("noop:0"));
      await().until(() -> server.peek() != null);
      assertThat(server.poll(), equalTo("noop:0"));
    }
  }

  private class TestEndPointSupplier implements AtEndpointSupplier {

    int invocationCount;

    @Override
    public String get() {
      invocationCount++;
      return "localhost:" + server.getPort();
    }
  }

  private static final class TestReconnectStrategy extends SimpleReconnectStrategy {

    boolean reconnectNoLimit;
    Throwable connectException;
    int connectFailCount;
    Throwable disconnectException;
    int disconnectCount;

    @Builder(builderMethodName = "testBuilder")
    public TestReconnectStrategy(long maxReconnectRetries, int resolveEndpointFrequency, long reconnectPauseMillis) {
      super(maxReconnectRetries, resolveEndpointFrequency, reconnectPauseMillis);
    }

    @Override
    public void onConnectFailure(Throwable connectException) {
      connectFailCount++;
      this.connectException = connectException;
      super.onConnectFailure(connectException);
    }

    @Override
    public void onDisconnect(Throwable disconnectException) {
      disconnectCount++;
      this.disconnectException = disconnectException;
      super.onDisconnect(disconnectException);
    }

    @Override
    public boolean isReconnectSupported() {
      return reconnectNoLimit || super.isReconnectSupported();
    }

    public void reconnectNoLimit() {
      reconnectNoLimit = true;
    }
  }

  private static void stubTestServerConnectAndResponseBehaviour(TestServer server) {
    server.setRequestHandler(s -> testServerResponse(server, s));
  }

  private static void stubTestServerConnectAndAndAutomaticNotification(TestServer server) {
    server.setRequestHandler(s -> testServerResponseWithAutomaticNotification(server, s));
  }

  private static void stubTestServerConnectAndResponseBehaviourWithRequestLatch(TestServer server,
                                                                                CountDownLatch latch) {
    server.setRequestHandler(request -> {
      if (request != null) {
        awaitLatch(latch);
      }
      testServerResponse(server, request);
    });
  }

  private static void stubTestServerConnectAndResponseBehaviourWithConnectLatch(TestServer server,
                                                                                CountDownLatch latch) {
    server.setRequestHandler(request -> {
      if (request == null) {
        awaitLatch(latch);
      }
      testServerResponse(server, request);
    });
  }

  private static void stubTestServerToCloseClientSocketOnRequest(TestServer server) {
    stubTestServerToCloseClientSocketOnRequest(server, null, null, 0);
  }

  private static void stubTestServerToCloseClientSocketOnRequest(TestServer server,
                                                                 CountDownLatch latch,
                                                                 TestClock clock,
                                                                 long elapsedMillis) {
    AtomicInteger invocationCount = new AtomicInteger(0);
    server.setRequestHandler(request -> {
      if (request != null && invocationCount.incrementAndGet() == 1) {
        awaitLatch(latch);
        log.info("deliberately closing client socket as part of test");
        server.closeClientSocket();
        if (clock != null) {
          clock.advance(elapsedMillis);
        }
      } else {
        testServerResponse(server, request);
      }
    });
  }

  private static void awaitLatch(CountDownLatch latch) {
    if (latch != null) {
      try {
        latch.await();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static void testServerResponse(TestServer server, String command) {
    if (command == null) {
      // invoked on connect
      server.writeAndFlush("@");
    } else if (command.equals("request")) {
      server.writeAndFlush("response\n@");
    } else if (command.startsWith("request")) {
      String suffix = command.replace("request", "");
      server.writeAndFlush("response" + suffix + "\n@");
    }
  }

  private static void testServerResponseWithAutomaticNotification(TestServer server, String command) {
    if (command == null) {
      // invoked on connect
      server.writeAndFlush("@");
    } else if (command.equals("request")) {
      server.writeAndFlush("response\n@");
    } else if (command.startsWith("request")) {
      String suffix = command.replace("request", "");
      server.writeAndFlush("response" + suffix + "\n@");
    } else if (command.equals("monitor")) {
      server.writeAndFlush("notification:xyz\n");
    }
  }

  static class TestClock extends Clock {

    private final AtomicReference<Instant> instant = new AtomicReference<>();

    private final ZoneId zone;

    TestClock() {
      this.instant.set(Instant.now());
      this.zone = ZoneId.systemDefault();
    }

    void advance(long millis) {
      instant.set(instant.get().plus(Duration.ofMillis(millis)));
    }

    @Override
    public Instant instant() {
      return instant.get();
    }

    @Override
    public ZoneId getZone() {
      return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
