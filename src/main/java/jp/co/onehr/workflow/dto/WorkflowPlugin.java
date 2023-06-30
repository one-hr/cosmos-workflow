package jp.co.onehr.workflow.dto;

import jp.co.onehr.workflow.dto.node.Node;
import jp.co.onehr.workflow.dto.param.PluginParam;

/**
 * The plugin definition that can be used in the workflow.
 * Custom plugins need to extend this class.
 */
public abstract class WorkflowPlugin {

    /**
     * Plugin types
     * The simple name of custom plugin
     */
    private String type;

    public WorkflowPlugin() {
        type = getClass().getSimpleName();
    }

    public String getType() {
        return type;
    }

    /**
     * Implement the specific handle of the plugin.
     *
     * @param node
     * @param param
     * @return
     * @throws Exception
     */
    public abstract PluginResult handle(Node node, PluginParam param) throws Exception;

}

