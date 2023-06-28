package jp.co.onehr.workflow.service.approval.simple;

import jp.co.onehr.workflow.dto.ActionResult;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.node.Node;
import jp.co.onehr.workflow.dto.param.ActionExtendParam;
import jp.co.onehr.workflow.service.ApprovalStrategy;
import jp.co.onehr.workflow.service.action.BackService;


public class SimpleBackService extends BackService implements ApprovalStrategy {

    @Override
    public ActionResult execute(Definition definition, Instance instance, Node node, String operatorId, ActionExtendParam extendParam) {
        handleSimpleBack(definition, instance, extendParam);
        return new ActionResult();
    }

}
