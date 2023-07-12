package jp.co.onehr.workflow.service.action;

import jp.co.onehr.workflow.constant.BackMode;
import jp.co.onehr.workflow.constant.Status;
import jp.co.onehr.workflow.dto.ActionResult;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.param.ActionExtendParam;
import jp.co.onehr.workflow.service.ActionStrategy;
import jp.co.onehr.workflow.service.NodeService;

public class RejectService implements ActionStrategy {
    @Override
    public ActionResult execute(Definition definition, Instance instance, String operatorId, ActionExtendParam extendParam) {
        var currentNode = NodeService.getCurrentNode(definition, instance.nodeId);

        // Current node of instance will move to first node.
        extendParam = extendParam == null ? new ActionExtendParam() : extendParam;
        extendParam.backMode = BackMode.FIRST;
        instance.status = Status.REJECTED;
        return currentNode.getApprovalType().backExecute(definition, instance, currentNode, operatorId, extendParam);
    }
}