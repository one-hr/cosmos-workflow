package jp.co.onehr.workflow.dto;


import java.util.Map;

import com.google.common.collect.Maps;
import jp.co.onehr.workflow.dto.node.Node;

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
     * The node where the action processing is completed
     */
    public Node node;

    public ActionResult() {

    }

}
