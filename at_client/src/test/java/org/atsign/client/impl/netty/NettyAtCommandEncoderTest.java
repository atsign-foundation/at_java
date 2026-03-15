package org.atsign.client.impl.netty;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;

class NettyAtCommandEncoderTest {

  private EmbeddedChannel channel;

  @BeforeEach
  public void setup() {
    channel = new EmbeddedChannel(new NettyAtCommandEncoder());
  }

  @Test
  public void testEncode() {
    channel.writeOutbound("scan");
    byte[] expected = new byte[] {'s', 'c', 'a', 'n', '\n'};
    assertMatch(channel.readOutbound(), expected);
  }

  @Test
  public void testEncodeEmptyString() {
    channel.writeOutbound("");
    byte[] expected = new byte[] {'\n'};
    assertMatch(channel.readOutbound(), expected);
  }

  @Test
  public void testEncodeWithNewline() {
    channel.writeOutbound("scan\n");
    byte[] expected = new byte[] {'s', 'c', 'a', 'n', '\n'};
    assertMatch(channel.readOutbound(), expected);
  }

  private static void assertMatch(ByteBuf buf, byte[] expected) {
    assertThat(buf.readableBytes(), equalTo(expected.length));
    for (int i = 0; i < expected.length; i++) {
      assertThat(buf.getByte(i), equalTo(expected[i]));
    }
  }

}
