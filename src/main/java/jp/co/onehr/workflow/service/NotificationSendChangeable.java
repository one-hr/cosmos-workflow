package jp.co.onehr.workflow.service;

import jp.co.onehr.workflow.constant.NotificationMode;


public interface NotificationSendChangeable {

    /**
     * Determine whether nodes are sending notification
     *
     * @param notificationMode
     * @param selectedSend
     * @return
     */
    default boolean whetherSendNotification(NotificationMode notificationMode, Boolean selectedSend) {
        return switch (notificationMode) {
            case ALWAYS -> true;
            // not send mail when selectedSend is "false"
            case USER_DEFAULT_SEND -> selectedSend == null || selectedSend;
            // send mail when selectedSend is "true"
            case USER_DEFAULT_NOT_SEND -> selectedSend != null && selectedSend;
            case NEVER -> false;
        };
    }

}
