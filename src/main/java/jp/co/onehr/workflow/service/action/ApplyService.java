package jp.co.onehr.workflow.service.action;

import jp.co.onehr.workflow.constant.Status;
import jp.co.onehr.workflow.dto.ActionResult;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.param.ActionExtendParam;
import jp.co.onehr.workflow.service.ActionStrategy;
import jp.co.onehr.workflow.service.NodeService;

/**
 * Processing of the "Apply" action
 */
public class ApplyService implements ActionStrategy {
    @Override
    public ActionResult execute(Definition definition, Instance instance, String operatorId, ActionExtendParam extendParam) {
        var firstNode = NodeService.getFirstNode(definition);
        instance.nodeId = firstNode.nodeId;
        instance.status = Status.PROCESSING;
        return new ActionResult();
    }
}
