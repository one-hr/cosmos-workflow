package jp.co.onehr.workflow.service.action;

import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.param.ActionExtendParam;
import jp.co.onehr.workflow.service.ActionStrategy;
import jp.co.onehr.workflow.service.NodeService;


public class NextService implements ActionStrategy {

    @Override
    public void execute(Definition definition, Instance instance, ActionExtendParam extendParam) {
        var currentNode = NodeService.getCurrentNode(definition, instance.nodeId);
        currentNode.getApproveType().nextExecute(definition, instance, currentNode, extendParam);
    }

    /**
     * Handle the simple next action, directly moving to the next node.
     *
     * @param definition
     * @param instance
     */
    protected void handleSimpleNext(Definition definition, Instance instance) {
        var nodes = NodeService.getAllNodes(definition);
        var currentNodeIndex = NodeService.getCurrentNodeIndex(nodes, instance.nodeId);

        var nextNode = nodes.get(currentNodeIndex + 1);
        instance.nodeId = nextNode.nodeId;
    }
}
