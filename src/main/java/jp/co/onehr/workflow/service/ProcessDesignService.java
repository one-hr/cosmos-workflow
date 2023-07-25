package jp.co.onehr.workflow.service;

import java.util.List;

import io.github.thunderz99.cosmos.condition.Condition;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Workflow;

public class ProcessDesignService {

    private static final ProcessDesignService singleton = new ProcessDesignService();

    private ProcessDesignService() {
    }

    public static ProcessDesignService getService() {
        return singleton;
    }


    // === Workflow ===
    public Workflow createWorkflow(String host, Workflow workflow) throws Exception {
        return WorkflowService.singleton.create(host, workflow);
    }

    public Workflow upsertWorkflow(String host, Workflow workflow) throws Exception {
        return WorkflowService.singleton.upsert(host, workflow);
    }

    public Workflow getWorkflow(String host, String workflowId) throws Exception {
        return WorkflowService.singleton.getWorkflow(host, workflowId);
    }

    public List<Workflow> findWorkflows(String host, Condition cond) throws Exception {
        return WorkflowService.singleton.find(host, cond);
    }

    // === Definition ===
    public Definition upsertDefinition(String host, Definition definition) throws Exception {
        return DefinitionService.singleton.upsert(host, definition);
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
