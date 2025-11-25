package jp.co.onehr.workflow.util;

public class CheckUtil {

    public static void check(boolean result, String message) {
        if (!result) {
            throw new IllegalArgumentException(message);
        }
    }
    
}
