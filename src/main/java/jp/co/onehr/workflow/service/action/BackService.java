package jp.co.onehr.workflow.service.action;

import jp.co.onehr.workflow.constant.BackMode;
import jp.co.onehr.workflow.constant.Status;
import jp.co.onehr.workflow.dto.ActionResult;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.param.ActionExtendParam;
import jp.co.onehr.workflow.service.ActionStrategy;
import jp.co.onehr.workflow.service.NodeService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;


public class BackService implements ActionStrategy {

    @Override
    public ActionResult execute(Definition definition, Instance instance, String operatorId, ActionExtendParam extendParam) {
        var currentNode = NodeService.getNodeByNodeId(definition, instance.nodeId);
        return currentNode.getApprovalType().backExecute(definition, instance, currentNode, operatorId, extendParam);
    }

    protected void handleSimpleBack(Definition definition, Instance instance, ActionExtendParam extendParam) {
        var backMode = BackMode.PREVIOUS;
        var backNodeId = "";
        if (ObjectUtils.isNotEmpty(extendParam)) {
            backNodeId = extendParam.backNodeId;
            if (ObjectUtils.isNotEmpty(extendParam.backMode)) {
                backMode = extendParam.backMode;
            }
        }

        switch (backMode) {
            case FIRST -> moveToFirstNode(definition, instance);
            default -> moveToPreviousNode(definition, instance, backNodeId);
        }
        instance.status = Status.PROCESSING;
    }

    /**
     * Handle the back action using the Previous mode
     *
     * @param definition
     * @param instance
     * @param backStepId
     */
    private void moveToPreviousNode(Definition definition, Instance instance, String backStepId) {

        var nodes = NodeService.getAllNodes(definition);
        var currentNodeIndex = NodeService.getNodeIndexByNodeId(nodes, instance.nodeId);

        if (StringUtils.isEmpty(backStepId)) {
            var backNode = nodes.get(currentNodeIndex - 1);
            instance.nodeId = backNode.nodeId;
        } else {
            // todo add a restriction where you can only go back to a previous node
            var backNode = NodeService.getNodeByNodeId(definition, backStepId);
            instance.nodeId = backNode.nodeId;
        }
    }

    /**
     * Handle the back action using the First mode
     *
     * @param definition
     * @param instance
     */
    private void moveToFirstNode(Definition definition, Instance instance) {
        var firstNode = NodeService.getFirstNode(definition);
        instance.nodeId = firstNode.nodeId;
    }

}
