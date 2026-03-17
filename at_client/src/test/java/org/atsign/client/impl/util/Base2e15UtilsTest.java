package org.atsign.client.impl.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.Test;

class Base2e15UtilsTest {

  // these mirror the values used in the tests for the dart implementation
  public static final byte[] DART_TEST_BYTES = "Base2e15 is awesome!".getBytes(StandardCharsets.UTF_8);
  public static final String DART_TEST_ENCODED = "嗺둽嬖蟝巍媖疌켉溁닽壪";

  @Test
  void testEncodeMatchesDartTestResult() {
    assertThat(Base2e15Utils.encode(DART_TEST_BYTES), equalTo(DART_TEST_ENCODED));
  }

  @Test
  void testDecodeMatchesDartTestResult() {
    assertThat(Base2e15Utils.decode(DART_TEST_ENCODED), equalTo(DART_TEST_BYTES));
  }

  @Test
  void testDecodeOutsideUnicodeRangeThrowsException() {
    Exception ex = assertThrows(IllegalArgumentException.class, () -> Base2e15Utils.decode("嗺둽嬖蟝巍x疌켉溁닽壪"));
    assertThat(ex.getMessage(), containsString("U+0078 at index 5 is not within 15 or 7 bit range"));
  }

  @Test
  void testEncodeRepeatingSequenceOf15BytesGeneratesTheExpectedResult() {
    byte[] bytes = new byte[15];
    Arrays.fill(bytes, (byte) Integer.parseInt("10101010", 2));
    String encoded = Base2e15Utils.encode(bytes);

    assertThat(encoded.length(), equalTo(8));

    // first sequence of bits will fall in the 3rd base2e15 range
    int bits1 = Integer.parseInt("101010101010101", 2) & 0x7FFF;
    assertThat(bits1, equalTo(21845));
    int codePoint1 = bits1 + 0xAC00 - 0x545C;
    String unicode1 = new String(Character.toChars(codePoint1));

    // second sequence of bits will fall in the 2nd base2e15 range
    int bits2 = Integer.parseInt("010101010101010", 2) & 0x7FFF;
    assertThat(bits2, equalTo(10922));
    int codePoint2 = bits2 + 0x4E00 - 0x1936;
    String unicode2 = new String(Character.toChars(codePoint2));

    String expected = unicode1 + unicode2
        + unicode1 + unicode2
        + unicode1 + unicode2
        + unicode1 + unicode2;

    assertThat(encoded, equalTo(expected));

    assertThat(Base2e15Utils.decode(encoded), equalTo(bytes));
  }

  @Test
  void testEncodeRepeatingSequenceOf16BytesGeneratesTheExpectedResult() {
    byte[] bytes = new byte[16];
    Arrays.fill(bytes, (byte) Integer.parseInt("10101010", 2));
    String encoded = Base2e15Utils.encode(bytes);

    assertThat(encoded.length(), equalTo(9));

    // first sequence of bits will fall in the 3rd 15 bit range
    int bits1 = Integer.parseInt("101010101010101", 2) & 0x7FFF;
    assertThat(bits1, equalTo(21845));
    int codePoint1 = bits1 + 0xAC00 - 0x545C;
    String unicode1 = new String(Character.toChars(codePoint1));

    // second sequence of bits will fall in the 2nd 15 bit range
    int bits2 = Integer.parseInt("010101010101010", 2) & 0x7FFF;
    assertThat(bits2, equalTo(10922));
    int codePoint2 = bits2 + 0x4E00 - 0x1936;
    String unicode2 = new String(Character.toChars(codePoint2));

    // third sequence of bits will be padded to 15 bits and fall in the 3rd 15 bit range
    int bits3 = (Integer.parseInt("10101010", 2) << 7) & 0x7FFF;
    assertThat(bits3, equalTo(21760));
    int codePoint3 = bits3 + 0xAC00 - 0x545C;
    String unicode3 = new String(Character.toChars(codePoint3));

    String expected = unicode1 + unicode2
        + unicode1 + unicode2
        + unicode1 + unicode2
        + unicode1 + unicode2
        + unicode3;

    assertThat(encoded, equalTo(expected));

    assertThat(Base2e15Utils.decode(encoded), equalTo(bytes));
  }

  @Test
  void testEncodeRepeatingSequenceOf14BytesGeneratesTheExpectedResult() {
    byte[] bytes = new byte[14];
    Arrays.fill(bytes, (byte) Integer.parseInt("10101010", 2));
    String encoded = Base2e15Utils.encode(bytes);

    assertThat(encoded.length(), equalTo(8));

    // first sequence of bits will fall in the 3rd 15 bit range
    int bits1 = Integer.parseInt("101010101010101", 2) & 0x7FFF;
    assertThat(bits1, equalTo(21845));
    int codePoint1 = bits1 + 0xAC00 - 0x545C;
    String unicode1 = new String(Character.toChars(codePoint1));

    // second sequence of bits will fall in the 2nd 15 bit range
    int bits2 = Integer.parseInt("010101010101010", 2) & 0x7FFF;
    assertThat(bits2, equalTo(10922));
    int codePoint2 = bits2 + 0x4E00 - 0x1936;
    String unicode2 = new String(Character.toChars(codePoint2));

    // third sequence of bits will NOT be padded to 15 bits and fall in the 7 bit range
    int bits3 = Integer.parseInt("10101010", 2) & 0x7F;
    assertThat(bits3, equalTo(42));
    int codePoint3 = bits3 + 0x3400;
    String unicode3 = new String(Character.toChars(codePoint3));

    String expected = unicode1 + unicode2
        + unicode1 + unicode2
        + unicode1 + unicode2
        + unicode1 + unicode3;

    assertThat(encoded, equalTo(expected));

    assertThat(Base2e15Utils.decode(encoded), equalTo(bytes));
  }

  @Test
  void testEncodeDecodeRandomBytesResultsInOriginalBytes() {
    Random random = new Random(42);

    for (int len = 0; len < 1000; len++) {
      byte[] input = new byte[len];
      random.nextBytes(input);

      String encoded = Base2e15Utils.encode(input);
      byte[] decoded = Base2e15Utils.decode(encoded);

      assertThat(decoded, equalTo(input));
    }
  }
}
