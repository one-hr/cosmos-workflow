package jp.co.onehr.workflow.service;


import java.util.HashMap;
import java.util.Map;

import jp.co.onehr.workflow.ProcessConfiguration;
import jp.co.onehr.workflow.constant.WorkflowErrors;
import jp.co.onehr.workflow.contract.plugin.PluginParam;
import jp.co.onehr.workflow.contract.plugin.PluginResult;
import jp.co.onehr.workflow.dto.node.Node;
import jp.co.onehr.workflow.exception.WorkflowException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;

public class PluginService {

    public static final PluginService singleton = new PluginService();

    private PluginService() {

    }

    /**
     * Execute the handle of all the plugins on the node.
     *
     * @param node
     * @param param
     * @return
     */
    public Map<String, PluginResult> processPlugin(Node node, PluginParam param) {
        var result = new HashMap<String, PluginResult>();

        var pluginTypes = node.plugins;

        if (CollectionUtils.isNotEmpty(pluginTypes)) {
            for (var pluginType : pluginTypes) {
                try {
                    var plugin = ProcessConfiguration.getConfiguration().getPlugin(pluginType);
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
