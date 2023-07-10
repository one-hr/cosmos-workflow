package jp.co.onehr.workflow.constant;


import jp.co.onehr.workflow.dto.ActionResult;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.param.ActionExtendParam;
import jp.co.onehr.workflow.exception.WorkflowException;
import jp.co.onehr.workflow.service.ActionStrategy;
import jp.co.onehr.workflow.service.InstanceService;
import jp.co.onehr.workflow.service.NodeService;
import jp.co.onehr.workflow.service.action.*;
import org.apache.commons.lang3.StringUtils;

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
    WITHDRAW(new WithdrawalService());

    private final ActionStrategy strategy;

    Action(ActionStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Perform the corresponding action's handling
     */
    public ActionResult execute(Definition definition, Instance instance, String operatorId, ActionExtendParam extendParam) {

        if (StringUtils.isEmpty(operatorId)) {
            throw new WorkflowException(WorkflowErrors.INSTANCE_OPERATOR_INVALID, "The operator of the instance cannot be empty", instance.getId());
        }

        // Before executing an action, check if the user's action is allowed
        InstanceService.singleton.setAllowingActions(definition, instance, operatorId);

        if (!instance.allowingActions.contains(this)) {
            throw new WorkflowException(WorkflowErrors.NODE_ACTION_INVALID, "The current action is not allowed at the node for the instance", instance.getId());
        }

        var actionResult = strategy.execute(definition, instance, operatorId, extendParam);

        var currentNode = NodeService.getCurrentNode(definition, instance.nodeId);
        if (actionResult.resetOperator) {
            currentNode.resetCurrentOperators(instance);
        }

        actionResult.node = currentNode;

        return actionResult;
    }

}
