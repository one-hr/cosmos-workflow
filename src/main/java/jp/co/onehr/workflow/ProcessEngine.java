package jp.co.onehr.workflow;

import java.util.List;

import io.github.thunderz99.cosmos.condition.Condition;
import jp.co.onehr.workflow.constant.Action;
import jp.co.onehr.workflow.constant.OperationMode;
import jp.co.onehr.workflow.dto.ActionResult;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.base.SimpleData;
import jp.co.onehr.workflow.dto.param.*;
import jp.co.onehr.workflow.service.ProcessEngineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process engine, all operations performed on the workflow need to be done through the engine
 * and cannot directly call other methods within the workflow
 */
public class ProcessEngine extends SimpleData {

    public static final Logger log = LoggerFactory.getLogger(ProcessEngine.class);

    private final ProcessConfiguration configuration;

    private final ProcessEngineService service;

    protected ProcessEngine(ProcessConfiguration configuration) {
        this.configuration = configuration;
        this.service = ProcessEngineService.getService();
    }

    // === Instance ===
    public Instance startInstance(String host, ApplicationParam param) throws Exception {
        return service.startInstance(host, param);
    }

    public Instance getInstance(String host, String instanceId) throws Exception {
        return service.getInstance(host, instanceId);
    }

    public Instance getInstanceWithOps(String host, String instanceId, String operatorId) throws Exception {
        return service.getInstanceWithOps(host, instanceId, operatorId, OperationMode.OPERATOR_MODE);
    }

    public Instance getInstanceWithOps(String host, String instanceId, String operatorId, OperationMode operationMode) throws Exception {
        return service.getInstanceWithOps(host, instanceId, operatorId, operationMode);
    }

    public ActionResult resolve(String host, String instanceId, Action action, String operatorId) throws Exception {
        return service.resolve(host, instanceId, action, operatorId, null);
    }

    public ActionResult resolve(String host, String instanceId, Action action, String operatorId, ActionExtendParam extendParam) throws Exception {
        return service.resolve(host, instanceId, action, operatorId, extendParam);
    }

    public Instance rebinding(String host, String instanceId, String operatorId, RebindingParam rebindingParam) throws Exception {
        return service.rebinding(host, instanceId, operatorId, rebindingParam);
    }

    public List<Instance> bulkRebinding(String host, String workflowId, String operatorId, BulkRebindingParam bulkRebindingParam) throws Exception {
        return service.bulkRebinding(host, workflowId, operatorId, bulkRebindingParam);
    }

    public Instance relocate(String host, String definitionId, String operatorId, RelocateParam relocateParam) throws Exception {
        return service.relocate(host, definitionId, operatorId, relocateParam);
    }

    public List<Instance> findInstances(String host, Condition cond) throws Exception {
        return service.findInstances(host, cond);
    }

    public Instance migrationInstance(String host, Instance instance) throws Exception {
        return service.migrationInstance(host, instance);
    }
}
