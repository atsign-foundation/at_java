package org.atsign.client.connection.netty;

import static org.atsign.common.AtSign.createAtSign;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.atsign.common.exceptions.AtSecondaryNotFoundException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NettyAtEndpointSupplierTest {

  private static TestServer TEST_SERVER;

  @BeforeAll
  public static void setupAll() throws Exception {
    TEST_SERVER = new TestServer();
  }

  @AfterAll
  public static void teardownAll() throws Exception {
    TEST_SERVER.close();
  }

  @BeforeEach
  public void setup() throws IOException {
    TEST_SERVER.setRequestHandler(s -> {
      if (s == null) {
        // invoked on connect
        TEST_SERVER.writeAndFlush("@");
      } else if (s.equals("colin")) {
        TEST_SERVER.writeAndFlush("host:60001\r\n@");
      } else {
        TEST_SERVER.writeAndFlush("null\r\n@");
      }
    });
    TEST_SERVER.reset();
  }

  @Test
  void testMandatoryFields() throws Exception {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                               () -> NettyAtEndpointSupplier.builder().build());
    assertThat(ex.getMessage(), containsString("atSign not set"));
    ex = assertThrows(IllegalArgumentException.class,
                      () -> NettyAtEndpointSupplier.builder().atsign(createAtSign("colin")).build());
    assertThat(ex.getMessage(), containsString("rootUrl not set"));
  }

  @Test
  void testConnectAndResolve() throws Exception {
    NettyAtEndpointSupplier provider = NettyAtEndpointSupplier.builder()
        .rootUrl("localhost:" + TEST_SERVER.getPort())
        .atsign(createAtSign("colin"))
        .sslContext(TEST_SERVER.getClientSslContext())
        .build();
    assertThat(provider.get(), equalTo("host:60001"));
  }

  @Test
  void testConnectAndResolveFail() throws Exception {
    NettyAtEndpointSupplier provider = NettyAtEndpointSupplier.builder()
        .rootUrl("localhost:" + TEST_SERVER.getPort())
        .atsign(createAtSign("gary"))
        .sslContext(TEST_SERVER.getClientSslContext())
        .build();
    AtSecondaryNotFoundException ex = assertThrows(AtSecondaryNotFoundException.class, () -> provider.get());
    assertThat(ex.getMessage(), containsString("unable to resolve the endpoint for @gary"));
  }

  @Test
  void testResolveFailIfUnableToConnect() throws Exception {
    TEST_SERVER.closeServerSocket();
    NettyAtEndpointSupplier provider = NettyAtEndpointSupplier.builder()
        .rootUrl("localhost:" + TEST_SERVER.getPort())
        .atsign(createAtSign("colin"))
        .sslContext(TEST_SERVER.getClientSslContext())
        .build();
    AtSecondaryNotFoundException ex = assertThrows(AtSecondaryNotFoundException.class, () -> provider.get());
    assertThat(ex.getMessage(), containsString("unable to resolve the endpoint for @colin"));
  }

  @Test
  void testResolveFailIfNoResponse() throws Exception {
    TEST_SERVER.setRequestHandler(s -> {
      if (s == null) {
        // invoked on connect
        TEST_SERVER.writeAndFlush("@");
      }
      // but no response for anything else
    });
    NettyAtEndpointSupplier provider = NettyAtEndpointSupplier.builder()
        .rootUrl("localhost:" + TEST_SERVER.getPort())
        .atsign(createAtSign("colin"))
        .sslContext(TEST_SERVER.getClientSslContext())
        .build();
    AtSecondaryNotFoundException ex = assertThrows(AtSecondaryNotFoundException.class, () -> provider.get());
    assertThat(ex.getMessage(), containsString("unable to resolve the endpoint for @colin"));
  }

}
