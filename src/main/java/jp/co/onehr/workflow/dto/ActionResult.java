package jp.co.onehr.workflow.dto;


import java.util.Map;

import com.google.common.collect.Maps;
import jp.co.onehr.workflow.contract.plugin.PluginResult;

/**
 * The execution result of the workflow action.
 */
public class ActionResult {

    /**
     * Whether the node needs to reset all operator
     */
    public boolean resetOperator = true;

    /**
     * The execution results of all plugins in the node
     */
    public Map<String, PluginResult> pluginResult = Maps.newHashMap();

    /**
     * The processed workflow instance
     */
    public Instance instance;

    /**
     * Flag to indicate whether to withdraw the instance.
     */
    public boolean withdraw = false;

    public ActionResult() {

    }

}
