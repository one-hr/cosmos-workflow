package jp.co.onehr.workflow.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import io.github.thunderz99.cosmos.condition.Condition;
import jp.co.onehr.workflow.ProcessConfiguration;
import jp.co.onehr.workflow.constant.*;
import jp.co.onehr.workflow.contract.notification.Notification;
import jp.co.onehr.workflow.dto.ActionResult;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.OperateLog;
import jp.co.onehr.workflow.dto.base.DeletedObject;
import jp.co.onehr.workflow.dto.node.Node;
import jp.co.onehr.workflow.dto.param.ActionExtendParam;
import jp.co.onehr.workflow.dto.param.ApplicationParam;
import jp.co.onehr.workflow.exception.WorkflowException;
import jp.co.onehr.workflow.service.base.BaseCRUDService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;


public class InstanceService extends BaseCRUDService<Instance> implements NotificationSendChangeable {

    public static final InstanceService singleton = new InstanceService();

    // Enable recursive action for nodes.
    public static final Set<Action> recursiveAction = Set.of(Action.NEXT, Action.BACK);

    private InstanceService() {
        super(Instance.class);
    }

    @Override
    protected Instance readSuppressing404(String host, String id) throws Exception {
        return super.readSuppressing404(host, id);
    }

    @Override
    protected DeletedObject delete(String host, String id) throws Exception {
        return super.delete(host, id);
    }

    @Override
    protected DeletedObject purge(String host, String id) throws Exception {
        return super.purge(host, id);
    }

    @Override
    protected List<Instance> find(String host, Condition cond) throws Exception {
        return super.find(host, cond);
    }

    /**
     * Make sure Instance is existed
     *
     * @param host
     * @param instanceId
     * @return
     * @throws Exception
     */
    protected Instance getInstance(String host, String instanceId) throws Exception {
        var instance = super.readSuppressing404(host, instanceId);
        if (ObjectUtils.isEmpty(instance)) {
            throw new WorkflowException(WorkflowErrors.INSTANCE_NOT_EXIST, "The instance does not exist in the database", instanceId);
        }
        return instance;
    }

    /**
     * Start an instance based on the workflow definition
     *
     * @param host
     * @param param
     * @return
     * @throws Exception
     */
    protected Instance start(String host, ApplicationParam param) throws Exception {
        var workflow = WorkflowService.singleton.getWorkflow(host, param.workflowId);
        var definition = DefinitionService.singleton.getCurrentDefinition(host, workflow.id, workflow.currentVersion);

        var instance = new Instance(workflow.id, definition.id);
        instance.status = Status.PROCESSING;
        instance.setApplicationInfo(param);

        var operatorId = ApplicationMode.SELF.equals(param.applicationMode) ? param.applicant : param.proxyApplicant;
        var firstNode = NodeService.getFirstNode(definition);
        instance.nodeId = firstNode.nodeId;
        firstNode.resetCurrentOperators(instance);
        firstNode.resetParallelApproval(instance, firstNode.getApprovalType(), Action.APPLY, operatorId);
        firstNode.handleFirstNode(definition, instance);

        var operateLog = new OperateLog();
        operateLog.nodeId = firstNode.nodeId;
        operateLog.nodeName = firstNode.nodeName;
        operateLog.nodeType = firstNode.getClass().getSimpleName();
        operateLog.statusBefore = Status.NEW;
        operateLog.operatorId = operatorId;
        operateLog.action = Action.APPLY;
        operateLog.statusAfter = Status.PROCESSING;
        operateLog.comment = param.comment;
        operateLog.businessParam = param.businessParam;
        instance.operateLogList.add(operateLog);

        return super.create(host, instance);
    }

    /**
     * Advance the workflow instance processing based on the action
     *
     * @param host
     * @param instanceId
     * @param action
     * @param extendParam
     * @return
     * @throws Exception
     */
    protected ActionResult resolve(String host, String instanceId, Action action, String operatorId, ActionExtendParam extendParam) throws Exception {

        var existInstance = getInstance(host, instanceId);

        var definition = DefinitionService.singleton.getDefinition(host, existInstance.definitionId);

        var configuration = ProcessConfiguration.getConfiguration();

        var existNode = NodeService.getNodeByInstance(definition, existInstance);

        Instance instance = existInstance.copy();

        checkAllowingAction(operatorId, extendParam, definition, instance, action);

        ActionResult result = action.execute(definition, existInstance.status, instance, operatorId, extendParam);

        result = recursiveInstance(definition, existInstance.status, result, action, operatorId, extendParam, 0);

        var updatedInstance = result.instance;

        // Delete the instance if withdraw
        if (result.withdraw) {
            delete(host, updatedInstance.id);
        } else {

            // If the instance reaches the last node, the status is changed to Approved.
            if (NodeService.isLastNode(definition, updatedInstance.nodeId)) {
                updatedInstance.status = Status.APPROVED;
                var operateLogList = updatedInstance.operateLogList;
                operateLogList.get(operateLogList.size() - 1).statusAfter = Status.APPROVED;
            }
            result.instance = super.update(host, updatedInstance);
        }

        handleSendNotification(configuration, updatedInstance, existNode, action, extendParam);

        return result;
    }

