package jp.co.onehr.workflow.dto.param;


import jp.co.onehr.workflow.constant.BackMode;
import jp.co.onehr.workflow.contract.notification.Notification;
import jp.co.onehr.workflow.contract.plugin.BusinessParam;
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
     * User selects whether to send notifications
     */
    public Boolean selectedSend;

    /**
     * Content of the message notification
     */
    public Notification notification;

    /**
     * comment
     */
    public String comment = "";

    /**
     * business param
     */
    public BusinessParam businessParam;

    public ActionExtendParam() {

    }

}
