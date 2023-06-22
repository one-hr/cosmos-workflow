package jp.co.onehr.workflow.util;

import java.time.Instant;
import java.time.temporal.ChronoField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateUtil {

    private static final Logger log = LoggerFactory.getLogger(DateUtil.class);

    /**
     * @return 2023-06-22T07:06:53Z
     */
    public static Instant now() {
        return Instant.now().with(ChronoField.NANO_OF_SECOND, 0);
    }
    
    /**
     * @return 2023-06-22T07:06:53Z
     */
    public static String nowDateTimeStringUTC() {
        return now().toString();
    }


}
