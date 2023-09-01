package jp.co.onehr.workflow.service.action;

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
        if (definition.returnToStartNode) {
            var startNode = NodeService.getStartNode(definition);
            instance.nodeId = startNode.nodeId;
        }
        instance.status = Status.REJECTED;
        return new ActionResult();
    }
}