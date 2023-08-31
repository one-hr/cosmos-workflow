package jp.co.onehr.workflow.dto.param;


import jp.co.onehr.workflow.constant.ApplicationMode;
import jp.co.onehr.workflow.contract.context.InstanceContext;
import jp.co.onehr.workflow.contract.context.OperatorLogContext;
import jp.co.onehr.workflow.contract.notification.Notification;
import jp.co.onehr.workflow.dto.base.SimpleData;

/**
 * Parameters used when initiating a workflow instance
 */
public class ApplicationParam extends SimpleData {

    public String workflowId = "";

    public ApplicationMode applicationMode = ApplicationMode.SELF;

    public String applicant = "";

    public String proxyApplicant = "";

    public String comment = "";

    public InstanceContext instanceContext;

    public OperatorLogContext logContext;

    /**
     * Content of the message notification
     */
    public Notification notification;

    public ApplicationParam() {

    }

}
