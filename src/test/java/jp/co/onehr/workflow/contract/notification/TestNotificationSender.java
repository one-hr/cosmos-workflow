package jp.co.onehr.workflow.contract.notification;

import jp.co.onehr.workflow.constant.Action;
import jp.co.onehr.workflow.dto.Instance;


public class TestNotificationSender implements NotificationSender {

    public static final TestNotificationSender singleton = new TestNotificationSender();

    @Override
    public void sendNotification(Instance instance, Action action, Notification notification) {
        if (notification instanceof TestNotification testNotification) {
            if (action.equals(Action.REJECT)) {
                testNotification.result = "reject content";
            } else {
                testNotification.result = action.name() + ":" + testNotification.content;
            }
        }
    }

}
