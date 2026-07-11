package org.atsign.client.impl.netty;

import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.atsign.client.impl.commands.DataResponses.matchDataStringNoWhitespace;
import static org.atsign.client.impl.commands.ErrorResponses.throwExceptionIfError;
import static org.atsign.client.impl.common.CommandElement.isPrompt;
import static org.atsign.client.impl.common.Preconditions.checkNotNull;

import java.io.IOException;
import java.time.Clock;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

import org.atsign.client.api.AtCommandExecutor;
import org.atsign.client.api.AtSign;
import org.atsign.client.impl.AtEndpointSupplier;
import org.atsign.client.impl.commands.CommandBuilders;
import org.atsign.client.impl.common.ReconnectStrategy;
import org.atsign.client.impl.common.CommandElement;
import org.atsign.client.impl.common.CommandQueue;
import org.atsign.client.impl.exceptions.AtException;
import org.atsign.client.impl.exceptions.AtOnReadyException;
import org.atsign.client.impl.exceptions.AtSecondaryConnectException;
import org.atsign.client.impl.exceptions.AtSecondaryNotFoundException;
import org.atsign.client.impl.exceptions.AtTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.JdkSslContext;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Builder;

/**
 * An implementation of {@link AtCommandExecutor} that uses Netty.
 */
public class NettyAtCommandExecutor implements AtCommandExecutor {

  public static final long DEFAULT_COMMAND_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(2);
  public static final long DEFAULT_HEARTBEAT_MILLIS = TimeUnit.SECONDS.toMillis(30);
  public static final int DEFAULT_MAX_FRAME_LENGTH = 10230000;
  public static final long DEFAULT_AWAIT_READY_MILLIS = TimeUnit.SECONDS.toMillis(1);
  private final long heartbeatMillis;

  private enum Status {

    Unconnected, Connected, Readying, Ready, Disconnected, Closing, Closed;

    boolean isClosedOrClosing() {
      return this == Closed || this == Closing;
    }

    String toLowerCase() {
      return name().toLowerCase();
    }
  }

  private final AtomicReference<Status> status = new AtomicReference<>(Status.Unconnected);

  private final Logger log;

  private final AtEndpointSupplier endpointSupplier;

  private final Clock clock;

  private final AtomicReference<String> endpoint = new AtomicReference<>();

  private final int maxFrameLength;
  private final SslContext sslContext;
  private final EventLoopGroup group;
  private final Bootstrap bootstrap;
  private final NettyAtThreadFactory threadFactory;
  private volatile Channel channel;

  private final ReconnectStrategy reconnectStrategy;

  private final CommandQueue pending;
  private final CommandQueue queue;

  private final AtomicReference<Consumer<AtCommandExecutor>> onReadyConsumer = new AtomicReference<>();

  private final long timeoutMillis;

  private final boolean isVerbose;

  private final AtomicReference<AtException> closeException = new AtomicReference<>();

  private final AtomicBoolean isForceReconnect = new AtomicBoolean();

  private final ReentrantLock readyingLock = new ReentrantLock();

  private volatile long lastReadMillis;

  private final AtSign atSign;

  private final Map<String, Object> clientConfig;

  private final AtomicReference<String> fromChallenge = new AtomicReference<>();

