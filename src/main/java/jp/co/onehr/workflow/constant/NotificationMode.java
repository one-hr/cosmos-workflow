package jp.co.onehr.workflow.constant;


public enum NotificationMode {
    /**
     * always send notification
     */
    ALWAYS,
    /**
     * never send notification
     */
    NEVER,
    /**
     * select by user, default send notification
     */
    USER_DEFAULT_SEND,
    /**
     * select by user, default not send notification
     */
    USER_DEFAULT_NOT_SEND
}
