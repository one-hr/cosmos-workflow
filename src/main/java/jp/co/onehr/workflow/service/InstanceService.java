package jp.co.onehr.workflow.service;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.github.thunderz99.cosmos.condition.Condition;
import io.github.thunderz99.cosmos.condition.SubConditionType;
import jp.co.onehr.workflow.ProcessConfiguration;
import jp.co.onehr.workflow.constant.*;
import jp.co.onehr.workflow.contract.context.InstanceContext;
import jp.co.onehr.workflow.contract.context.OperatorLogContext;
import jp.co.onehr.workflow.contract.notification.Notification;
import jp.co.onehr.workflow.dto.ActionResult;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.OperateLog;
import jp.co.onehr.workflow.dto.base.BaseData;
import jp.co.onehr.workflow.dto.base.DeletedObject;
import jp.co.onehr.workflow.dto.node.Node;
import jp.co.onehr.workflow.dto.param.*;
import jp.co.onehr.workflow.exception.WorkflowException;
import jp.co.onehr.workflow.service.base.BaseCRUDService;
import jp.co.onehr.workflow.util.DateUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;


public class InstanceService extends BaseCRUDService<Instance> implements NotificationSendChangeable {

    public static final InstanceService singleton = new InstanceService();

    public static final int DATA_LIMIT = 50_000;

    public static final String WORKFLOW_ID = "workflowId";
    public static final String DEFINITION_ID = "definitionId";
    public static final String STATUS = "status";

    // Enable recursive action for nodes.
    public static final Set<Action> recursiveAction = Set.of(Action.NEXT, Action.BACK, Action.REAPPLY);

    private InstanceService() {
        super(Instance.class);
    }

    @Override
    protected Instance readSuppressing404(String host, String id) throws Exception {
        return super.readSuppressing404(host, id);
    }

