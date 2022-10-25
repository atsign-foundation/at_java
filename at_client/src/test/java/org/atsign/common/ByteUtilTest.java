package org.atsign.common;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
public class ByteUtilTest {


    @Test
    public void testByteUtil0(){
        ByteUtil byteUtil = new ByteUtil();
        byte[] bytec = new byte[] {74, 101 ,114 ,101 ,109 ,121 ,84 ,117 ,98 ,111 ,110 ,103 ,98 ,97 ,110 ,117 ,97 ,32  };
        assertEquals("JeremyTubongbanua ",byteUtil.convert(bytec));
    }


    @Test
    public void testByteUtil1(){
        ByteUtil byteUtil = new ByteUtil();
        String s = "ABC";
        byte[] bytes = {65,66,67};
        Assert.assertArrayEquals(bytes,byteUtil.convert(s));
    }
    @Test
    public void testByteUtil2(){
        ByteUtil byteUtil = new ByteUtil();
        byte[] bytec = {64,62,75,85,96,45};
        assertEquals("@>KU`-",byteUtil.convert(bytec));
    }

    @Test
    public void testByteUtil3(){
        ByteUtil byteUtil = new ByteUtil();
        String s = "@>KU`-";
        byte[] bytes = {64,62,75,85,96,45};
        Assert.assertArrayEquals(bytes,byteUtil.convert(s));
    }

    @Test
    public void testByteUtil4(){
        ByteUtil byteUtil = new ByteUtil();
        byte[] bytec = {64,65,69,54};
        assertEquals("@AE6",byteUtil.convert(bytec));
    }
    @Test
    public void testByteUtil5(){
        ByteUtil byteUtil = new ByteUtil();
        String s = "TECHNOLOGY";
        byte[] bytes = { 84, 69, 67, 72, 78, 79, 76, 79, 71, 89};
        Assert.assertArrayEquals(bytes,byteUtil.convert(s));
    }

    @Test
    public void testByteUtil6(){
        ByteUtil byteUtil = new ByteUtil();
        byte[] bytec = new byte[] { 75, 69, 82, 115, 121, 90, 43, 98};
        assertEquals("KERsyZ+b",byteUtil.convert(bytec));
    }
    @Test
    public void testByteUtil7(){
        ByteUtil byteUtil = new ByteUtil();
        String s = "+ada#4C0)";
        byte[] bytes = {43 ,97 ,100 ,97 ,35 ,52 ,67 ,48 ,41};
        Assert.assertArrayEquals(bytes,byteUtil.convert(s));
    }

    @Test
    public void testByteUtil8(){
        ByteUtil byteUtil = new ByteUtil();
        byte[] bytec = new byte[] {45 ,45 ,43 ,97 ,115 ,100 ,99 ,96, 96, 126, 50 ,41 };
        assertEquals("--+asdc``~2)",byteUtil.convert(bytec));
    }

    @Test
    public void testByteUtil9(){
        ByteUtil byteUtil = new ByteUtil();
        String s = "at_sign";
        byte[] bytes = {97 ,116 ,95 ,115 ,105 ,103 ,110 };
        Assert.assertArrayEquals(bytes,byteUtil.convert(s));
    }

    @Test
    public void testByteUtil10(){
        ByteUtil byteUtil = new ByteUtil();
        byte[] bytec = {83, 105, 100, 104, 97, 114, 116, 104};
        assertEquals("Sidharth",byteUtil.convert(bytec));
    }

}
