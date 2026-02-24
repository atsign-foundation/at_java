package org.atsign.client.connection.netty;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.CharsetUtil;

class NettyAtServerResponseDecoderTest {

  private EmbeddedChannel channel;

  @BeforeEach
  public void setup() {
    channel = new EmbeddedChannel(new NettyAtResponseDecoder(1024));
  }

  @Test
  void testDecodeInitialPrompt() {
    writeInbound(channel, "@");
    assertReadInbound(channel, equalTo("@"));
  }

  @Test
  void testDecodeAuthenticatedPrompt() {
    writeInbound(channel, "@gary@");
    assertReadInbound(channel, equalTo("@gary@"));
  }

  @Test
  void testDecodeResponse() {
    writeInbound(channel, "data:success\n");
    assertReadInbound(channel, equalTo("data:success"));
  }

  @Test
  void testDecodeResponseWithCrlf() {
    writeInbound(channel, "data:success\r\n");
    assertReadInbound(channel, equalTo("data:success"));
  }

  @Test
  void testDecodeResponseAndPrompt() {
    writeInbound(channel, "data:success\n@");
    assertReadInbound(channel, equalTo("data:success"), equalTo("@"));
  }

  @Test
  void testDecodeResponseAndAuthenticatedPrompt() {
    writeInbound(channel, "data:success\n@gary@");
    assertReadInbound(channel, equalTo("data:success"), equalTo("@gary@"));
  }

  @Test
  void testDecodeSequence() {
    writeInbound(channel, "@data:_183dece2-5cb6\n@data:success\n@gary@");
    assertReadInbound(channel, equalTo("@"),
                      equalTo("data:_183dece2-5cb6"),
                      equalTo("@"),
                      equalTo("data:success"),
                      equalTo("@gary@"));
  }

  @Test
  void testDecodeSequenceWhereTheResponseIsFragmented() {
    writeInbound(channel, "@data:_183dece2");
    writeInbound(channel, "-5cb6\n@data:success\n@gary@");
    assertReadInbound(channel, equalTo("@"),
                      equalTo("data:_183dece2-5cb6"),
                      equalTo("@"),
                      equalTo("data:success"),
                      equalTo("@gary@"));
  }

  @Test
  void testDecodeSequenceWhereTheAuthenticatedPromptIsFragmented() {
    writeInbound(channel, "@data:_183dece2-5cb6\n@data:success\n@gar");
    writeInbound(channel, "y@");
    assertReadInbound(channel, equalTo("@"),
                      equalTo("data:_183dece2-5cb6"),
                      equalTo("@"),
                      equalTo("data:success"),
                      equalTo("@gary@"));
  }

  @Test
  void testDecodeSequenceWhereResponseContainsAnAtSymbol() {
    writeInbound(channel, "@data:[\"@gary:signing_privatekey@gary\"" +
        ",\"public:pkaminstalled@gary\"]\n@");
    assertReadInbound(channel, equalTo("@"),
                      equalTo("data:[\"@gary:signing_privatekey@gary\",\"public:pkaminstalled@gary\"]"),
                      equalTo("@"));
  }

  @Test
  void testDecodeSequenceWhereResponseContainsAnAtSymbolAndThePromptIsAuthenticated() {
    writeInbound(channel, "@gary@data:[\"@gary:signing_privatekey@gary\"" +
        ",\"public:pkaminstalled@gary\"]\n@gary@");
    assertReadInbound(channel, equalTo("@gary@"),
                      equalTo("data:[\"@gary:signing_privatekey@gary\",\"public:pkaminstalled@gary\"]"),
                      equalTo("@gary@"));
  }


  @Test
  void testDecodeSequenceWhereResponseContainsAnAtSymbolAndThePromptIsAuthenticatedAndFragmented() {
    writeInbound(channel, "@gar");
    writeInbound(channel, "y@data");
    writeInbound(channel, ":[\"@gary:signing_privatekey");
    writeInbound(channel, "@gary\",\"public:pkaminstalled@gary\"]\n@gary@");
    assertReadInbound(channel, equalTo("@gary@"),
                      equalTo("data:[\"@gary:signing_privatekey@gary\",\"public:pkaminstalled@gary\"]"),
                      equalTo("@gary@"));
  }

  @Test
  void testMaxFrameSize() {
    EmbeddedChannel channel = new EmbeddedChannel(new NettyAtResponseDecoder(10));
    writeInbound(channel, "0123456789");
    Exception ex = assertThrows(Exception.class, () -> writeInbound(channel, "1"));
    assertThat(ex.getMessage(), containsString("buffer exceeds maxFrameSize [10]"));
  }

  private static void writeInbound(EmbeddedChannel channel, String s) {
    channel.writeInbound(Unpooled.copiedBuffer(s, CharsetUtil.UTF_8));
  }

  private static void assertReadInbound(EmbeddedChannel channel, Matcher<String>... matchers) {
    for (Matcher<String> matcher : matchers) {
      assertThat(channel.readInbound(), matcher);
    }
    assertThat(channel.readInbound(), nullValue());
  }
}
