package jp.co.onehr.workflow.service;

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

        var actions = setOperatorBasicAllowingActions(definition, instance, operatorId);

        var configuration = ProcessEngineConfiguration.getConfiguration();
        actions = configuration.handleAllowingActionsByOperator(definition, instance, actions, operatorId);

        instance.allowingActions.addAll(actions);
    }

    /**
     * The basic action rules for an instance.
     *
     * @param definition
     * @param instance
     * @param operatorId
     * @return
     */
    private Set<Action> setOperatorBasicAllowingActions(Definition definition, Instance instance, String operatorId) {
        // TODO SC_SAAS-15115
        return Sets.newHashSet();
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

}
