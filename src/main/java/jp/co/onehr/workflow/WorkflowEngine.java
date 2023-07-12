package jp.co.onehr.workflow;

import java.util.List;

import io.github.thunderz99.cosmos.condition.Condition;
import jp.co.onehr.workflow.constant.Action;
import jp.co.onehr.workflow.dto.ActionResult;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.Workflow;
import jp.co.onehr.workflow.dto.base.SimpleData;
import jp.co.onehr.workflow.dto.param.ActionExtendParam;
import jp.co.onehr.workflow.dto.param.ApplicationParam;
import jp.co.onehr.workflow.service.WorkflowEngineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Workflow engine, all operations performed on the workflow need to be done through the engine
 * and cannot directly call other methods within the workflow
 */
public class WorkflowEngine extends SimpleData {

    public static final Logger log = LoggerFactory.getLogger(WorkflowEngine.class);

    private EngineConfiguration configuration;

    private WorkflowEngineService service;

    protected WorkflowEngine(EngineConfiguration configuration) {
        this.configuration = configuration;
        this.service = WorkflowEngineService.getService();
    }

    // === Workflow ===
    public Workflow createWorkflow(String host, Workflow workflow) throws Exception {
        return service.createWorkflow(host, workflow);
    }

    public Workflow upsertWorkflow(String host, Workflow workflow) throws Exception {
        return service.upsertWorkflow(host, workflow);
    }

    public Workflow getWorkflow(String host, String workflowId) throws Exception {
        return service.getWorkflow(host, workflowId);
    }

    public List<Workflow> findWorkflows(String host, Condition cond) throws Exception {
        return service.findWorkflows(host, cond);
    }

    // === Definition ===
    public Definition upsertDefinition(String host, Definition definition) throws Exception {
        return service.upsertDefinition(host, definition);
    }

    public Definition getDefinition(String host, String definitionId) throws Exception {
        return service.getDefinition(host, definitionId);
    }

    public Definition getCurrentDefinition(String host, String workflowId, int version) throws Exception {
        return service.getCurrentDefinition(host, workflowId, version);
    }

    public List<Definition> findDefinitions(String host, Condition cond) throws Exception {
        return service.findDefinitions(host, cond);
    }

    // === Instance ===
    public Instance startInstance(String host, ApplicationParam param) throws Exception {
        return service.startInstance(host, param);
    }

    public Instance getInstance(String host, String instanceId) throws Exception {
        return service.getInstance(host, instanceId);
    }

    public Instance getInstanceWithOps(String host, String instanceId, String operatorId) throws Exception {
        return service.getInstanceWithOps(host, instanceId, operatorId);
    }

    public ActionResult resolve(String host, Instance instance, Action action, String operatorId) throws Exception {
        // TODO Warning instance from invoker is not reliable.
        return service.resolve(host, instance, action, operatorId, null);
    }

    public ActionResult resolve(String host, Instance instance, Action action, String operatorId, ActionExtendParam extendParam) throws Exception {
        // TODO Warning instance from invoker is not reliable.
        return service.resolve(host, instance, action, operatorId, extendParam);
    }

    public List<Instance> findInstances(String host, Condition cond) throws Exception {
        return service.findInstances(host, cond);
    }

}