  /**
   * Builder method for instantiating instances of a Netty based implementation of
   * {@link AtCommandExecutor}
   *
   * @param endpoint A supplier that will be invoked prior to connection attempts, need to return
   *        host:port
   * @param maxFrameLength Optional, defaults to {@link #DEFAULT_MAX_FRAME_LENGTH}
   * @param sslContext Optional {@link SSLContext}, defaults to Netty default client context
   * @param reconnect Optional implementation of {@link ReconnectStrategy}, defaults to
   *        {@link ReconnectStrategy#NONE}
   * @param onReady Optional {@link Consumer} which will be invoked each time connection becomes
   *        ready. Useful for connection setup e.g. authentication
   * @param threadFactory Optional {@link ThreadFactory}, defaults to {@link DefaultThreadFactory}
   * @param timeoutMillis Optional timeout (in milliseconds) for each command that is sent. Defaults
   *        to {@link #DEFAULT_COMMAND_TIMEOUT_MILLIS}
   * @param heartbeatMillis Optional frequency which controls if and when the connection sends noop
   *        commands when there has been no other activity (command responses, notifications)
   * @param queueLimit Optional, defaults to 0. Defaults to {@link #DEFAULT_HEARTBEAT_MILLIS}.
   * @param clock Optional implementation of {@link Clock}. Defaults to system clock.
   * @param log Optional implementation of {@link Logger}. Defaults to classname.
   */
  @Builder
  protected NettyAtCommandExecutor(AtEndpointSupplier endpoint,
                                   Integer maxFrameLength,
                                   SSLContext sslContext,
                                   ReconnectStrategy reconnect,
                                   Consumer<AtCommandExecutor> onReady,
                                   ThreadFactory threadFactory,
                                   Long timeoutMillis,
                                   Long heartbeatMillis,
                                   Integer queueLimit,
                                   Long awaitReadyMillis,
                                   Boolean isVerbose,
                                   AtSign atSign,
                                   Map<String, Object> clientConfig,
                                   Clock clock,
                                   Logger log)
      throws AtException {
    this.atSign = atSign;
    this.clientConfig = clientConfig;
    this.endpointSupplier = checkNotNull(endpoint, "endpoint is not set");
    this.maxFrameLength = defaultIfUnset(maxFrameLength, DEFAULT_MAX_FRAME_LENGTH);
    this.reconnectStrategy = defaultIfNull(reconnect, ReconnectStrategy.NONE);
    this.onReadyConsumer.set(defaultIfNull(onReady, c -> {
    }));
    this.pending = new CommandQueue(1);
    this.queue = new CommandQueue(defaultIfUnset(queueLimit, pending.getQueueCapacity()));
    this.isVerbose = isVerbose != null && isVerbose;
    this.clock = defaultIfNull(clock, Clock.systemUTC());
    this.log = defaultIfNull(log, LoggerFactory.getLogger(getClass()));
    this.threadFactory = new NettyAtThreadFactory(threadFactory);
    // DO NOT increase the thread count, this is by design and is crucial to the thread safety
    this.group = new NioEventLoopGroup(1, this.threadFactory);
    this.sslContext = sslContext != null ? wrapSslContext(sslContext) : createDefaultSslContext();
    this.bootstrap = new Bootstrap()
        .group(group)
        .channel(NioSocketChannel.class)
        .option(ChannelOption.SO_KEEPALIVE, true)
        .handler(new SocketChannelChannelInitializer());

    this.timeoutMillis = defaultIfUnset(timeoutMillis, DEFAULT_COMMAND_TIMEOUT_MILLIS);
    group.scheduleAtFixedRate(this::checkForTimeouts, this.timeoutMillis, this.timeoutMillis, MILLISECONDS);

    this.heartbeatMillis = defaultIfUnset(heartbeatMillis, DEFAULT_HEARTBEAT_MILLIS);
    group.scheduleAtFixedRate(this::sendHeartbeat, this.heartbeatMillis, this.heartbeatMillis, MILLISECONDS);

    try {
      connect().get(this.timeoutMillis, MILLISECONDS);
      awaitStatus(Status.Ready, defaultIfUnset(awaitReadyMillis, DEFAULT_AWAIT_READY_MILLIS));
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      if (!reconnectStrategy.isReconnectSupported()) {
        throw new AtSecondaryConnectException("connect failed (and retry is false) : " + e.getMessage(), e);
      }
    }
    if (closeException.get() != null) {
      throw closeException.get();
    }
  }

