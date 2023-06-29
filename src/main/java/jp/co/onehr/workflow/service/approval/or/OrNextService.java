package jp.co.onehr.workflow.service.approval.or;

import jp.co.onehr.workflow.dto.ActionResult;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.node.Node;
import jp.co.onehr.workflow.dto.param.ActionExtendParam;
import jp.co.onehr.workflow.service.ApprovalStrategy;
import jp.co.onehr.workflow.service.action.NextService;

public class OrNextService extends NextService implements ApprovalStrategy {

    @Override
    public ActionResult execute(Definition definition, Instance instance, Node node, String operatorId, ActionExtendParam extendParam) {
        handleSimpleNext(definition, instance);
        return new ActionResult();
    }

}
