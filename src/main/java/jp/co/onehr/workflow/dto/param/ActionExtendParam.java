package jp.co.onehr.workflow.dto.param;


import jp.co.onehr.workflow.constant.BackMode;
import jp.co.onehr.workflow.constant.OperationMode;
import jp.co.onehr.workflow.contract.context.InstanceContext;
import jp.co.onehr.workflow.contract.context.OperatorLogContext;
import jp.co.onehr.workflow.contract.notification.Notification;
import jp.co.onehr.workflow.contract.plugin.PluginParam;
import jp.co.onehr.workflow.dto.base.SimpleData;

/**
 * The extension parameters required when moving a node
 */
public class ActionExtendParam extends SimpleData {

    /**
     * The mode of back action
     */
    public BackMode backMode;

    /**
     * When using the "back" action with the "PREVIOUS" mode
     * specify the ID of the node to go back to
     */
    public String backNodeId = "";

    /**
     * The parameters required for executing the plugin processing
     */
    public PluginParam pluginParam;

    /**
     * Content of the message notification
     */
    public Notification notification;

    /**
     * comment
     */
    public String comment = "";

    /**
     * Context of business data corresponding to a workflow instance.
     */
    public InstanceContext instanceContext;

    /**
     * Context of the operation corresponding to the workflow history.
     */
    public OperatorLogContext logContext;

    /**
     * operation mode
     */
    public OperationMode operationMode = OperationMode.OPERATOR_MODE;

    public ActionExtendParam() {

    }

}
