package jp.co.onehr.workflow.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.thunderz99.cosmos.condition.Condition;
import jp.co.onehr.workflow.EngineConfiguration;
import jp.co.onehr.workflow.constant.Action;
import jp.co.onehr.workflow.constant.NodeType;
import jp.co.onehr.workflow.constant.Status;
import jp.co.onehr.workflow.dto.ActionResult;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.base.DeletedObject;
import jp.co.onehr.workflow.dto.param.ActionExtendParam;
import jp.co.onehr.workflow.dto.param.ApplicationParam;
import jp.co.onehr.workflow.service.base.BaseCRUDService;


public class InstanceService extends BaseCRUDService<Instance> {

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
     * @param instance
     * @param action
     * @param extendParam
     * @return
     * @throws Exception
     */
    protected ActionResult resolve(String host, Instance instance, Action action, String operatorId, ActionExtendParam extendParam) throws Exception {

        var definition = DefinitionService.singleton.getDefinition(host, instance.definitionId);

        var result = action.execute(definition, instance, operatorId, extendParam);

        var updateNode = result.node;
        var configuration = EngineConfiguration.getConfiguration();

        if (configuration.isSkipNode(updateNode.getType())) {
            result = action.execute(definition, instance, operatorId, extendParam);
        }

        var updatedInstance = super.update(host, instance);

        result.instance = updatedInstance;

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

        var actions = setBasicAllowingActions(definition, instance, operatorId);

        var configuration = EngineConfiguration.getConfiguration();
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
    private Set<Action> setBasicAllowingActions(Definition definition, Instance instance, String operatorId) {

        var actions = new HashSet<Action>();

        var currentNode = NodeService.getCurrentNode(definition, instance.nodeId);
        if (NodeType.SingleNode.isEqual(currentNode.getType()) || NodeType.MultipleNode.isEqual(currentNode.getType())) {
            if (!instance.expandOperatorIdSet.contains(operatorId)) {
                return actions;
            }
        }

        // The first node only allows "save" and "next" actions
        if (NodeService.isFirstNode(definition, instance.nodeId)) {
            actions.add(Action.NEXT);
            actions.add(Action.SAVE);
            return actions;
        }

        // The last node only allows "save" and "back" actions
        if (NodeService.isLastNode(definition, instance.nodeId)) {
            actions.add(Action.SAVE);
            actions.add(Action.BACK);
            return actions;
        }

        // In other cases, the node allows all actions
        actions.add(Action.NEXT);
        actions.add(Action.SAVE);
        actions.add(Action.BACK);
        return actions;
    }
}
