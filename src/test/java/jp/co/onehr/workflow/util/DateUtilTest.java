package jp.co.onehr.workflow.util;

import java.time.Instant;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class DateUtilTest {

    @Test
    void nowTest() {
        //2023-06-22T07:06:53Z
        Instant utlInstant = DateUtil.now();
        assertThat(utlInstant).isBeforeOrEqualTo(Instant.now())
                .isBeforeOrEqualTo(ZonedDateTime.now().toInstant())
                .isBeforeOrEqualTo(Instant.now().plusSeconds(1))
                .isAfter("2023-06-22T00:00:00Z")
                .isAfter(Instant.now().minusSeconds(5))
                .isBefore("2030-06-23T00:00:00Z")
                .isStrictlyBetween("2020-09-23T00:00:00Z", "2030-09-23T00:00:00Z");
    }

    @Test
    void nowDateTimeStringUTCTest() {
        //"2023-06-22T07:06:53Z"
        String utlDateTimeStringUTC = DateUtil.nowDateTimeStringUTC();

        assertThat(utlDateTimeStringUTC).isNotNull().isNotEmpty()
                .matches("\\d{4}-(?:0[1-9]|1[0-2])-(?:0[1-9]|[1-2]\\d|3[0-1])T(?:[0-1]\\d|2[0-3]):[0-5]\\d:[0-5]\\dZ")
                .isGreaterThan("2023-06-22")
                .isLessThan("2030-06-23")
                .isStrictlyBetween("2023-06-22", "2030-06-23");
    }

}
