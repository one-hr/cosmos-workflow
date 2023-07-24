package jp.co.onehr.workflow.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import io.github.thunderz99.cosmos.condition.Condition;
import jp.co.onehr.workflow.ProcessEngineConfiguration;
import jp.co.onehr.workflow.constant.Action;
import jp.co.onehr.workflow.constant.Status;
import jp.co.onehr.workflow.constant.WorkflowErrors;
import jp.co.onehr.workflow.contract.notification.Notification;
import jp.co.onehr.workflow.dto.ActionResult;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.base.DeletedObject;
import jp.co.onehr.workflow.dto.node.Node;
import jp.co.onehr.workflow.dto.param.ActionExtendParam;
import jp.co.onehr.workflow.dto.param.ApplicationParam;
import jp.co.onehr.workflow.exception.WorkflowException;
import jp.co.onehr.workflow.service.base.BaseCRUDService;
import org.apache.commons.lang3.ObjectUtils;


public class InstanceService extends BaseCRUDService<Instance> implements NotificationSendChangeable {

    public static final InstanceService singleton = new InstanceService();

    private InstanceService() {
        super(Instance.class);
    }

    @Override
    protected Instance readSuppressing404(String host, String id) throws Exception {
        return super.readSuppressing404(host, id);
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
        var Instance = super.readSuppressing404(host, instanceId);
        if (ObjectUtils.isEmpty(Instance)) {
            throw new WorkflowException(WorkflowErrors.INSTANCE_NOT_EXIST, "The instance does not exist in the database", instanceId);
        }
        return Instance;
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

        var firstNode = NodeService.getFirstNode(definition);
        instance.nodeId = firstNode.nodeId;
        firstNode.resetCurrentOperators(instance);
        firstNode.handleFirstNode(definition, instance);

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

        var configuration = ProcessEngineConfiguration.getConfiguration();

        // TODO required checking. fail-fast
        var existNode = NodeService.getNodeByInstance(definition, existInstance);

        var result = action.execute(definition, existInstance, operatorId, extendParam);

        var updatedInstance = result.instance;

        var updatedNode = result.node;

        if (configuration.isSkipNode(updatedNode.getType())) {
            result = action.execute(definition, updatedInstance, operatorId, extendParam);
            updatedInstance = result.instance;
        }

        // Delete the instance if withdraw
        if (result.withdraw) {
            delete(host, updatedInstance.id);
        } else {
            // If the instance reaches the last node, the status is changed to Approved.
            if (NodeService.isLastNode(definition, updatedInstance.nodeId)) {
                updatedInstance.status = Status.APPROVED;
            }
            result.instance = super.update(host, updatedInstance);
        }

        handleSendNotification(configuration, updatedInstance, existNode, action, extendParam);

        return result;
    }

    /**
     * Set the allowed actions for the instance.
     *
     * @param definition
     * @param instance
     * @param operatorId
     */
    public void setAllowingActions(Definition definition, Instance instance, String operatorId) {
        instance.allowingActions.clear();

        var configuration = ProcessEngineConfiguration.getConfiguration();

        var actions = generateActionsByStatus(instance);

        var customRemovalActions = configuration.generateCustomRemovalActionsByOperator(definition, instance, operatorId);
        actions.removeAll(customRemovalActions);

        var removalActions = generateRemovalActionsByOperator(definition, instance, operatorId);
        actions.removeAll(removalActions);

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
                }
            }
            case REJECTED, CANCELED -> {
                // Only the applicant or the proxy applicant can perform actions in the Rejected and Canceled states.
                if (!isInstanceApplicant(instance, operatorId)) {
                    actions.addAll(List.of(Action.values()));
                }
                // todo Rejection and cancellation handling involves returning to the first node.
                //  If the first node's operator is not the applicant, the handling for the first node's operator needs to be determined.
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
     * After the instance is complete, determine if the notification needs to be sent, and send the notification
     *
     * @param configuration
     * @param instance
     * @param node
     * @param action
     * @param extendParam
     */
    private void handleSendNotification(ProcessEngineConfiguration configuration, Instance instance, Node node, Action action, ActionExtendParam extendParam) {
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
