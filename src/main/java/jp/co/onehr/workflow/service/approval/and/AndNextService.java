package jp.co.onehr.workflow.service.approval.and;

import jp.co.onehr.workflow.dto.ActionResult;
import jp.co.onehr.workflow.dto.ApprovalStatus;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.node.Node;
import jp.co.onehr.workflow.dto.param.ActionExtendParam;
import jp.co.onehr.workflow.service.ApprovalStrategy;
import jp.co.onehr.workflow.service.action.NextService;
import org.apache.commons.lang3.ObjectUtils;

public class AndNextService extends NextService implements ApprovalStrategy {

    @Override
    public ActionResult execute(Definition definition, Instance instance, Node node, String operatorId, ActionExtendParam extendParam) {
        var actionResult = new ActionResult();
        actionResult.resetOperator = false;
        var allApproved = true;
        for (ApprovalStatus as : instance.parallelApproval.values()) {
            if (as.operatorId.equals(operatorId)) {
                as.approved = true;
            }
            if (ObjectUtils.isNotEmpty(as.operatorId)) {
                allApproved = allApproved && as.approved;
            }
        }

        if (allApproved) {
            handleSimpleNext(definition, instance);
            actionResult.resetOperator = true;
        }

        return actionResult;
    }

}
