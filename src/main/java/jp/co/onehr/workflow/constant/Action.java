package jp.co.onehr.workflow.constant;


import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.param.ActionExtendParam;
import jp.co.onehr.workflow.exception.WorkflowException;
import jp.co.onehr.workflow.service.ActionStrategy;
import jp.co.onehr.workflow.service.InstanceService;
import jp.co.onehr.workflow.service.NodeService;
import jp.co.onehr.workflow.service.action.BackService;
import jp.co.onehr.workflow.service.action.NextService;
import jp.co.onehr.workflow.service.action.SaveService;
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
    BACK(new BackService());

    private final ActionStrategy strategy;

    Action(ActionStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Perform the corresponding action's handling
     */
    public void execute(Definition definition, Instance instance, String operatorId, ActionExtendParam extendParam) {

        if (StringUtils.isEmpty(operatorId)) {
            throw new WorkflowException(WorkflowErrors.INSTANCE_OPERATOR_INVALID, "The operator of the instance cannot be empty", instance.getId());
        }

        // todo
        InstanceService.singleton.setAllowingActions(definition, instance, operatorId);

        var actionResult = strategy.execute(definition, instance, operatorId, extendParam);

        if (actionResult.resetOperator) {
            var currentNode = NodeService.getCurrentNode(definition, instance.nodeId);
            currentNode.resetCurrentOperators(instance);
        }

    }

}
