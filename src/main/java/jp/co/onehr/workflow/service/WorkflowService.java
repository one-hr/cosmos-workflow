package jp.co.onehr.workflow.service;

import java.util.List;

import io.github.thunderz99.cosmos.condition.Condition;
import jp.co.onehr.workflow.constant.WorkflowErrors;
import jp.co.onehr.workflow.dto.Workflow;
import jp.co.onehr.workflow.dto.base.DeletedObject;
import jp.co.onehr.workflow.dto.param.WorkflowCreationParam;
import jp.co.onehr.workflow.dto.param.WorkflowUpdatingParam;
import jp.co.onehr.workflow.exception.WorkflowException;
import jp.co.onehr.workflow.service.base.BaseCRUDService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;


public class WorkflowService extends BaseCRUDService<Workflow> {

    public static final WorkflowService singleton = new WorkflowService();

    private WorkflowService() {
        super(Workflow.class);
    }

    protected Workflow create(String host, WorkflowCreationParam creationParam) throws Exception {
        if (StringUtils.isBlank(creationParam.name)) {
            throw new WorkflowException(WorkflowErrors.WORKFLOW_NANE_INVALID, "The name field is mandatory when creating a workflow", host);
        }

        var workflow = new Workflow();
        var id = creationParam.id;
        if (StringUtils.isBlank(id)) {
            id = generateId(workflow);
        }

        workflow.id = id;
        workflow.name = creationParam.name;
        workflow.enableVersion = creationParam.enableVersion;

        var definition = DefinitionService.singleton.createInitialDefinition(host, workflow, creationParam);
        workflow.currentVersion = definition.version;
        return this.create(host, workflow);
    }

    protected Workflow update(String host, WorkflowUpdatingParam updatingParam) throws Exception {
        if (StringUtils.isBlank(updatingParam.id)) {
            throw new WorkflowException(WorkflowErrors.WORKFLOW_ID_INVALID, "The id field is mandatory when creating a workflow", host);
        }

        var workflow = this.readSuppressing404(host, updatingParam.id);

        if (StringUtils.isNotEmpty(updatingParam.name)) {
            workflow.name = updatingParam.name;
        }

        if (ObjectUtils.isNotEmpty(updatingParam.enableVersion)) {
            workflow.enableVersion = updatingParam.enableVersion;
        }
        return this.update(host, workflow);
    }

    @Override
    protected Workflow update(String host, Workflow data) throws Exception {
        return super.update(host, data);
    }

    @Override
    protected Workflow readSuppressing404(String host, String id) throws Exception {
        return super.readSuppressing404(host, id);
    }

    @Override
    protected DeletedObject purge(String host, String id) throws Exception {

        var definitions = DefinitionService.singleton.find(host, Condition.filter("workflowId", id).fields("id"));
        for (var definition : definitions) {
            if (StringUtils.isNotEmpty(definition.getId())) {
                DefinitionService.singleton.purge(host, definition.getId());
            }
        }

        var instances = InstanceService.singleton.find(host, Condition.filter("workflowId", id).fields("id"));
        for (var instance : instances) {
            if (StringUtils.isNotEmpty(instance.getId())) {
                InstanceService.singleton.purge(host, instance.getId());
            }
        }

        return super.purge(host, id);
    }

    @Override
    protected DeletedObject delete(String host, String id) throws Exception {

        var definitions = DefinitionService.singleton.find(host, Condition.filter("workflowId", id).fields("id"));
        for (var definition : definitions) {
            if (StringUtils.isNotEmpty(definition.getId())) {
                DefinitionService.singleton.delete(host, definition.getId());
            }
        }

        var instances = InstanceService.singleton.find(host, Condition.filter("workflowId", id).fields("id"));
        for (var instance : instances) {
            if (StringUtils.isNotEmpty(instance.getId())) {
                InstanceService.singleton.delete(host, instance.getId());
            }
        }

        return super.delete(host, id);
    }

    @Override
    protected List<Workflow> find(String host, Condition cond) throws Exception {
        return super.find(host, cond);
    }

    /**
     * Make sure Workflow is existed
     *
     * @param host
     * @param workflowId
     * @return
     * @throws Exception
     */
    protected Workflow getWorkflow(String host, String workflowId) throws Exception {
        var workflow = WorkflowService.singleton.readSuppressing404(host, workflowId);
        if (ObjectUtils.isEmpty(workflow)) {
            throw new WorkflowException(WorkflowErrors.WORKFLOW_NOT_EXIST, "The workflow does not exist in the database", workflowId);
        }
        return workflow;
    }
}
