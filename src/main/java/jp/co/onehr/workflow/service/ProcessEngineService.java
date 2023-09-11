package jp.co.onehr.workflow.service;

import java.util.List;

import io.github.thunderz99.cosmos.condition.Condition;
import jp.co.onehr.workflow.constant.Action;
import jp.co.onehr.workflow.constant.OperationMode;
import jp.co.onehr.workflow.dto.ActionResult;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.param.ActionExtendParam;
import jp.co.onehr.workflow.dto.param.ApplicationParam;
import jp.co.onehr.workflow.dto.param.BulkRebindingParam;
import jp.co.onehr.workflow.dto.param.RebindingParam;

/**
 * The service corresponding to the workflow engine is called the Workflow Engine Service
 * <p>
 * The service responsible for exposing private methods
 * allowing users to perform corresponding operations through the engine
 */
public class ProcessEngineService {

    private static final ProcessEngineService singleton = new ProcessEngineService();

    private ProcessEngineService() {
    }

    public static ProcessEngineService getService() {
        return singleton;
    }


    // === Instance ===
    public Instance startInstance(String host, ApplicationParam param) throws Exception {
        return InstanceService.singleton.start(host, param);
    }

    public Instance getInstance(String host, String instanceId) throws Exception {
        Instance instance = InstanceService.singleton.readSuppressing404(host, instanceId);
        return instance;
    }

    /**
     * Get the instance with the allowed actions.
     *
     * @param host
     * @param instanceId
     * @param operatorId
     * @return
     * @throws Exception
     */
    public Instance getInstanceWithOps(String host, String instanceId, String operatorId, OperationMode operationMode) throws Exception {
        var instance = InstanceService.singleton.readSuppressing404(host, instanceId);
        var definition = DefinitionService.singleton.getDefinition(host, instance.definitionId);
        InstanceService.singleton.setAllowingActions(definition, instance, operatorId, operationMode);

        return instance;
    }

    public ActionResult resolve(String host, String instanceId, Action action, String operatorId, ActionExtendParam extendParam) throws Exception {
        return InstanceService.singleton.resolve(host, instanceId, action, operatorId, extendParam);
    }

    public Instance rebinding(String host, String instanceId, String operatorId, RebindingParam rebindingParam) throws Exception {
        return InstanceService.singleton.rebinding(host, instanceId, operatorId, rebindingParam);
    }

    public List<Instance> bulkRebinding(String host, String workflowId, String operatorId, BulkRebindingParam bulkRebindingParam) throws Exception {
        return InstanceService.singleton.bulkRebinding(host, workflowId, operatorId, bulkRebindingParam);
    }

    public List<Instance> findInstances(String host, Condition cond) throws Exception {
        List<Instance> instances = InstanceService.singleton.find(host, cond);
        return instances;
    }
}
