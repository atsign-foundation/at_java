package org.atsign.client.connection.netty;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.CharsetUtil;

/**
 *
 */
public final class NettyAtResponseDecoder extends ByteToMessageDecoder {

  private static final byte LF = (byte) '\n';
  private static final byte CR = (byte) '\r';
  private static final byte PROMPT = (byte) '@';
  private static final byte COLON = (byte) ':';

  private final long maxFrameSize;

  public NettyAtResponseDecoder(long maxFrameSize) {
    this.maxFrameSize = maxFrameSize;
  }

  @Override
  protected void decode(ChannelHandlerContext ctx,
                        ByteBuf in,
                        List<Object> out) {

    while (true) {

      if (in.readableBytes() > maxFrameSize) {
        throw new TooLongFrameException("buffer exceeds maxFrameSize [" + maxFrameSize + "]");
      }

      int i;
      if ((i = findPromptIndex(in, 0)) == 0) {
        int j = findPromptIndex(in, i + 1);
        if (j > i) {
          out.add(in.readCharSequence(j - i + 1, CharsetUtil.UTF_8).toString());
        } else if (in.readableBytes() == 1 || findLinefeedIndex(in) > i) {
          out.add(in.readCharSequence(1, CharsetUtil.UTF_8).toString());
        } else {
          // this is possibly an authenticated prompt which is fragmented
          return;
        }
      } else if ((i = findLinefeedIndex(in)) > -1) {
        int skip = 1;
        if (i > 0 && in.getByte(i - 1) == CR) {
          i--;
          skip++;
        }
        out.add(in.readCharSequence(i, CharsetUtil.UTF_8).toString());
        in.skipBytes(skip);
      } else {
        return;
      }
    }
  }

  private static int findLinefeedIndex(ByteBuf in) {
    for (int i = in.readerIndex(); i < in.writerIndex(); i++) {
      if (in.getByte(i) == LF) {
        return i - in.readerIndex();
      }
    }
    return -1;
  }

  private static int findPromptIndex(ByteBuf in, int offset) {
    for (int i = in.readerIndex() + offset; i < in.writerIndex(); i++) {
      if (in.getByte(i) == LF || in.getByte(i) == COLON) {
        return -1;
      }
      if (in.getByte(i) == PROMPT) {
        return i - in.readerIndex();
      }
    }
    return -1;
  }

}