    private void checkAllowingAction(String operatorId, ActionExtendParam extendParam, Definition definition, Instance instance, Action action) {
        OperationMode targetOperationMode = null;
        // operator as not admin
        if (extendParam == null || !extendParam.operationMode.isAdminMode()) {
            if (StringUtils.isBlank(operatorId)) {
                throw new WorkflowException(WorkflowErrors.INSTANCE_OPERATOR_INVALID, "The operator of the instance cannot be empty", instance.getId());
            }
            targetOperationMode = OperationMode.OPERATOR_MODE;
        } else {
            targetOperationMode = OperationMode.ADMIN_MODE;
        }

        // Before executing an action, check if the user's action is allowed
        InstanceService.singleton.setAllowingActions(definition, instance, operatorId, targetOperationMode);

        if (!instance.allowingActions.contains(action)) {
            throw new WorkflowException(WorkflowErrors.NODE_ACTION_INVALID, "The current action is not allowed at the node for the instance", instance.getId());
        }
    }

    /**
     * If there is no operator in the current node, it will move on to the next (or previous) node
     * up to a maximum of 200 nodes for step movement.
     *
     * @param definition
     * @param actionResult
     * @param action
     * @param operatorId
     * @param extendParam
     * @param count
     * @return
     */
    protected ActionResult recursiveInstance(Definition definition, Status currentStatus, ActionResult actionResult, Action action,
                                             String operatorId, ActionExtendParam extendParam, int count) {
        var instance = actionResult.instance;

        if (NodeService.isLastNode(definition, instance.nodeId)) {
            return actionResult;
        }

        if (count > 200) {
            throw new WorkflowException(WorkflowErrors.INSTANCE_INVALID, "recursiveOperatorIds become Endless loop", instance.getId());
        }

        if (CollectionUtils.isEmpty(instance.expandOperatorIdSet) && recursiveAction.contains(action)) {
            actionResult = action.execute(definition, currentStatus, instance, operatorId, extendParam);
            return recursiveInstance(definition, currentStatus, actionResult, action, operatorId, extendParam, ++count);
        }

        return actionResult;
    }

    /**
     * Set the allowed actions for the instance.
     *
     * @param definition
     * @param instance
     * @param operatorId
     */
    public void setAllowingActions(Definition definition, Instance instance, String operatorId, OperationMode operationMode) {
        instance.allowingActions.clear();

        var configuration = ProcessConfiguration.getConfiguration();

        var actions = generateActionsByStatus(instance);

        if (operationMode != null && operationMode.isAdminMode()) {
            var removalActions = generateRemovalActionsByAdmin(definition, instance, operatorId);
            actions.removeAll(removalActions);
        } else {
            var customRemovalActions = configuration.generateCustomRemovalActionsByOperator(definition, instance, operatorId);
            actions.removeAll(customRemovalActions);

            var removalActions = generateRemovalActionsByOperator(definition, instance, operatorId);
            actions.removeAll(removalActions);
        }

        instance.allowingActions.addAll(actions);
    }

    /**
     * Set action permissions entry point
     * Generate all available actions based on the status
     * Subsequent methods only allow deletion of actions and do not allow adding new actions
     *
     * @param instance
     * @return
     */
    private Set<Action> generateActionsByStatus(Instance instance) {
        var actions = new HashSet<Action>();
        var status = instance.status;
        switch (status) {
            case PROCESSING -> {
                actions.add(Action.SAVE);
                actions.add(Action.NEXT);
                actions.add(Action.BACK);
                actions.add(Action.CANCEL);
                actions.add(Action.REJECT);
                actions.add(Action.WITHDRAW);
                actions.add(Action.RETRIEVE);
            }
            case REJECTED -> {
                actions.add(Action.CANCEL);
                actions.add(Action.WITHDRAW);
                actions.add(Action.APPLY);
            }
            case CANCELED -> {
                actions.add(Action.WITHDRAW);
                actions.add(Action.APPLY);
            }
            case APPROVED -> {
                actions.add(Action.CANCEL);
                actions.add(Action.WITHDRAW);
                actions.add(Action.RETRIEVE);
            }
            case FINISHED -> {
            }
        }
        return Sets.newHashSet(actions);
    }