  /**
   * A builder for instantiating {@link AtCommandExecutor} implementations that are included in
   * this library. Example usage:
   *
   * <pre>
   *
   * NettyAtCommandExecutor.builder()
   *   .endpoint(...)          // an endpoint supplier
   *   .maxFrameLength(...)    // maximum message size supported (optional)
   *   .sslContext(..)         // (optional)
   *   .reconnect(...)         // a ReconnectStrategy (optional)
   *   .onReady(...)           // commands to run when first prompt is received
   *   .threadFactory(...)     // (optional)
   *   .timeoutMillis()        // timeout after which commands will complete exceptionally (optional)
   *   .heartbeatMillis(...)   // frequency for noop command (optional)
   *   .queueLimit(...)        // max number of commands to queue (default 0 i.e. no queuing)
   *   .awaitReadyMillis(...)  // how long to wait for executor to become ready during build() (optional)
   *   .isVerbose(...)         // defaults to false
   *   .build();
   * }
   * </pre>
   *
   * If <b>maxFrameLength</b> is not set then the builder defaults to
   * {@link #DEFAULT_MAX_FRAME_LENGTH}.
   * If <b>reconnect</b> is not set then the builder will default to a {@link ReconnectStrategy#NONE}
   * which will never retry to connect or reconnect after disconnect.
   * If <b>timeoutMillis</b> is not set then builder will default to
   * {@link #DEFAULT_COMMAND_TIMEOUT_MILLIS}.
   * If <b>heartbeatMillis</b> is not set then builder will default to
   * {@link #DEFAULT_HEARTBEAT_MILLIS}.
   * If <b>awaitReadyMillis</b> is not set then the builder will default to
   * {@link #DEFAULT_AWAIT_READY_MILLIS}.
   */
  public static class NettyAtCommandExecutorBuilder {
    // required for javadoc
  }

  @Override
  public void send(String command, CompletableFuture<String> future) {
    if (threadFactory.isCurrentThreadOnReadyThread()) {
      throw new AtOnReadyException("onReady is prohibited from invoking send, use sendSync");
    } else if (threadFactory.isCurrentThreadMyThread()) {
      throw new RuntimeException("send on netty event thread is prohibited");
    } else {
      group.execute(() -> send(new CommandElement(command, future, clock.millis(), false)));
    }
  }

  @Override
  public String sendSync(String command) throws ExecutionException, InterruptedException {
    CompletableFuture<String> future = new CompletableFuture<>();
    if (threadFactory.isCurrentThreadOnReadyThread()) {
      send(new CommandElement(command, future, clock.millis(), true));
    } else if (threadFactory.isCurrentThreadMyThread()) {
      throw new ExecutionException("send on netty event thread is prohibited", null);
    } else {
      group.execute(() -> send(new CommandElement(command, future, clock.millis(), false)));
    }
    return future.get();
  }

  @Override
  public void send(String command, Consumer<String> consumer, CompletableFuture<Void> future) {
    if (threadFactory.isCurrentThreadOnReadyThread()) {
      throw new AtOnReadyException("onReady is prohibited from invoking send, use sendSync");
    } else if (threadFactory.isCurrentThreadMyThread()) {
      throw new RuntimeException("send on netty event thread is prohibited");
    } else {
      group.execute(() -> send(new CommandElement(command, future, consumer, clock.millis(), false)));
    }
  }

  @Override
  public void sendSync(String command, Consumer<String> consumer) throws ExecutionException, InterruptedException {
    CompletableFuture<Void> future = new CompletableFuture<>();
    if (threadFactory.isCurrentThreadOnReadyThread()) {
      send(new CommandElement(command, future, consumer, clock.millis(), true));
    } else if (threadFactory.isCurrentThreadMyThread()) {
      throw new ExecutionException("send on netty event thread is prohibited", null);
    } else {
      group.execute(() -> send(new CommandElement(command, future, consumer, clock.millis(), true)));
    }
    future.get();
  }

  @Override
  public AtCommandExecutor onReady(Consumer<AtCommandExecutor> consumer) {
    checkNotNull(consumer, "null");
    try {
      readyingLock.lock();
      onReadyConsumer.set(consumer);
      forceReconnect();
    } finally {
      readyingLock.unlock();
    }
    return this;
  }

