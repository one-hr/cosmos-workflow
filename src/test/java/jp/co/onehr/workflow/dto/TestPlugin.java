package jp.co.onehr.workflow.dto;


import java.util.Map;

import jp.co.onehr.workflow.dto.node.Node;
import jp.co.onehr.workflow.dto.param.PluginParam;
import jp.co.onehr.workflow.dto.param.TestPluginParam;

public class TestPlugin extends WorkflowPlugin {

    public TestPlugin() {

    }

    @Override
    public PluginResult handle(Node node, PluginParam param) throws Exception {
        var result = new TestPluginResult();

        if (param instanceof TestPluginParam pluginParam) {
            var num = pluginParam.num;
            var str = pluginParam.str;
            result.resultNum = num;
            result.resultStr = str;
        }

        var map = (Map) node.configuration;
        result.resultMap.putAll(map);
        result.nodeType = node.getType();

        return result;
    }

}
