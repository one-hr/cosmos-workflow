package jp.co.onehr.workflow;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.github.thunderz99.cosmos.CosmosDatabase;
import jp.co.onehr.workflow.constant.Action;
import jp.co.onehr.workflow.constant.NodeType;
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
public class ProcessEngineConfiguration {

    public final static Logger log = LoggerFactory.getLogger(ProcessEngineConfiguration.class);

    private static final ProcessEngineConfiguration singleton = new ProcessEngineConfiguration();

    /**
     * host -> database
     */
    private Map<String, CosmosDatabase> dbCache = Maps.newHashMap();

    /**
     * host -> collectionName
     */
    private Map<String, String> collectionCache = Maps.newHashMap();

    private OperatorService operatorService;

    /**
     * Customized plugins available for the workflow.
     */
    private Map<String, WorkflowPlugin> pluginCache = Maps.newHashMap();

    private Set<String> skipNodeTypes = Sets.newHashSet(NodeType.RobotNode.name());

    /**
     * Customized sending message notification functionality.
     */
    private NotificationSender notificationSender;

    /**
     * Customized instance action restrictions.
     */
    private ActionRestriction actionRestriction;

    private ProcessEngineConfiguration() {

    }

    public static ProcessEngineConfiguration getConfiguration() {
        return singleton;
    }

    /**
     * build workflow engine
     *
     * @return
     */
    public ProcessEngine buildEngine() {
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
        if (operatorService != null) {
            return operatorService.handleParallelApproval(operatorIds, orgIds, expandOperatorIds);
        }

        var parallelApprovalMap = new HashMap<String, ApprovalStatus>();

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