  @Override
  public void close() {
    if (!status.get().isClosedOrClosing()) {
      close(new AtSecondaryConnectException("connection closed"));
    }
  }

  public int getPendingSize() {
    return pending.size();
  }

  public int getQueuedSize() {
    return queue.size();
  }

  public boolean isReady() {
    return status.get() == Status.Ready;
  }

  public void forceReconnect() {
    if (channel != null) {
      isForceReconnect.set(true);
      channel.close();
    }
  }

  private void close(AtException closeException) {
    this.closeException.set(closeException);
    status.set(Status.Closing);
    if (channel != null) {
      channel.close();
    }
    group.shutdownGracefully();
    if (!pending.isEmpty() || !queue.isEmpty()) {
      pending.forEach(command -> command.completeExceptionally(closeException));
      queue.forEach(command -> command.completeExceptionally(closeException));
    }
    status.set(Status.Closed);
  }

  private Future<Void> connect() {
    try {
      resolveEndpoint();
    } catch (Exception e) {
      log.error("unabled to resolve endpoint");
      scheduleReconnect();
      return failedFuture(new RuntimeException("unable to resolve endpoint : " + e.getMessage(), e));
    }
    String endpoint = this.endpoint.get();
    log.debug("bootstrap.connect({}, {})", toHost(endpoint), toPort(endpoint));
    ChannelFuture channelFuture = bootstrap.connect(toHost(endpoint), toPort(endpoint));
    channelFuture.addListener((ChannelFutureListener) this::onConnect);
    return channelFuture;
  }

  private void onConnect(ChannelFuture future) {
    if (future.isSuccess()) {
      channel = future.channel();
      log.info("connected to {}", endpoint);
      status.set(Status.Connected);
      reconnectStrategy.onConnect();
    } else {
      log.warn("connect to {} failed : {}", endpoint, future.cause().getMessage());
      reconnectStrategy.onConnectFailure(future.cause());
      scheduleReconnect();
    }
  }

  private void checkForTimeouts() {
    checkForTimeout(queue, this.timeoutMillis, "timed out (in queue)");
    checkForTimeout(pending, this.timeoutMillis, "timed out (no response from server)");
  }

  private void checkForTimeout(CommandQueue commands, long timeoutMillis, String message) {
    Collection<CommandElement> expired = commands.pollTimedOut(clock.millis() - timeoutMillis);
    if (!expired.isEmpty()) {
      AtTimeoutException ex = new AtTimeoutException(message);
      expired.forEach(x -> x.completeExceptionally(ex));
      if (commands == pending && sendNext()) {
        log.debug("sent queued command");
      }
    }
  }

  private void sendHeartbeat() {
    if (isReady() && (clock.millis() - lastReadMillis) > heartbeatMillis) {
      writeAndFlushCommand(new CommandElement("noop:0", null, 0, false));
    }
  }

  private void completeIsOnReadyExceptionally(CommandQueue commands, String message) {
    Collection<CommandElement> isOnReadyCommands = commands.pollIsOnReady();
    if (!isOnReadyCommands.isEmpty()) {
      AtTimeoutException ex = new AtTimeoutException(message);
      isOnReadyCommands.forEach(x -> x.completeExceptionally(ex));
    }
  }

  private void resolveEndpoint() throws AtSecondaryNotFoundException {
    if (endpoint.get() == null || reconnectStrategy.isReresolveEndpoint()) {
      endpoint.set(endpointSupplier.get());
    }
  }

  private void scheduleReconnect() {
    if (status.get() == Status.Closed) {
      return;
    }
    boolean isForceReconnect = this.isForceReconnect.getAndSet(false);
    if (isForceReconnect || reconnectStrategy.isReconnectSupported()) {
      pending.removeIf(CommandElement::isOnReady);
      log.debug("re-queued {} pending commands", queue.drain(pending));
      pending.clear();
      long delayMillis = isForceReconnect ? 0 : reconnectStrategy.getReconnectPauseMillis();
      log.debug("scheduling connect");
      group.schedule(this::connect, delayMillis, MILLISECONDS);
    } else {
      log.warn("reconnect retries exceeded, closing connection");
      close(new AtSecondaryConnectException("reconnect retries exceeded"));
    }
  }

