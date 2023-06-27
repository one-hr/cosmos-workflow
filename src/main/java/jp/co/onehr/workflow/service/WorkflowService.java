package jp.co.onehr.workflow.service;

import io.github.thunderz99.cosmos.condition.Condition;
import jp.co.onehr.workflow.constant.WorkflowErrors;
import jp.co.onehr.workflow.dto.Workflow;
import jp.co.onehr.workflow.dto.base.DeletedObject;
import jp.co.onehr.workflow.exception.WorkflowException;
import jp.co.onehr.workflow.service.base.BaseCRUDService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;


public class WorkflowService extends BaseCRUDService<Workflow> {

    public static final WorkflowService singleton = new WorkflowService();

    WorkflowService() {
        super(Workflow.class);
    }

    @Override
    public Workflow create(String host, Workflow workflow) throws Exception {
        workflow.id = generateId(workflow);
        var definition = DefinitionService.singleton.createInitialDefinition(host, workflow);
        workflow.currentVersion = definition.version;
        return super.create(host, workflow);
    }

    @Override
    public Workflow upsert(String host, Workflow workflow) throws Exception {
        workflow.id = generateId(workflow);
        return super.upsert(host, workflow);
    }

    @Override
    public DeletedObject purge(String host, String id) throws Exception {

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

    /**
     * Make sure Workflow is existed
     *
     * @param host
     * @param workflowId
     * @return
     * @throws Exception
     */
    public Workflow getWorkflow(String host, String workflowId) throws Exception {
        var workflow = WorkflowService.singleton.readSuppressing404(host, workflowId);
        if (ObjectUtils.isEmpty(workflow)) {
            throw new WorkflowException(WorkflowErrors.WORKFLOW_NOT_EXIST, "The workflow does not exist in the database", workflowId);
        }
        return workflow;
    }
}
