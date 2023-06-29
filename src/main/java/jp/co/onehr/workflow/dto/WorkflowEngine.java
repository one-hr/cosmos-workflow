package jp.co.onehr.workflow.dto;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import io.github.thunderz99.cosmos.CosmosDatabase;
import jp.co.onehr.workflow.dto.base.SimpleData;
import jp.co.onehr.workflow.service.OperatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WorkflowEngine extends SimpleData {

    public final static Logger log = LoggerFactory.getLogger(WorkflowEngine.class);

    /**
     * host -> database
     */
    static Map<String, CosmosDatabase> dbCache = Maps.newHashMap();

    /**
     * host -> collectionName
     */
    static Map<String, String> collectionCache = Maps.newHashMap();

    private WorkflowEngine() {

    }

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
}
