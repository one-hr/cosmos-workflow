package jp.co.onehr.workflow.service.approval.simple;

import jp.co.onehr.workflow.constant.NodeType;
import jp.co.onehr.workflow.dto.ActionResult;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.node.Node;
import jp.co.onehr.workflow.dto.param.ActionExtendParam;
import jp.co.onehr.workflow.service.ApprovalStrategy;
import jp.co.onehr.workflow.service.PluginService;
import jp.co.onehr.workflow.service.action.NextService;
import org.apache.commons.lang3.ObjectUtils;

public class SimpleNextService extends NextService implements ApprovalStrategy {

    @Override
    public ActionResult execute(Definition definition, Instance instance, Node node, String operatorId, ActionExtendParam extendParam) {
        var result = new ActionResult();

        var currentNodeType = NodeType.getNodeType(node.getType());

        // only the robot node supports the use of plugins for processing during the next action.
        switch (currentNodeType) {
            case RobotNode -> {
                if (ObjectUtils.isNotEmpty(extendParam)) {
                    var pluginResultMap = PluginService.singleton.processPlugin(node, extendParam.pluginParam);
                    result.pluginResult.putAll(pluginResultMap);
                }
                handleSimpleNext(definition, instance);
            }
            default -> handleSimpleNext(definition, instance);
        }

        return result;
    }

}
