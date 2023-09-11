package jp.co.onehr.workflow.service.action;

import jp.co.onehr.workflow.dto.ActionResult;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.param.ActionExtendParam;
import jp.co.onehr.workflow.service.ActionStrategy;
import jp.co.onehr.workflow.service.NodeService;


/**
 * Instance Rebinding of Definition
 */
public class RebindingService implements ActionStrategy {

    @Override
    public ActionResult execute(Definition definition, Instance instance, String operatorId, ActionExtendParam extendParam) {
        var firstNode = NodeService.getFirstNode(definition);
        instance.nodeId = firstNode.nodeId;
        return new ActionResult();
    }

}
