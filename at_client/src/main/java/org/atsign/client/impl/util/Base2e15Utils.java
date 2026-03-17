package org.atsign.client.impl.util;

import static java.lang.String.format;
import static org.atsign.client.impl.common.Preconditions.checkNotNull;

import lombok.Builder;
import lombok.Value;


/**
 * Base2e15 - Binary-to-text encoding using Unicode characters.
 * Each character encodes 15 bits of data, except the last character which
 * encodes either 7 bits (partial byte) or 15 bits (full).<br>
 * 15-bit values are encoded into the following Unicode code point ranges<br>
 * {@code 0x0000 - 0x1935 -> U+3480 - U+4DB5} (CJK Unified Ideographs Extension A)<br>
 * {@code 0x1936 - 0x545B -> U+4E00 - U+8925} (CJK Unified Ideographs)<br>
 * {@code 0x545C - 0x7FFF -> U+AC00 - U+D7A3} (Hangul Syllables)<br>
 * 7-bit values are encoded into the following Unicode codepoint range<br>
 * {@code 0x00 - 0x7F -> U+3400 - U+347F} (CJK Unified Ideographs Extension A)
 *
 */
public class Base2e15Utils {

  private static final int MASK_15_BITS = 0x7FFF;

  private static final int MASK_7_BITS = 0x7F;

  private static final BitsToCodePointMapper MAPPER15 = new CompositeRange(
      MASK_15_BITS,
      Range.builder()
          .bitsStart(0x0000).bitsEnd(0x1935)
          .codePointsStart(0x3480).codePointsEnd(0x4DB5)
          .build(),
      Range.builder()
          .bitsStart(0x1936).bitsEnd(0x545B)
          .codePointsStart(0x4E00).codePointsEnd(0x8925)
          .build(),
      Range.builder()
          .bitsStart(0x545C).bitsEnd(0x7FFF)
          .codePointsStart(0xAC00).codePointsEnd(0xD7A3)
          .build());

  private static final BitsToCodePointMapper MAPPER7 =
      Range.builder()
          .mask(MASK_7_BITS)
          .bitsStart(0x00).bitsEnd(0x7F)
          .codePointsStart(0x3400).codePointsEnd(0x347F)
          .build();

  private static final int BYTE_LENGTH = 8;

  private static final int MASK_BYTE = 0xFF;

  private static final byte[] EMPTY_BYTES = new byte[0];
  private static final String EMPTY_STRING = "";

  public static String encode(byte[] bytes) {
    if (checkNotNull(bytes).length == 0) {
      return EMPTY_STRING;
    }

    int bytesLength = bytes.length;
    int buffer = 0;
    int bitCount = 0;
    int[] codePoints = new int[(bytesLength * BYTE_LENGTH + 14) / 15];

    int codePointsIndex = 0;
    for (int i = 0; i < bytesLength; i++) {
      int b = bytes[i] & MASK_BYTE;
      buffer = (buffer << BYTE_LENGTH) | b;
      bitCount = bitCount + BYTE_LENGTH;
      if (bitCount >= 15) {
        bitCount = bitCount - 15;
        codePoints[codePointsIndex++] = MAPPER15.toCodePoint(buffer >> bitCount);
      }
    }

    if (bitCount > 0) {
      if (bitCount <= 7) {
        codePoints[codePointsIndex++] = MAPPER7.toCodePoint(buffer << (7 - bitCount));
      } else {
        codePoints[codePointsIndex++] = MAPPER15.toCodePoint(buffer << (15 - bitCount));
      }
    }

    return new String(codePoints, 0, codePointsIndex);
  }

