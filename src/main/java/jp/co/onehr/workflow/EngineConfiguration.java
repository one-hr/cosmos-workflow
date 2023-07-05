package jp.co.onehr.workflow;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.github.thunderz99.cosmos.CosmosDatabase;
import jp.co.onehr.workflow.constant.NodeType;
import jp.co.onehr.workflow.dto.ApprovalStatus;
import jp.co.onehr.workflow.dto.WorkflowPlugin;
import jp.co.onehr.workflow.service.OperatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The configuration class for the workflow is responsible for configuring the workflow's database, custom methods, plugins
 * <p>
 * Once the configuration is complete, the workflow engine can be built
 * the engine can only be constructed through the configuration class
 */
public class EngineConfiguration {

    public final static Logger log = LoggerFactory.getLogger(EngineConfiguration.class);

    private static final EngineConfiguration singleton = new EngineConfiguration();

    /**
     * host -> database
     */
    private Map<String, CosmosDatabase> dbCache = Maps.newHashMap();

    /**
     * host -> collectionName
     */
    private Map<String, String> collectionCache = Maps.newHashMap();

    private OperatorService operatorService;

    private Map<String, WorkflowPlugin> pluginCache = Maps.newHashMap();

    private Set<String> skipNodeTypes = Sets.newHashSet(NodeType.RobotNode.name());

    private EngineConfiguration() {

    }

    public static EngineConfiguration getConfiguration() {
        return singleton;
    }

    /**
     * build workflow engine
     *
     * @return
     */
    public WorkflowEngine buildEngine() {
        return new WorkflowEngine(this);
    }

    // === Configuration and registration for Cosmos DB ===

    public void registerDB(String host, CosmosDatabase db, String collectionName) {
        dbCache.put(host, db);
        collectionCache.put(host, collectionName);
    }

    public CosmosDatabase getDatabase(String host) {
        return dbCache.get(host);
    }

    public String getCollectionName(String host) {
        return collectionCache.get(host);
    }

    // === Handling of custom node operators  ===

    public void registerOperatorService(OperatorService service) {
        operatorService = service;
    }

    public Set<String> handleExpandOperators(Set<String> operatorIds) {
        if (operatorService != null) {
            return operatorService.handleOperators(operatorIds);
        }
        return operatorIds;
    }

    public Set<String> handleExpandOrganizations(Set<String> orgIds) {
        if (operatorService != null) {
            return operatorService.handleOrganizations(orgIds);
        }
        return orgIds;
    }

    public Map<String, ApprovalStatus> handleParallelApproval(Set<String> operatorIds, Set<String> orgIds, Set<String> expandOperatorIds) {
        var parallelApprovalMap = new HashMap<String, ApprovalStatus>();

        if (operatorService != null) {
            return operatorService.handleParallelApproval(operatorIds, orgIds, expandOperatorIds);
        }

        for (var expandOperatorId : expandOperatorIds) {
            parallelApprovalMap.put(expandOperatorId, new ApprovalStatus(expandOperatorId, false));
        }

        return parallelApprovalMap;
    }

    // === Configuration and registration for plugin ===

    public void registerPlugin(WorkflowPlugin plugin) {
        pluginCache.put(plugin.getType(), plugin);
    }

    public WorkflowPlugin getPlugin(String pluginType) {
        return pluginCache.get(pluginType);
    }

    // === Configuration and registration for skip node type ===

    public void registerSkipNodeTypes(Set<String> skipTypes) {
        skipNodeTypes.addAll(skipTypes);
    }

    public boolean isSkipNode(String nodeType) {
        return skipNodeTypes.contains(nodeType);
    }
}
