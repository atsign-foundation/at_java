package org.atsign.client.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.TimeZone;

public class DateUtil {
    
    public static OffsetDateTime parse(String rawDateStr) throws ParseException {
        OffsetDateTime odt = null;
        if(rawDateStr != null) {
            String dateString = rawDateStr.replace("Z", "");
            String PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";
            SimpleDateFormat sdf = new SimpleDateFormat(PATTERN);
            sdf.setTimeZone(TimeZone.getTimeZone(ZoneId.of("Z")));
            Date date = sdf.parse(dateString);
            odt = date.toInstant().atOffset(ZoneOffset.UTC); // 2022-06-24T13:43:53.979Z
        }
        return odt;
    }

}
