package org.atsign.client.impl.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * A Netty encoder which ensures that commands are terminated by a newline
 */
public class NettyAtCommandEncoder extends MessageToByteEncoder<CharSequence> {

  private static final byte LF = (byte) '\n';

  @Override
  protected void encode(ChannelHandlerContext ctx, CharSequence msg, ByteBuf out) {
    ByteBufUtil.writeUtf8(out, msg);
    int len = msg.length();
    if (len == 0 || msg.charAt(len - 1) != '\n') {
      out.writeByte(LF);
    }
  }
}
