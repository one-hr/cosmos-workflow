package jp.co.onehr.workflow.service;

import java.util.List;

import io.github.thunderz99.cosmos.condition.Condition;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Workflow;
import jp.co.onehr.workflow.dto.base.DeletedObject;
import jp.co.onehr.workflow.dto.param.DefinitionParam;
import jp.co.onehr.workflow.dto.param.WorkflowCreationParam;
import jp.co.onehr.workflow.dto.param.WorkflowUpdatingParam;

public class ProcessDesignService {

    private static final ProcessDesignService singleton = new ProcessDesignService();

    private ProcessDesignService() {
    }

    public static ProcessDesignService getService() {
        return singleton;
    }


    // === Workflow ===
    public Workflow createWorkflow(String host, WorkflowCreationParam creationParam) throws Exception {
        return WorkflowService.singleton.create(host, creationParam);
    }

    public Workflow updateWorkflow(String host, WorkflowUpdatingParam updatingParam) throws Exception {
        return WorkflowService.singleton.update(host, updatingParam);
    }

    public Workflow getWorkflow(String host, String workflowId) throws Exception {
        return WorkflowService.singleton.getWorkflow(host, workflowId);
    }

    public Workflow readWorkflow(String host, String workflowId) throws Exception {
        return WorkflowService.singleton.readSuppressing404(host, workflowId);
    }

    public DeletedObject deleteWorkflow(String host, String workflowId) throws Exception {
        return WorkflowService.singleton.delete(host, workflowId);
    }

    public List<Workflow> findWorkflows(String host, Condition cond) throws Exception {
        return WorkflowService.singleton.find(host, cond);
    }

    // === Definition ===
    public Definition upsertDefinition(String host, DefinitionParam param) throws Exception {
        return DefinitionService.singleton.upsert(host, param);
    }

    public Definition getDefinition(String host, String definitionId) throws Exception {
        return DefinitionService.singleton.getDefinition(host, definitionId);
    }

    public Definition getCurrentDefinition(String host, String workflowId, int version) throws Exception {
        return DefinitionService.singleton.getCurrentDefinition(host, workflowId, version);
    }

    public List<Definition> findDefinitions(String host, Condition cond) throws Exception {
        return DefinitionService.singleton.find(host, cond);
    }
}
