package jp.co.onehr.workflow.constant;


import jp.co.onehr.workflow.ProcessConfiguration;
import jp.co.onehr.workflow.contract.context.InstanceContext;
import jp.co.onehr.workflow.contract.log.OperateLogService;
import jp.co.onehr.workflow.dto.ActionResult;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.OperateLog;
import jp.co.onehr.workflow.dto.node.Node;
import jp.co.onehr.workflow.dto.param.ActionExtendParam;
import jp.co.onehr.workflow.service.ActionStrategy;
import jp.co.onehr.workflow.service.NodeService;
import jp.co.onehr.workflow.service.action.*;
import jp.co.onehr.workflow.util.DateUtil;

/**
 * All the possible move actions that can be performed on a node
 */
public enum Action {

    /**
     * Keep the instance at the current node
     */
    SAVE(new SaveService()),

    /**
     * Move the instance from the current node to the next node
     */
    NEXT(new NextService()),

    /**
     * Move the instance back to the previous node from the current node
     */
    BACK(new BackService()),

    /**
     * Cancel. Applicant cancels instance when the instance is processing
     */
    CANCEL(new CancelService()),

    /**
     * Reject. Approver closes instance when the instance is processing
     */
    REJECT(new RejectService()),

    /**
     * Withdraw. Withdraw instance means the instance data will be deleted immediately
     */
    WITHDRAW(new WithdrawalService()),

    /**
     * Retrieve. The operator of the previous node can retrieve their own instance.
     */
    RETRIEVE(new RetrieveService()),

    /**
     * Instance Rebinding of Definition
     */
    REBINDING(new RebindingService()),

    /**
     * Handling of the apply action to transition the instance from "REJECTED" or "CANCELED" to a "PROCESSING" status
     */
    APPLY(new ApplyService()),

    /**
     * The action of 'reapply' is consistent with the action of 'next'.
     * <p>
     * When the process instance is returned to the initiator of the first node
     * you can change 'next' to 'reapply' to distinguish the operation prompt and the record in the history.
     */
    REAPPLY(new NextService());


    private final ActionStrategy strategy;

    Action(ActionStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Perform the corresponding action's handling
     */
    public ActionResult execute(Definition definition, Status currentStatus, Instance instance, String operatorId, ActionExtendParam extendParam) {

        var beforeNode = NodeService.getNodeByNodeId(definition, instance.nodeId);

        var actionResult = strategy.execute(definition, instance, operatorId, extendParam);

        var afterNode = NodeService.getNodeByNodeId(definition, instance.nodeId);

        if (actionResult.resetOperator) {
            instance.preNodeId = "";
            instance.preExpandOperatorIdSet.clear();

            InstanceContext instanceContext = null;

            if (extendParam != null) {
                instanceContext = extendParam.instanceContext;
            }

            var previousNodeInfo = NodeService.getPreviousNodeInfo(definition, instance, instanceContext);
            if (!previousNodeInfo.isEmpty()) {
                instance.preNodeId = previousNodeInfo.nodeId;
                instance.preExpandOperatorIdSet.addAll(previousNodeInfo.expandOperatorIdSet);
            }

            afterNode.resetCurrentOperators(instance, instanceContext);

            afterNode.resetParallelApproval(instance, afterNode.getApprovalType(), this, operatorId, instanceContext);
        }

        if (OperateLogService.ACTIONS_LOG_RECORD.contains(this) && beforeNode.whetherAddOperationLog(this)) {
            generateOperateLog(operatorId, extendParam, currentStatus, beforeNode, instance, actionResult);
        }

        actionResult.instance = instance;

        return actionResult;
    }

    private void generateOperateLog(String operatorId, ActionExtendParam extendParam, Status currentStatus, Node currentNode, Instance updatedInstance, ActionResult actionResult) {
        var operateLog = new OperateLog();
        operateLog.nodeId = currentNode.nodeId;
        operateLog.nodeName = currentNode.nodeName;
        operateLog.nodeType = currentNode.getType();
        operateLog.statusBefore = currentStatus;
        operateLog.operatorId = operatorId;
        operateLog.action = this.name();
        operateLog.statusAfter = updatedInstance.status;
        operateLog.comment = extendParam != null ? extendParam.comment : operateLog.comment;
        operateLog.logContext = extendParam != null ? extendParam.logContext : null;
        operateLog.operatorAt = DateUtil.nowDateTimeStringUTC();

        var configuration = ProcessConfiguration.getConfiguration();
        configuration.handlingActionResultLog(operateLog, actionResult);

        updatedInstance.operateLogList.add(operateLog);

        // Reset after use to avoid reusing duplicates during recursion
        if (extendParam != null) {
            extendParam.logContext = null;
            extendParam.comment = "";
        }
    }
}