  private void send(CommandElement command) {
    if (status.get().isClosedOrClosing()) {
      command.completeExceptionally(new IllegalStateException("connection " + status.get().toLowerCase()));
    } else if ((isReady() || threadFactory.isCurrentThreadOnReadyThread()) && pending.offer(command)) {
      writeAndFlushCommand(command);
    } else if (queue.offer(command)) {
      log.debug("{} command{} queued ({})", queue.size(), queue.size() > 1 ? "s" : "", getPendingStatus());
    } else {
      command.completeExceptionally(new AtTimeoutException(
          queue.getQueueCapacity() > 0 ? "queue is full" : "queue not enabled"));
    }
  }

  private String getPendingStatus() {
    if (!pending.isEmpty()) {
      return pending.size() == 1 ? "pending command" : pending.size() + " pending commands";
    } else {
      return status.get().toLowerCase();
    }
  }

  private void writeAndFlushCommand(CommandElement command) {
    if (command != null) {
      ChannelFuture future = channel.writeAndFlush(command.toString());
      future.addListener((ChannelFutureListener) f -> onCommandWrite(command, f));
    }
  }

  private void onCommandWrite(CommandElement command, ChannelFuture future) {
    if (future.isSuccess()) {
      if (isVerbose) {
        log.info("SENT: {}", command);
      } else {
        log.debug("SENT: {}", command);
      }
      if (command.isConsumerCommand()) {
        // hack, until change which sends prompt after monitor
        group.schedule(this::onPrompt, 100, MILLISECONDS);
      }
    }
  }

  private boolean sendNext() {
    CommandElement command = queue.poll();
    if (command != null) {
      send(command);
      return true;
    } else {
      return false;
    }
  }

  private void onPrompt() {
    if (status.get() == Status.Connected) {
      log.debug("received prompt, readying...");
      status.set(Status.Readying);
      threadFactory.newThread(createOnReadyRunnable()).start();
    }
    CommandElement command = pending.pop("@");
    if (command != null) {
      command.complete(null);
    }
  }

  private Runnable createOnReadyRunnable() {
    return () -> {
      threadFactory.markCurrentThreadOnReadyThread();
      try {
        readyingLock.lock();
        sendFromIfRequired();
        onReadyConsumer.get().accept(this);
      } catch (AtOnReadyException e) {
        log.error("onReady exception", e);
        close(new AtSecondaryConnectException(e.getMessage()));
        return;
      } finally {
        readyingLock.unlock();
      }
      log.debug("ready");
      status.set(Status.Ready);
      checkForTimeouts();
      if (sendNext()) {
        log.debug("sent queued command");
      }
      threadFactory.clearCurrentThreadOnReadyThread();
    };
  }

  /**
   * Sends {@code from:@atSign} as the first command once the connection is ready, so the
   * connection's atSign is established before any other verb (including a {@code scan} sent by
   * an onReady consumer prior to authenticating). The challenge returned by the server is
   * retained so that CRAM / PKAM authentication can reuse it instead of issuing a second
   * {@code from:}. When no atSign was supplied to the builder this is a no-op and
   * {@link #getFromChallenge()} returns {@code null}.
   *
   * <p>
   * Runs on the onReady thread, where {@link #sendSync(String)} is permitted. On reconnect
   * the ready sequence re-runs, so the challenge is refreshed on each connect.
   */
  private void sendFromIfRequired() {
    if (atSign == null) {
      return;
    }
    try {
      String fromCommand = CommandBuilders.fromCommandBuilder()
          .atSign(atSign)
          .config(clientConfig)
          .build();
      String fromResponse = sendSync(fromCommand);
      String challenge = matchDataStringNoWhitespace(throwExceptionIfError(fromResponse));
      fromChallenge.set(challenge);
    } catch (AtException | ExecutionException | InterruptedException | RuntimeException e) {
      throw new AtOnReadyException("from command failed : " + e.getMessage(), e);
    }
  }

