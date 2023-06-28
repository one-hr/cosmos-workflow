package jp.co.onehr.workflow.service;

import jp.co.onehr.workflow.constant.Action;
import jp.co.onehr.workflow.constant.Status;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.param.ActionExtendParam;
import jp.co.onehr.workflow.dto.param.ApplicationParam;
import jp.co.onehr.workflow.service.base.BaseCRUDService;


public class InstanceService extends BaseCRUDService<Instance> {

    public static final InstanceService singleton = new InstanceService();

    InstanceService() {
        super(Instance.class);
    }

    /**
     * Start an instance based on the workflow definition
     *
     * @param host
     * @param param
     * @return
     * @throws Exception
     */
    public Instance start(String host, ApplicationParam param) throws Exception {
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

    public Instance resolve(String host, Instance instance, Action action, String operatorId) throws Exception {
        return resolve(host, instance, action, operatorId, null);
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
    public Instance resolve(String host, Instance instance, Action action, String operatorId, ActionExtendParam extendParam) throws Exception {

        var definition = DefinitionService.singleton.getDefinition(host, instance.definitionId);

        action.execute(definition, instance, operatorId, extendParam);

        return super.update(host, instance);
    }

    /**
     * todo Set the allowed actions for the instance.
     *
     * @param definition
     * @param instance
     * @param operatorId
     */
    public void setAllowingActions(Definition definition, Instance instance, String operatorId) {

    }
}
