package org.atsign.client.connection.netty;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.atsign.client.util.Preconditions.checkNotNull;

import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestServer implements AutoCloseable {

  @Getter
  private int port;
  @Getter
  private final SSLContext clientSslContext;

  private final SSLServerSocketFactory factory;
  private volatile ServerSocket serverSocket;
  private volatile Socket socket;
  private volatile PrintWriter writer;
  private volatile BufferedReader reader;

  private final BlockingQueue<String> received = new LinkedBlockingQueue<>();

  @Setter
  private Consumer<String> requestHandler = x -> {
  };

  private AtomicBoolean isAccept = new AtomicBoolean(true);

  private AtomicBoolean shutdown = new AtomicBoolean();

  private final Thread workerThread;

  public TestServer() throws Exception {
    KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
    kpg.initialize(2048, new SecureRandom());
    KeyPair keyPair = kpg.generateKeyPair();
    X509Certificate cert = generateSelfSignedCert(keyPair);
    SSLContext serverCtx = buildServerSslContext(keyPair.getPrivate(), cert);
    factory = serverCtx.getServerSocketFactory();
    clientSslContext = buildClientSslContext(cert);
    newServerSocketNewPort();
    workerThread = new Thread(() -> {
      while (!shutdown.get()) {
        try {
          if (serverSocket != null && socket == null && isAccept.get()) {
            log.debug("accepting on {}", serverSocket.getLocalPort());
            socket = serverSocket.accept();
            writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), false);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            requestHandler.accept(null);
          } else if (socket != null) {
            try {
              String request = reader.readLine();
              if (request != null) {
                received.add(request);
                requestHandler.accept(request);
              } else {
                closeClientSocket();
              }
            } catch (Exception e) {
              closeClientSocket();
            }
          } else {
            if (serverSocket == null) {
              log.debug("sleeping (no server socket)...");
            } else if (!isAccept.get()) {
              log.debug("sleeping (server socket is not accepting)...");
            } else {
              log.debug("sleeping...");
            }
            sleepNoThrow(100, MILLISECONDS);
          }
        } catch (Exception e) {
          log.debug("unexpected exception", e);
        }
      }
    });
    workerThread.setDaemon(true);
    workerThread.start();
  }

  private static void sleepNoThrow(long duration, TimeUnit unit) {
    try {
      Thread.sleep(unit.toMillis(duration));
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public String poll() {
    return received.poll();
  }

  public String peek() {
    return received.peek();
  }

  public TestServer writeAndFlush(String... messages) {
    for (String message : messages) {
      try {
        checkNotNull(writer).print(message);
        writer.flush();
      } catch (Exception e) {
        closeClientSocket();
      }
    }
    return this;
  }

  public void closeClientSocket() {
    try {
      closeNoThrow(writer, reader, socket);
    } finally {
      socket = null;
    }
  }

  public void closeServerSocket() {
    try {
      closeNoThrow(serverSocket, socket, reader, writer);
    } finally {
      serverSocket = null;
      socket = null;
    }
  }

  public void accept() {
    boolean previous = isAccept.getAndSet(true);
    if (!previous) {
      log.info("accept is now unblocked");
    }
  }

  public void newServerSocket() {
    newServerSocket(true, true);
  }

  public void newServerSocketWithoutAccept() {
    newServerSocket(true, false);
  }

  public void newServerSocketNewPort() {
    newServerSocket(false, true);
  }

  protected void newServerSocket(boolean reusePort, boolean autoAccept) {
    if (serverSocket != null) {
      throw new IllegalStateException("the server socket has not been closed");
    }
    try {
      isAccept.set(autoAccept);
      serverSocket = factory.createServerSocket(reusePort ? port : 0);
      port = serverSocket.getLocalPort();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void reset() throws IOException {
    closeNoThrow(reader, writer, socket);
    socket = null;
    if (serverSocket == null) {
      newServerSocket(true, true);
    }
    received.clear();
  }


  @Override
  public void close() throws InterruptedException {
    shutdown.set(true);
    closeNoThrow(writer, reader, socket, serverSocket);
    workerThread.join();
  }

  private static void closeNoThrow(Closeable... closeables) {
    for (Closeable closeable : closeables) {
      if (closeable != null) {
        try {
          closeable.close();
        } catch (IOException e) {
        }
      }
    }
  }

  private static X509Certificate generateSelfSignedCert(KeyPair keyPair) throws Exception {
    Instant now = Instant.now();
    Instant expiry = now.plus(1, ChronoUnit.DAYS);

    X500Name subject = new X500Name("CN=MockSocketServer,O=Test,C=US");

    X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
        subject,
        BigInteger.valueOf(System.currentTimeMillis()),
        Date.from(now),
        Date.from(expiry),
        subject,
        keyPair.getPublic());

    ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA")
        .build(keyPair.getPrivate());

    return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
  }

  private static SSLContext buildServerSslContext(PrivateKey privateKey,
                                                  X509Certificate cert)
      throws Exception {
    KeyStore ks = KeyStore.getInstance("PKCS12");
    ks.load(null, null);
    ks.setKeyEntry("mock", privateKey, new char[0], new X509Certificate[] {cert});

    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(ks, new char[0]);

    SSLContext ctx = SSLContext.getInstance("TLS");
    ctx.init(kmf.getKeyManagers(), null, new SecureRandom());
    return ctx;
  }

  private static SSLContext buildClientSslContext(X509Certificate cert) throws Exception {
    KeyStore ts = KeyStore.getInstance("PKCS12");
    ts.load(null, null);
    ts.setCertificateEntry("mock-server", cert);
    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(ts);
    SSLContext ctx = SSLContext.getInstance("TLS");
    ctx.init(null, tmf.getTrustManagers(), new SecureRandom());
    return ctx;
  }
}
