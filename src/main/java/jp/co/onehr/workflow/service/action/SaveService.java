package jp.co.onehr.workflow.service.action;

import jp.co.onehr.workflow.ProcessConfiguration;
import jp.co.onehr.workflow.constant.ApprovalType;
import jp.co.onehr.workflow.contract.context.InstanceContext;
import jp.co.onehr.workflow.dto.ActionResult;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.param.ActionExtendParam;
import jp.co.onehr.workflow.service.ActionStrategy;
import jp.co.onehr.workflow.service.NodeService;
import org.apache.commons.collections4.CollectionUtils;


public class SaveService implements ActionStrategy {

    @Override
    public ActionResult execute(Definition definition, Instance instance, String operatorId, ActionExtendParam extendParam) {
        var actionResult = new ActionResult();

        var currentNode = NodeService.getNodeByNodeId(definition, instance.nodeId);

        InstanceContext instanceContext = null;

        if (extendParam != null) {
            instanceContext = extendParam.instanceContext;
        }

        var expandOperatorIds = currentNode.generateExpandOperatorIds(instanceContext);

        if (!CollectionUtils.containsAll(expandOperatorIds, instance.expandOperatorIdSet)) {
            instance.expandOperatorIdSet.clear();
            instance.expandOperatorIdSet.addAll(expandOperatorIds);
            if (currentNode.getApprovalType().equals(ApprovalType.AND)) {

                var parallelApproval = ProcessConfiguration.getConfiguration()
                        .handleModificationParallelApproval(instance.operatorIdSet, instance.operatorOrgIdSet,
                                instance.expandOperatorIdSet, instance.parallelApproval, instanceContext);

                instance.parallelApproval.clear();
                instance.parallelApproval.putAll(parallelApproval);

            }
        }

        actionResult.resetOperator = false;
        return actionResult;
    }

}