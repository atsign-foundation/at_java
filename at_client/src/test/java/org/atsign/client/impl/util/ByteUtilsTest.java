package org.atsign.client.impl.util;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ByteUtilsTest {


  @Test
  public void testByteUtil0() {
    byte[] bytec = new byte[] {74, 101, 114, 101, 109, 121, 84, 117, 98, 111, 110, 103, 98, 97, 110, 117, 97, 32};
    assertEquals("JeremyTubongbanua ", ByteUtils.convert(bytec));

  }


  @Test
  public void testByteUtil1() {
    String s = "ABC";
    byte[] bytes = {65, 66, 67};
    assertArrayEquals(bytes, ByteUtils.convert(s));
  }

  @Test
  public void testByteUtil2() {
    byte[] bytec = {64, 62, 75, 85, 96, 45};
    assertEquals("@>KU`-", ByteUtils.convert(bytec));
  }

  @Test
  public void testByteUtil3() {
    String s = "@>KU`-";
    byte[] bytes = {64, 62, 75, 85, 96, 45};
    assertArrayEquals(bytes, ByteUtils.convert(s));
  }

  @Test
  public void testByteUtil4() {
    byte[] bytec = {64, 65, 69, 54};
    assertEquals("@AE6", ByteUtils.convert(bytec));
  }

  @Test
  public void testByteUtil5() {
    String s = "TECHNOLOGY";
    byte[] bytes = {84, 69, 67, 72, 78, 79, 76, 79, 71, 89};
    assertArrayEquals(bytes, ByteUtils.convert(s));
  }

  @Test
  public void testByteUtil6() {
    byte[] bytec = new byte[] {75, 69, 82, 115, 121, 90, 43, 98};
    assertEquals("KERsyZ+b", ByteUtils.convert(bytec));
  }

  @Test
  public void testByteUtil7() {
    String s = "+ada#4C0)";
    byte[] bytes = {43, 97, 100, 97, 35, 52, 67, 48, 41};
    assertArrayEquals(bytes, ByteUtils.convert(s));
  }

  @Test
  public void testByteUtil8() {
    byte[] bytec = new byte[] {45, 45, 43, 97, 115, 100, 99, 96, 96, 126, 50, 41};
    assertEquals("--+asdc``~2)", ByteUtils.convert(bytec));
  }

  @Test
  public void testByteUtil9() {
    String s = "at_sign";
    byte[] bytes = {97, 116, 95, 115, 105, 103, 110};
    assertArrayEquals(bytes, ByteUtils.convert(s));
  }

  @Test
  public void testByteUtil10() {
    byte[] bytec = {83, 105, 100, 104, 97, 114, 116, 104};
    assertEquals("Sidharth", ByteUtils.convert(bytec));
  }

}
