package jp.co.onehr.workflow;

import java.util.List;

import io.github.thunderz99.cosmos.condition.Condition;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Workflow;
import jp.co.onehr.workflow.dto.base.SimpleData;
import jp.co.onehr.workflow.service.ProcessDesignService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessDesign extends SimpleData {

    public static final Logger log = LoggerFactory.getLogger(ProcessDesign.class);


    private final ProcessDesignService processDesignService;

    protected ProcessDesign() {
        this.processDesignService = ProcessDesignService.getService();
    }

    // === Workflow ===
    public Workflow createWorkflow(String host, Workflow workflow) throws Exception {
        return processDesignService.createWorkflow(host, workflow);
    }

    public Workflow upsertWorkflow(String host, Workflow workflow) throws Exception {
        return processDesignService.upsertWorkflow(host, workflow);
    }

    public Workflow getWorkflow(String host, String workflowId) throws Exception {
        return processDesignService.getWorkflow(host, workflowId);
    }

    public List<Workflow> findWorkflows(String host, Condition cond) throws Exception {
        return processDesignService.findWorkflows(host, cond);
    }

    // === Definition ===
    public Definition upsertDefinition(String host, Definition definition) throws Exception {
        return processDesignService.upsertDefinition(host, definition);
    }

    public Definition getDefinition(String host, String definitionId) throws Exception {
        return processDesignService.getDefinition(host, definitionId);
    }

    public Definition getCurrentDefinition(String host, String workflowId, int version) throws Exception {
        return processDesignService.getCurrentDefinition(host, workflowId, version);
    }

    public List<Definition> findDefinitions(String host, Condition cond) throws Exception {
        return processDesignService.findDefinitions(host, cond);
    }
}