    @Override
    protected Instance upsert(String host, Instance data) throws Exception {
        return super.upsert(host, data);
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

        var configuration = ProcessConfiguration.getConfiguration();

        var instance = new Instance(workflow.id, definition.id);
        instance.status = Status.PROCESSING;
        instance.setApplicationInfo(param);

        var operatorId = ApplicationMode.SELF.equals(param.applicationMode) ? param.applicant : param.proxyApplicant;
        var firstNode = NodeService.getFirstNode(definition);
        instance.nodeId = firstNode.nodeId;
        firstNode.resetCurrentOperators(instance, param.instanceContext);
        firstNode.resetParallelApproval(instance, firstNode.getApprovalType(), Action.APPLY, operatorId, param.instanceContext);
        firstNode.handleFirstNode(definition, instance);

        var operateLog = new OperateLog();
        operateLog.nodeId = firstNode.nodeId;
        operateLog.nodeName = firstNode.nodeName;
        operateLog.nodeType = firstNode.getClass().getSimpleName();
        operateLog.statusBefore = Status.NEW;
        operateLog.operatorId = operatorId;
        operateLog.action = Action.APPLY.name();
        operateLog.statusAfter = Status.PROCESSING;
        operateLog.comment = param.comment;
        operateLog.logContext = param.logContext;
        operateLog.operatorAt = DateUtil.nowDateTimeStringUTC();

        instance.operateLogList.add(operateLog);

        var result = super.create(host, instance);

        Notification notification = null;
        if (ObjectUtils.isNotEmpty(param)) {
            notification = param.notification;
        }

        var startNode = NodeService.getStartNode(definition);
        handleSendNotification(configuration, result, startNode, Action.APPLY, notification);

        return result;
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

        Notification notification = null;
        if (ObjectUtils.isNotEmpty(extendParam)) {
            notification = extendParam.notification;
        }
        handleSendNotification(configuration, updatedInstance, existNode, action, notification);

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

        // When the instance reaches the last node, the workflow ends, and the result is returned.
        if (NodeService.isLastNode(definition, instance.nodeId)) {
            return actionResult;
        }

        // When the instance reaches the first node, the workflow ends, and the result is returned.
        if (NodeService.isFirstNode(definition, instance.nodeId)) {
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
     * binding different versions of the definition to an instance
     * Warning: Reversion will reset the instance to the first node
     *
     * @param host
     * @param instanceId
     * @param rebindingParam
     * @return
     * @throws Exception
     */
    protected Instance rebinding(String host, String instanceId, String operatorId, RebindingParam rebindingParam) throws Exception {
        var existInstance = getInstance(host, instanceId);
        var existDefinition = DefinitionService.singleton.getDefinition(host, existInstance.definitionId);
        var isNodeExists = NodeService.checkNodeExists(existDefinition, existInstance.nodeId);

        Node existNode = null;
        if (isNodeExists) {
            existNode = NodeService.getNodeByNodeId(existDefinition, existInstance.nodeId);
        }

        var workflow = WorkflowService.singleton.getWorkflow(host, existInstance.workflowId);

        var version = workflow.currentVersion;
        if (ObjectUtils.isNotEmpty(rebindingParam) && rebindingParam.definitionVersion != null) {
            if (rebindingParam.definitionVersion > workflow.currentVersion) {
                throw new WorkflowException(WorkflowErrors.DEFINITION_NOT_EXIST, "The specified version definition does not exist", instanceId);
            }
            version = rebindingParam.definitionVersion;
        }

        var definition = DefinitionService.singleton.getCurrentDefinition(host, workflow.getId(), version);

        var contextParam = new ContextParam();
        contextParam.formRebindingParam(rebindingParam);

        return rebindingDefinition(host, definition, existInstance, operatorId, existNode, contextParam);
    }

    /**
     * Bulk Rebinding of Instances to Definitions
     * Warning: Reversion will reset the instance to the first node
     *
     * @param host
     * @param workflowId
     * @param operatorId
     * @param bulkRebindingParam
     * @return
     * @throws Exception
     */
    protected List<Instance> bulkRebinding(String host, String workflowId, String operatorId, BulkRebindingParam bulkRebindingParam) throws Exception {
        var workflow = WorkflowService.singleton.getWorkflow(host, workflowId);

        var version = workflow.currentVersion;
        if (ObjectUtils.isNotEmpty(bulkRebindingParam) && bulkRebindingParam.definitionVersion != null) {
            if (bulkRebindingParam.definitionVersion > workflow.currentVersion) {
                throw new WorkflowException(WorkflowErrors.DEFINITION_NOT_EXIST, "The specified version definition does not exist", workflowId);
            }
            version = bulkRebindingParam.definitionVersion;
        }

        var configuration = ProcessConfiguration.getConfiguration();

        var definitions = DefinitionService.singleton.find(host, Condition.filter(WORKFLOW_ID, workflowId).limit(DATA_LIMIT));

        var definitionMap = definitions.stream().collect(Collectors.toMap(BaseData::getId, i -> i));

        // target definition
        var definition = DefinitionService.singleton.getCurrentDefinition(host, workflow.getId(), version);

        var statuses = new HashSet<String>();
        if (ObjectUtils.isNotEmpty(bulkRebindingParam)) {
            statuses.addAll(bulkRebindingParam.statuses.stream().map(Enum::name).collect(Collectors.toSet()));
        }

        List<Condition> subConditions = Lists.newArrayList();
        subConditions.add(Condition.filter(WORKFLOW_ID, workflowId));

        var includeTargetDefinition = ObjectUtils.isNotEmpty(bulkRebindingParam) ? bulkRebindingParam.includeTargetDefinition : false;
        if (!includeTargetDefinition) {
            subConditions.add(Condition.filter(DEFINITION_ID + " != ", definition.id));
        }

        if (CollectionUtils.isNotEmpty(statuses)) {
            subConditions.add(Condition.filter(STATUS, statuses));
        }

        var condition = Condition.filter(SubConditionType.AND + " bulkInstance", subConditions);

        var instances = this.find(host, condition.limit(DATA_LIMIT));

        var result = new LinkedList<Instance>();

        for (var existInstance : instances) {
            var existDefinition = definitionMap.get(existInstance.definitionId);
            var isNodeExists = NodeService.checkNodeExists(existDefinition, existInstance.nodeId);

            Node existNode = null;
            if (isNodeExists) {
                existNode = NodeService.getNodeByNodeId(existDefinition, existInstance.nodeId);
            }

            var contextParam = new ContextParam();

            configuration.generateContextParam4Bulk(contextParam, existDefinition, existInstance, operatorId);

            var instance = rebindingDefinition(host, definition, existInstance, operatorId, existNode, contextParam);
            result.add(instance);
        }

        return result;
    }

    /**
     * Move the instance to any specified node
     *
     * @param host
     * @param definitionId
     * @param operatorId
     * @param relocateParam
     * @return
     * @throws Exception
     */
    protected Instance relocate(String host, String definitionId, String operatorId, RelocateParam relocateParam) throws Exception {
        var definition = DefinitionService.singleton.getDefinition(host, definitionId);

        if (ObjectUtils.isEmpty(relocateParam)) {
            throw new WorkflowException(WorkflowErrors.RELOCATE_PARAM_NOT_EXIST, "Relocate param cannot be null", definitionId);
        }

        var instanceId = relocateParam.instanceId;
        var relocateNodeId = relocateParam.relocateNodeId;

        if (StringUtils.isBlank(instanceId)) {
            throw new WorkflowException(WorkflowErrors.RELOCATE_INSTANCE_ID_NOT_EXIST, "The ID of the instance to be relocated does not exist", definitionId);
        }

        if (StringUtils.isBlank(relocateNodeId)) {
            throw new WorkflowException(WorkflowErrors.RELOCATE_NODE_ID_NOT_EXIST, "The ID of the node to be relocated does not exist", instanceId);
        }

        var existInstance = getInstance(host, instanceId);

        // Instance that has been approved cannot be relocated
        if (existInstance.status.equals(Status.APPROVED) || existInstance.status.equals(Status.FINISHED)) {
            throw new WorkflowException(WorkflowErrors.RELOCATE_INSTANCE_STATUS_INVALID, "The current status of the instance does not allow relocation", instanceId);
        }

        if (!existInstance.definitionId.equals(definitionId)) {
            throw new WorkflowException(WorkflowErrors.RELOCATE_DEFINITION_MISMATCH, "The instance and definition ID for relocation do not match", instanceId);
        }

        var existNode = NodeService.getNodeByNodeId(definition, existInstance.nodeId);

        // If the instance ID matches the relocateNode ID, no action will be taken, and the result will be returned directly
        if (existNode.nodeId.equals(relocateNodeId)) {
            return existInstance;
        }

        var relocateNode = NodeService.getNodeByNodeId(definition, relocateNodeId);

        Instance instance = existInstance.copy();
        instance.nodeId = relocateNode.nodeId;

        InstanceContext instanceContext = relocateParam.instanceContext;

        // Generate information related to the instance's preNode
        instance.preNodeId = "";
        instance.preExpandOperatorIdSet.clear();
        var previousNodeInfo = NodeService.getPreviousNodeInfo(definition, instance, instanceContext);
        if (!previousNodeInfo.isEmpty()) {
            instance.preNodeId = previousNodeInfo.nodeId;
            instance.preExpandOperatorIdSet.addAll(previousNodeInfo.expandOperatorIdSet);
        }

        // Generate information related to the current operator of the instance
        relocateNode.resetCurrentOperators(instance, instanceContext);
        relocateNode.resetParallelApproval(instance, relocateNode.getApprovalType(), Action.RELOCATE, operatorId, instanceContext);

        instance.status = Status.PROCESSING;
        if (NodeService.isLastNode(definition, instance.nodeId)) {
            instance.status = Status.APPROVED;
        }

        var comment = relocateParam.comment;
        var logContext = relocateParam.logContext;

        var operateLog = generateRelocateLog(existInstance, instance, operatorId, existNode, comment, logContext);
        instance.operateLogList.add(operateLog);

        return super.update(host, instance);
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
            var customRemovalActions = configuration.generateCustomRemovalActionsByAdmin(definition, instance, operatorId);
            actions.removeAll(customRemovalActions);

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
                actions.add(Action.REAPPLY);
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
        actions.add(Action.REAPPLY);

        var status = instance.status;
        switch (status) {
            case PROCESSING -> {
                actions.add(Action.RETRIEVE);

                //If it is the first node, back and retrieve action is not allowed.
                // the reapply action can be used.
                if (NodeService.isFirstNode(definition, instance.nodeId)) {
                    actions.add(Action.BACK);
                    actions.remove(Action.REAPPLY);
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

                    // When in progress, the applicant or their proxy can cancel or delete the instance at any time
                    if (isInstanceApplicant(instance, operatorId)) {
                        actions.remove(Action.CANCEL);
                        actions.remove(Action.WITHDRAW);
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
            case REJECTED, CANCELED, APPROVED, FINISHED -> actions.addAll(List.of(Action.RETRIEVE, Action.APPLY));
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
     * @param notification
     */
    protected void handleSendNotification(ProcessConfiguration configuration, Instance instance, Node node, Action action, Notification notification) {
        var send = false;
        var notificationMode = node.getNotificationModesByAction(action);

        if (ObjectUtils.isNotEmpty(notification)) {
            Boolean selectedSend = notification.selectedSend;
            send = whetherSendNotification(notificationMode, selectedSend);
        }

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

    /**
     * Instance Rebinding of Definition
     *
     * @param definition
     * @param existInstance
     * @param operatorId
     * @param existNode
     * @param contextParam
     * @return
     * @throws Exception
     */
    private Instance rebindingDefinition(String host, Definition definition, Instance existInstance, String operatorId, Node existNode,
                                         ContextParam contextParam) throws Exception {
        Instance instance = existInstance.copy();

        instance.definitionId = definition.id;

        var firstNode = NodeService.getFirstNode(definition);
        instance.nodeId = firstNode.nodeId;
        instance.preNodeId = "";
        instance.preExpandOperatorIdSet.clear();

        var instanceContext = ObjectUtils.isNotEmpty(contextParam) ? contextParam.instanceContext : null;
        var comment = ObjectUtils.isNotEmpty(contextParam) ? contextParam.comment : "";
        var logContext = ObjectUtils.isNotEmpty(contextParam) ? contextParam.logContext : null;

        firstNode.resetCurrentOperators(instance, instanceContext);
        firstNode.resetParallelApproval(instance, firstNode.getApprovalType(), Action.REBINDING, operatorId, instanceContext);
        firstNode.handleFirstNode(definition, instance);

        var operateLog = generateRebindingLog(existInstance, operatorId, existNode, comment, logContext);
        instance.operateLogList.add(operateLog);

        return super.update(host, instance);
    }

    /**
     * Generate operation log for instance rebind definition
     *
     * @param existInstance
     * @param operatorId
     * @param existNode
     * @param comment
     * @param logContext
     * @return
     */
    private OperateLog generateRebindingLog(Instance existInstance, String operatorId, Node existNode, String comment, OperatorLogContext logContext) {
        var operateLog = new OperateLog();

        if (existNode != null) {
            operateLog.nodeId = existNode.nodeId;
            operateLog.nodeName = existNode.nodeName;
            operateLog.nodeType = existNode.getClass().getSimpleName();
        }

        operateLog.statusBefore = existInstance.status;
        operateLog.operatorId = operatorId;
        operateLog.action = Action.REBINDING.name();
        operateLog.statusAfter = Status.PROCESSING;
        operateLog.comment = comment;
        operateLog.logContext = logContext;
        operateLog.operatorAt = DateUtil.nowDateTimeStringUTC();

        return operateLog;
    }

    /**
     * Generate operation log for instance relocate
     *
     * @param existInstance
     * @param instance
     * @param operatorId
     * @param existNode
     * @param comment
     * @param logContext
     * @return
     * @throws Exception
     */
    private OperateLog generateRelocateLog(Instance existInstance, Instance instance, String operatorId, Node existNode, String comment, OperatorLogContext logContext) throws Exception {
        var operateLog = new OperateLog();

        operateLog.nodeId = existNode.nodeId;
        operateLog.nodeName = existNode.nodeName;
        operateLog.nodeType = existNode.getClass().getSimpleName();

        operateLog.statusBefore = existInstance.status;
        operateLog.operatorId = operatorId;
        operateLog.action = Action.RELOCATE.name();
        operateLog.statusAfter = instance.status;
        operateLog.comment = comment;
        operateLog.logContext = logContext;
        operateLog.operatorAt = DateUtil.nowDateTimeStringUTC();

        return operateLog;
    }
}