  /**
   * Returns the challenge from the initial {@code from:} and clears it, so it is consumed at most
   * once. The server's {@code from:} challenge is single-use — whichever authentication (CRAM or
   * PKAM) sends its digest first consumes it — so a second authentication on the same connection
   * (e.g. onboarding, which does CRAM then PKAM) must issue its own {@code from:} and gets
   * {@code null} here to signal that fallback. The challenge is also cleared on disconnect —
   * it is only valid for the server session that issued it — so a caller can never consume a
   * challenge from a connection that has since dropped.
   */
  @Override
  public String getFromChallenge() {
    return fromChallenge.getAndSet(null);
  }

  private void onResponse(String msg) {
    if (isVerbose) {
      log.info("RCVD: {}", msg);
    } else {
      log.debug("RCVD: {}", msg);
    }

    CommandElement command = pending.pop(msg);
    if (command != null) {
      command.complete(msg);
    } else {
      log.warn("no pending command : {}", msg);
    }
    if (sendNext()) {
      log.debug("sent queued command");
    }
  }

  private class ResponseHandler extends SimpleChannelInboundHandler<String> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
      lastReadMillis = clock.millis();
      if (isPrompt(msg)) {
        onPrompt();
      } else if (!isHeartbeat(msg)) {
        onResponse(msg);
      }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
      log.error("exception in handler", cause);
    }

    @Override
    public void channelInactive(ChannelHandlerContext context) {
      channel = null;
      // a from: challenge is only valid for the server session that just ended
      fromChallenge.set(null);
      if (status.get().isClosedOrClosing()) {
        log.debug("connection closed");
      } else {
        completeIsOnReadyExceptionally(pending, "connection closed");
        if (isForceReconnect.get()) {
          log.debug("connection force disconnected");
        } else {
          log.warn("connection unexpectedly unconnected");
          reconnectStrategy.onDisconnect(new IOException("connection closed"));
        }
        status.set(Status.Unconnected);
        scheduleReconnect();
      }
    }
  }

  private static int toPort(String hostAndPort) {
    return Integer.parseInt(hostAndPort.split(":")[1]);
  }

  private static String toHost(String hostAndPort) {
    return hostAndPort.split(":")[0];
  }

  private class SocketChannelChannelInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) {
      String endpoint = NettyAtCommandExecutor.this.endpoint.get();
      ChannelPipeline pipeline = ch.pipeline();
      pipeline.addLast(sslContext.newHandler(ch.alloc(), toHost(endpoint), toPort(endpoint)));
      pipeline.addLast(new NettyAtResponseDecoder(maxFrameLength));
      pipeline.addLast(new NettyAtCommandEncoder());
      pipeline.addLast(new ResponseHandler());
    }
  }

  private static SslContext createDefaultSslContext() {
    try {
      return SslContextBuilder.forClient().build();
    } catch (SSLException e) {
      throw new RuntimeException("failed to create SSL context", e);
    }
  }

  private static SslContext wrapSslContext(SSLContext context) {
    return new JdkSslContext(context, true, ClientAuth.NONE);
  }

  private void awaitStatus(Status status, long timeoutMillis) throws InterruptedException {
    long startMillis = clock.millis();
    while (clock.millis() < (startMillis + timeoutMillis)) {
      Thread.sleep(100);
      if (this.status.get() == status) {
        break;
      }
    }
  }

  private static boolean isHeartbeat(String s) {
    return "data:ok".equals(s);
  }

  private static <T> T defaultIfNull(T o, T defaultValue) {
    return o != null ? o : defaultValue;
  }

  private static long defaultIfUnset(Long l, long defaultValue) {
    return l != null ? l : defaultValue;
  }

  private static int defaultIfUnset(Integer i, int defaultValue) {
    return i != null ? i : defaultValue;
  }
}
