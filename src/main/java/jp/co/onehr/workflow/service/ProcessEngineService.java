package jp.co.onehr.workflow.service;

import java.util.List;

import io.github.thunderz99.cosmos.condition.Condition;
import jp.co.onehr.workflow.constant.Action;
import jp.co.onehr.workflow.dto.ActionResult;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.param.ActionExtendParam;
import jp.co.onehr.workflow.dto.param.ApplicationParam;

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
    public Instance getInstanceWithOps(String host, String instanceId, String operatorId) throws Exception {
        var instance = InstanceService.singleton.readSuppressing404(host, instanceId);
        var definition = DefinitionService.singleton.getDefinition(host, instance.definitionId);
        InstanceService.singleton.setAllowingActions(definition, instance, operatorId);

        return instance;
    }

    public ActionResult resolve(String host, String instanceId, Action action, String operatorId, ActionExtendParam extendParam) throws Exception {
        return InstanceService.singleton.resolve(host, instanceId, action, operatorId, extendParam);
    }

    public List<Instance> findInstances(String host, Condition cond) throws Exception {
        List<Instance> instances = InstanceService.singleton.find(host, cond);
        return instances;
    }
}