  public static byte[] decode(String encoded) {
    if (checkNotNull(encoded).isEmpty()) {
      return EMPTY_BYTES;
    }
    int[] codePoints = encoded.codePoints().toArray();
    int codePointsLength = codePoints.length;
    int totalBits =
        (codePointsLength - 1) * 15 + (MAPPER7.isCodePointInRange(codePoints[codePointsLength - 1]) ? 7 : 15);
    byte[] bytes = new byte[totalBits / BYTE_LENGTH];
    int bitCount = 0;
    int buffer = 0;
    int lastIndex = codePointsLength - 1;
    int codePoint;
    int codePointBits;

    int bytesIndex = 0;
    for (int i = 0; i < codePointsLength; i++) {
      int cp = codePoints[i];
      if (i == lastIndex && (codePoint = MAPPER7.toBits(cp)) >= 0) {
        codePointBits = 7;
      } else if ((codePoint = MAPPER15.toBits(cp)) >= 0) {
        codePointBits = 15;
      } else {
        throw new IllegalArgumentException(format("U+%04X at index %d is not within 15 or 7 bit range", cp, i));
      }
      buffer = (buffer << codePointBits) | codePoint;
      bitCount = bitCount + codePointBits;

      while (bitCount >= BYTE_LENGTH) {
        bitCount = bitCount - BYTE_LENGTH;
        bytes[bytesIndex++] = (byte) ((buffer >> bitCount) & MASK_BYTE);
      }
    }
    checkRemainingBufferIsEmpty(buffer, bitCount);
    return bytes;
  }

  private static void checkRemainingBufferIsEmpty(int buffer, int bitCount) {
    if (bitCount > 0) {
      int remaining = buffer & ((1 << bitCount) - 1);
      if (remaining != 0) {
        throw new IllegalStateException("residual buffer is not empty");
      }
    }
  }

  private interface BitsToCodePointMapper {
    int toCodePoint(int bits);

    boolean isCodePointInRange(int codePoint);

    int toBits(int codePoint);
  }

  @Value
  private static class Range implements BitsToCodePointMapper {

    int mask;
    int bitsStart;
    int bitsEnd;
    int codePointsStart;
    int codePointsEnd;
    int offset;

    @Builder
    Range(int mask, int bitsStart, int bitsEnd, int codePointsStart, int codePointsEnd) {
      this.mask = mask;
      this.bitsStart = bitsStart;
      this.bitsEnd = bitsEnd;
      this.codePointsStart = codePointsStart;
      this.codePointsEnd = codePointsEnd;
      this.offset = codePointsStart - bitsStart;
    }

    public boolean isBitsInRange(int bits) {
      return bits >= bitsStart && bits <= bitsEnd;
    }

    public int toCodePoint(int bits) {
      bits = bits & mask;
      if (isBitsInRange(bits)) {
        return _toCodePoint(bits);
      } else {
        throw new IllegalArgumentException(bits + " are not withing this mapping range");
      }
    }

    private int _toCodePoint(int bits) {
      return offset + bits;
    }

    public boolean isCodePointInRange(int codePoint) {
      return codePoint >= codePointsStart && codePoint <= codePointsEnd;
    }

    public int toBits(int codePoint) {
      if (isCodePointInRange(codePoint)) {
        return _toBits(codePoint);
      } else {
        return -1;
      }
    }

    private int _toBits(int codePoint) {
      return codePoint - offset;
    }
  }

  private static class CompositeRange implements BitsToCodePointMapper {

    private final int mask;
    private final Range[] ranges;

    CompositeRange(int mask, Range... ranges) {
      this.mask = mask;
      this.ranges = ranges;
    }

    @Override
    public int toCodePoint(int bits) {
      bits = bits & mask;
      for (int i = 0; i < ranges.length; i++) {
        if (ranges[i].isBitsInRange(bits)) {
          return ranges[i]._toCodePoint(bits);
        }
      }
      throw new IllegalArgumentException(bits + " are not withing this mapping range");
    }

    @Override
    public boolean isCodePointInRange(int codePoint) {
      for (int i = 0; i < ranges.length; i++) {
        if (ranges[i].isCodePointInRange(codePoint)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public int toBits(int codePoint) {
      for (int i = 0; i < ranges.length; i++) {
        if (ranges[i].isCodePointInRange(codePoint)) {
          return ranges[i]._toBits(codePoint);
        }
      }
      return -1;
    }
  }

}
