package jp.co.onehr.workflow.contract.notification;


import jp.co.onehr.workflow.constant.Action;
import jp.co.onehr.workflow.dto.Instance;

public interface NotificationSender {

    /**
     * Send notification
     */
    void sendNotification(Instance instance, Action action, Notification notification);
    
}