    /**
     * Generating the most basic action that needs to be removed
     *
     * @param definition
     * @param instance
     * @param operatorId
     * @return
     */
    private Set<Action> generateRemovalActionsByOperator(Definition definition, Instance instance, String operatorId) {
        var actions = new HashSet<Action>();

        var status = instance.status;
        switch (status) {
            case PROCESSING -> {
                actions.add(Action.RETRIEVE);

                //If it is the first node, back and retrieve action is not allowed.
                if (NodeService.isFirstNode(definition, instance.nodeId)) {
                    actions.add(Action.BACK);
                }

                // If it is the last node, next action is not allowed.
                if (NodeService.isLastNode(definition, instance.nodeId)) {
                    actions.add(Action.NEXT);
                }

                var currentNode = NodeService.getNodeByNodeId(definition, instance.nodeId);

                if (NodeService.isManualNode(currentNode.getType())) {
                    // If the current operator is not one of the allowed operators for the instance,
                    // all actions are not available for use.
                    if (!instance.expandOperatorIdSet.contains(operatorId)) {
                        actions.addAll(List.of(Action.values()));
                        // If the operator has the permission to retrieve the instance, the retrieve action is not removed.
                        if (isRetrieveOperator(instance, operatorId)) {
                            actions.remove(Action.RETRIEVE);
                        }
                    }

                    // In the case of parallel approval, if it is the operator of the instance
                    // Operators who have already approved the action are not allowed to perform any further actions.
                    if (instance.expandOperatorIdSet.contains(operatorId) && currentNode.getApprovalType().equals(ApprovalType.AND)) {

                        var approvedIds = instance.parallelApproval.values().stream()
                                .filter(approvalStatus -> approvalStatus.approved)
                                .map(approvalStatus -> approvalStatus.operatorId)
                                .collect(Collectors.toSet());

                        if (approvedIds.contains(operatorId)) {
                            actions.addAll(List.of(Action.values()));
                        }
                    }
                }
            }
            case REJECTED, CANCELED -> {
                // Only the applicant or the proxy applicant can perform actions in the Rejected and Canceled states.
                if (!isInstanceApplicant(instance, operatorId)) {
                    actions.addAll(List.of(Action.values()));
                }
            }
            case APPROVED -> {

                actions.add(Action.RETRIEVE);

                // Only the applicant/proxy applicant can cancel or delete the instance in the approved status
                if (!isInstanceApplicant(instance, operatorId)) {
                    actions.add(Action.CANCEL);
                    actions.add(Action.WITHDRAW);
                }
                // If the operator has the permission to retrieve the instance, the retrieve action is not removed.
                if (isRetrieveOperator(instance, operatorId)) {
                    actions.remove(Action.RETRIEVE);
                }
            }
            //No actions are allowed in the finished status
            case FINISHED -> actions.addAll(List.of(Action.values()));
        }

        return Sets.newHashSet(actions);
    }

    /**
     * Generating the most basic action that needs to be removed
     *
     * @param definition
     * @param instance
     * @param operatorId
     * @return
     */
    private Set<Action> generateRemovalActionsByAdmin(Definition definition, Instance instance, String operatorId) {
        var actions = new HashSet<Action>();

        var status = instance.status;
        switch (status) {
            case PROCESSING -> {
                actions.add(Action.CANCEL);
                actions.add(Action.REJECT);
                actions.add(Action.WITHDRAW);
                actions.add(Action.RETRIEVE);
                actions.add(Action.APPLY);

                //If it is the first node, back and retrieve action is not allowed.
                if (NodeService.isFirstNode(definition, instance.nodeId)) {
                    actions.add(Action.BACK);
                }

                // If it is the last node, next action is not allowed.
                if (NodeService.isLastNode(definition, instance.nodeId)) {
                    actions.add(Action.NEXT);
                }

            }
            case REJECTED, CANCELED, APPROVED, FINISHED -> actions.addAll(List.of(Action.values()));
        }

        return Sets.newHashSet(actions);
    }

    /**
     * After the instance is complete, determine if the notification needs to be sent, and send the notification
     *
     * @param configuration
     * @param instance
     * @param node
     * @param action
     * @param extendParam
     */
    protected void handleSendNotification(ProcessConfiguration configuration, Instance instance, Node node, Action action, ActionExtendParam extendParam) {
        var send = false;
        var notificationMode = node.getNotificationModesByAction(action);

        Boolean selectedSend = null;
        Notification notification = null;
        if (ObjectUtils.isNotEmpty(extendParam)) {
            selectedSend = extendParam.selectedSend;
            notification = extendParam.notification;
        }

        send = whetherSendNotification(notificationMode, selectedSend);

        if (send) {
            configuration.sendNotification(instance, action, notification);
        }
    }

    // To determine if the operator is the applicant of the instance.
    private boolean isInstanceApplicant(Instance instance, String operatorId) {
        if (operatorId.equals(instance.applicant)) {
            return true;
        }
        if (operatorId.equals(instance.proxyApplicant)) {
            return true;
        }
        return false;
    }

    // To determine if the operator is eligible to retrieve the instance
    private boolean isRetrieveOperator(Instance instance, String operatorId) {
        return instance.preExpandOperatorIdSet.contains(operatorId);
    }

}
