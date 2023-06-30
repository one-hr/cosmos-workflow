package jp.co.onehr.workflow.dto;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.github.thunderz99.cosmos.CosmosDatabase;
import jp.co.onehr.workflow.constant.NodeType;
import jp.co.onehr.workflow.dto.base.SimpleData;
import jp.co.onehr.workflow.service.OperatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Workflow engine, all processing needs to be done through the engine.
 * <p>
 * todo
 * Registering the relevant storage and processing, then it needs to be moved to the Configuration class
 */
public class WorkflowEngine extends SimpleData {

    public final static Logger log = LoggerFactory.getLogger(WorkflowEngine.class);

    private WorkflowEngine() {

    }

    // === Configuration and registration for Cosmos DB ===

    /**
     * host -> database
     */
    private static Map<String, CosmosDatabase> dbCache = Maps.newHashMap();

    /**
     * host -> collectionName
     */
    private static Map<String, String> collectionCache = Maps.newHashMap();

    /**
     * Register the database and collection used by the workflow
     *
     * @param host
     * @param db
     * @param collectionName
     */
    public static void registerDB(String host, CosmosDatabase db, String collectionName) {
        dbCache.put(host, db);
        collectionCache.put(host, collectionName);
    }

    public static CosmosDatabase getDatabase(String host) {
        return dbCache.get(host);
    }

    public static String getCollectionName(String host) {
        return collectionCache.get(host);
    }

    // === Handling of custom node operators  ===

    /**
     * Custom method for expanding operator
     */
    private static OperatorService operatorService;

    public static void registerOperatorService(OperatorService service) {
        operatorService = service;
    }

    public static Set<String> handleExpandOperators(Set<String> operatorIds) {
        if (operatorService != null) {
            return operatorService.handleOperators(operatorIds);
        }
        return operatorIds;
    }

    public static Set<String> handleExpandOrganizations(Set<String> orgIds) {
        if (operatorService != null) {
            return operatorService.handleOrganizations(orgIds);
        }
        return orgIds;
    }

    public static Map<String, ApprovalStatus> handleParallelApproval(Set<String> operatorIds, Set<String> orgIds, Set<String> expandOperatorIds) {
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

    private static Map<String, WorkflowPlugin> pluginCache = Maps.newHashMap();

    public static void registerPlugin(WorkflowPlugin plugin) {
        pluginCache.put(plugin.getType(), plugin);
    }

    public static WorkflowPlugin getPlugin(String pluginType) {
        return pluginCache.get(pluginType);
    }

    // === Configuration and registration for skip node type ===
    
    private static Set<String> skipNodeTypes = Sets.newHashSet(NodeType.RobotNode.name());

    public static void registerSkipNodeTypes(Set<String> skipTypes) {
        skipNodeTypes.addAll(skipTypes);
    }

    public static boolean isSkipNode(String nodeType) {
        return skipNodeTypes.contains(nodeType);
    }
}
