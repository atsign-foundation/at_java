package org.atsign.common;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.time.Month;
import java.time.OffsetDateTime;

import org.atsign.client.util.DateUtil;
import org.junit.Test;

public class DateUtilTests {
    
    @Test
    public void test1() {
        String DATE_STR = "2022-06-18 21:27:26.875Z";
        OffsetDateTime odt = null;
        try {
            odt = DateUtil.parse(DATE_STR);
        } catch (ParseException e) {
            System.err.println(e);
            e.printStackTrace();
        }
        assertEquals("2022-06-18T21:27:26.875Z", odt.toString());
        assertEquals(2022, odt.getYear());
        assertEquals(Month.JUNE, odt.getMonth());
        assertEquals(18, odt.getDayOfMonth());
    }

}
