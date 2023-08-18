package jp.co.onehr.workflow;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import io.github.thunderz99.cosmos.CosmosDatabase;
import jp.co.onehr.workflow.constant.Action;
import jp.co.onehr.workflow.contract.notification.Notification;
import jp.co.onehr.workflow.contract.notification.NotificationSender;
import jp.co.onehr.workflow.contract.operator.OperatorService;
import jp.co.onehr.workflow.contract.plugin.WorkflowPlugin;
import jp.co.onehr.workflow.contract.restriction.ActionRestriction;
import jp.co.onehr.workflow.dto.ApprovalStatus;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The configuration class for the workflow is responsible for configuring the workflow's database, custom methods, plugins
 * <p>
 * Once the configuration is complete, the workflow engine can be built
 * the engine can only be constructed through the configuration class
 */
public class ProcessConfiguration {

    public static final Logger log = LoggerFactory.getLogger(ProcessConfiguration.class);

    private static final ProcessConfiguration singleton = new ProcessConfiguration();

    /**
     * host -> database
     */
    private Map<String, CosmosDatabase> dbCache = Maps.newHashMap();

    /**
     * host -> collectionName
     */
    private Map<String, String> collectionCache = Maps.newHashMap();

    /**
     * Custom suffix for partition
     */
    private String partitionSuffix;

    /**
     * User-defined handling of operator IDs in the instance.
     */
    private OperatorService operatorService;

    /**
     * Whether to reset all parallel approval states during retrieval
     * <p>
     * By default, it is set to false, and only the operator who retrieves the instance needs to approve.
     */
    private boolean retrieveResetParallelApproval = false;

    /**
     * Customized plugins available for the workflow.
     */
    private Map<String, WorkflowPlugin> pluginCache = Maps.newHashMap();

    /**
     * Customized sending message notification functionality.
     */
    private NotificationSender notificationSender;

    /**
     * Customized instance action restrictions.
     */
    private ActionRestriction actionRestriction;

    private ProcessConfiguration() {

    }

    public static ProcessConfiguration getConfiguration() {
        return singleton;
    }

    public ProcessDesign buildProcessDesign() {
        return new ProcessDesign();
    }

    /**
     * build workflow engine
     *
     * @return
     */
    public ProcessEngine buildProcessEngine() {
        return new ProcessEngine(this);
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

    public void setPartitionSuffix(String partitionSuffix) {
        this.partitionSuffix = partitionSuffix;
    }

    public String getPartitionSuffix() {
        return this.partitionSuffix;
    }

    // === Handling of custom node operators  ===

    public void registerOperatorService(OperatorService service) {
        operatorService = service;
    }

    public void enableRetrieveResetParallelApproval(boolean enable) {
        retrieveResetParallelApproval = enable;
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
        if (operatorService != null) {
            return operatorService.handleParallelApproval(operatorIds, orgIds, expandOperatorIds);
        }

        var parallelApprovalMap = new HashMap<String, ApprovalStatus>();

        for (var expandOperatorId : expandOperatorIds) {
            parallelApprovalMap.put(expandOperatorId, new ApprovalStatus(expandOperatorId, false));
        }

        return parallelApprovalMap;
    }

    public Map<String, ApprovalStatus> handleRetrieveParallelApproval(Set<String> operatorIds, Set<String> orgIds, Set<String> expandOperatorIds, String operatorId) {
        if (operatorService != null) {
            return operatorService.handleRetrieveParallelApproval(operatorIds, orgIds, expandOperatorIds, retrieveResetParallelApproval, operatorId);
        }
        var parallelApprovalMap = new HashMap<String, ApprovalStatus>();

        // If the retrieval reset is enabled, then each operator of the concurrent approval needs to approve.
        if (retrieveResetParallelApproval) {
            for (var expandOperatorId : expandOperatorIds) {
                parallelApprovalMap.put(expandOperatorId, new ApprovalStatus(expandOperatorId, false));
            }
        } else {
            // Other operators are already approved by default, only the operator needs to approve
            for (var expandOperatorId : expandOperatorIds) {
                parallelApprovalMap.put(expandOperatorId, new ApprovalStatus(expandOperatorId, true));
                if (expandOperatorId.equals(operatorId)) {
                    parallelApprovalMap.put(expandOperatorId, new ApprovalStatus(operatorId, false));
                }
            }
        }

        return parallelApprovalMap;
    }

    public Map<String, ApprovalStatus> handleModificationParallelApproval(Set<String> operatorIds, Set<String> orgIds, Set<String> expandOperatorIds, Map<String, ApprovalStatus> existParallelApproval) {
        if (operatorService != null) {
            return operatorService.handleModificationParallelApproval(operatorIds, orgIds, expandOperatorIds, existParallelApproval);
        }
        var parallelApprovalMap = new HashMap<String, ApprovalStatus>();

        for (var expandOperatorId : expandOperatorIds) {
            if (existParallelApproval.containsKey(expandOperatorId)) {
                parallelApprovalMap.put(expandOperatorId, existParallelApproval.get(expandOperatorId));
            } else {
                parallelApprovalMap.put(expandOperatorId, new ApprovalStatus(expandOperatorId, false));
            }
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

    // == Configuration and registration for notification sender ===
    public void registerNotificationSender(NotificationSender sender) {
        notificationSender = sender;
    }

    public void sendNotification(Instance instance, Action action, Notification notification) {
        if (notificationSender != null) {
            notificationSender.sendNotification(instance, action, notification);
        }
    }

    // === Configuration and registration for Action Restriction ===
    public void registerActionRestriction(ActionRestriction restriction) {
        this.actionRestriction = restriction;
    }

    public Set<Action> generateCustomRemovalActionsByOperator(Definition definition, Instance instance, String operatorId) {
        var actions = new HashSet<Action>();
        if (actionRestriction != null) {
            actions.addAll(actionRestriction.generateCustomRemovalActionsByOperator(definition, instance, operatorId));
        }
        return actions;
    }
}
