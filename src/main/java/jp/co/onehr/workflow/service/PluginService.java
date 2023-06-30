package jp.co.onehr.workflow.service;


import java.util.HashMap;
import java.util.Map;

import jp.co.onehr.workflow.constant.WorkflowErrors;
import jp.co.onehr.workflow.dto.PluginResult;
import jp.co.onehr.workflow.dto.WorkflowEngine;
import jp.co.onehr.workflow.dto.node.Node;
import jp.co.onehr.workflow.dto.param.PluginParam;
import jp.co.onehr.workflow.exception.WorkflowException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;

public class PluginService {

    /**
     * Execute the handle of all the plugins on the node.
     *
     * @param node
     * @param param
     * @return
     */
    public static Map<String, PluginResult> processPlugin(Node node, PluginParam param) {
        var result = new HashMap<String, PluginResult>();

        var pluginTypes = node.plugins;

        if (CollectionUtils.isNotEmpty(pluginTypes)) {
            for (var pluginType : pluginTypes) {
                try {
                    var plugin = WorkflowEngine.getPlugin(pluginType);
                    if (ObjectUtils.isNotEmpty(plugin)) {
                        var pluginResult = plugin.handle(node, param);
                        result.put(pluginType, pluginResult);
                    }
                } catch (Exception e) {
                    throw new WorkflowException(WorkflowErrors.PLUGIN_HANDLE_INVALID, "An exception occurred during the plugin processing of the node", pluginType);
                }
            }
        }

        return result;
    }

}
