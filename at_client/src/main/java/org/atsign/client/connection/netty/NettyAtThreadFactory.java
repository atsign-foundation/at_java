package org.atsign.client.connection.netty;

import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.ThreadFactory;

/**
 * Wraps a thread factory (or the default netty thread factory) with one that sets thread locals.
 * This way we can detect when a command is invoked as part of OnReady and prevent
 * netty event thread from sending a command and then blocking on itself
 */
class NettyAtThreadFactory implements ThreadFactory {

  private final ThreadLocal<Boolean> isMyThread = ThreadLocal.withInitial(() -> false);

  private final ThreadLocal<Boolean> isOnReadyThread = ThreadLocal.withInitial(() -> false);

  private final ThreadFactory delegate;

  public NettyAtThreadFactory(ThreadFactory delegate) {
    this.delegate = delegate != null ? delegate : new DefaultThreadFactory("netty");
  }

  @Override
  public Thread newThread(Runnable r) {
    return delegate.newThread(() -> {
      isMyThread.set(true);
      r.run();
    });
  }

  boolean isCurrentThreadMyThread() {
    return isMyThread.get();
  }

  public boolean isCurrentThreadOnReadyThread() {
    return isOnReadyThread.get();
  }

  public void markCurrentThreadOnReadyThread() {
    isOnReadyThread.set(true);
    Thread.currentThread().setName(Thread.currentThread().getName() + "(ready)");
  }

  public void clearCurrentThreadOnReadyThread() {
    isOnReadyThread.set(null);
    Thread.currentThread().setName(Thread.currentThread().getName().replace("(ready)", ""));
  }
}
