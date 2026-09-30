package jp.co.onehr.workflow;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.Maps;
import io.github.thunderz99.cosmos.CosmosDatabase;
import jp.co.onehr.workflow.constant.Action;
import jp.co.onehr.workflow.contract.context.ContextParamService;
import jp.co.onehr.workflow.contract.context.InstanceContext;
import jp.co.onehr.workflow.contract.log.OperateLogService;
import jp.co.onehr.workflow.contract.notification.Notification;
import jp.co.onehr.workflow.contract.notification.NotificationSender;
import jp.co.onehr.workflow.contract.operator.OperatorService;
import jp.co.onehr.workflow.contract.plugin.WorkflowPlugin;
import jp.co.onehr.workflow.contract.restriction.ActionRestriction;
import jp.co.onehr.workflow.contract.restriction.AdminActionRestriction;
import jp.co.onehr.workflow.contract.restriction.ApplicantActionPermissionProvider;
import jp.co.onehr.workflow.contract.validation.Validations;
import jp.co.onehr.workflow.dto.ActionResult;
import jp.co.onehr.workflow.dto.ApprovalStatus;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.OperateLog;
import jp.co.onehr.workflow.dto.param.ApplicantActionContext;
import jp.co.onehr.workflow.dto.param.ContextParam;
import jp.co.onehr.workflow.service.DefinitionService;
import jp.co.onehr.workflow.service.InstanceService;
import jp.co.onehr.workflow.service.WorkflowService;
import jp.co.onehr.workflow.service.base.BaseNoSqlService;
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
    private final Map<String, CosmosDatabase> dbCache = new ConcurrentHashMap<>();

    /**
     * host -> collectionName
     */
    private final Map<String, String> collectionCache = new ConcurrentHashMap<>();

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
    private AdminActionRestriction adminActionRestriction;
    private ApplicantActionPermissionProvider applicantActionPermissionProvider;

    /**
     * Custom Processing of Operation Logs
     */
    private OperateLogService operateLogService;

    /**
     *
     */
    private ContextParamService contextParamService;

    /**
     * Custom validations for the workflow
     */
    private Validations validations;

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

    /**
     * Registers the database and collection of a host.
     *
     * <p>Re-registering the same account, database name and collection keeps the current registration and its
     * schema initialization state. A different target replaces the registration and makes the next access
     * initialize the schema again.</p>
     *
     * <p>The database, the collection and the schema state are published separately, and CRUD methods read the
     * database and the collection separately. A request running during a switch may therefore use the old target,
     * a mix of the old and new database and collection, or the new target before the schema state is invalidated.
     * The last case skips the initialization and usually fails on a missing table or container; a backend that
     * creates collections on write, such as MongoDB, may write to the new target instead. Such requests do not
     * record a schema state, and later requests initialize the new target. Callers should stop in-flight writes of
     * the host while switching.</p>
     */
    public synchronized void registerDB(String host, CosmosDatabase db, String collectionName) {
        var currentDb = dbCache.get(host);
        boolean sameDatabase;
        if (currentDb == null || db == null) {
            sameDatabase = currentDb == db;
        } else {
            // Callers may create a new wrapper for the same account on every registration,
            // so the account and database name are compared instead of the wrapper object.
            sameDatabase = currentDb.getCosmosAccount() == db.getCosmosAccount()
                    && Objects.equals(currentDb.getDatabaseName(), db.getDatabaseName());
        }
        if (sameDatabase && Objects.equals(collectionCache.get(host), collectionName)) {
            return;
        }
        // compute() removes the entry when the value is null; ConcurrentHashMap does not accept null values.
        dbCache.compute(host, (key, current) -> db);
        collectionCache.compute(host, (key, current) -> collectionName);
        // Publish the new target before dropping the old state, so a later initialization uses the new target.
        // The two steps are not atomic for readers; see the Javadoc above.
        BaseNoSqlService.invalidateSchemaCaches(host);
    }

    public CosmosDatabase getDatabase(String host) {
        return host == null ? null : dbCache.get(host);
    }

    public String getCollectionName(String host) {
        return host == null ? null : collectionCache.get(host);
    }

    /**
     * Invalidates only this process's schema initialization state for the host and keeps its registration.
     * Performs no database or network I/O and does not wait for an initialization in progress; such an
     * initialization records its result only in the discarded state. Call after a same-name database rebuild;
     * a database switch must also call {@link #registerDB(String, CosmosDatabase, String)}.
     */
    public void invalidateSchemaCache(String host) {
        if (host != null) {
            BaseNoSqlService.invalidateSchemaCaches(host);
        }
    }

    /**
     * Synchronously checks all built-in workflow partitions, bypassing successful schema caches.
     * Configure the partition suffix before the services are first used.
     *
     * @param host registered tenant identifier
     * @return the six logical partition names, including existing partitions; on Cosmos DB
     *         these describe the no-op schema path, not newly created physical containers
     * @throws Exception if registration or any table, index or TTL initialization fails;
     *                   no partial result is returned and later calls may retry. On PostgreSQL a table
     *                   that is still missing after creation, e.g. because another object uses its name,
     *                   also fails here. TTL cron jobs are scheduled when absent but their current state
     *                   is not verified.
     */
    public List<String> ensureTables(String host) throws Exception {
        var tables = new ArrayList<String>();
        tables.addAll(WorkflowService.singleton.ensureTables(host));
        tables.addAll(DefinitionService.singleton.ensureTables(host));
        tables.addAll(InstanceService.singleton.ensureTables(host));
        return List.copyOf(tables);
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

    public Set<String> handleExpandOperators(Set<String> operatorIds, InstanceContext instanceContext) {
        if (operatorService != null) {
            return operatorService.handleOperators(operatorIds, instanceContext);
        }
        return operatorIds;
    }

    public Set<String> handleExpandOrganizations(Set<String> orgIds, InstanceContext instanceContext) {
        if (operatorService != null) {
            return operatorService.handleOrganizations(orgIds, instanceContext);
        }
        return orgIds;
    }

    public Map<String, ApprovalStatus> handleParallelApproval(Set<String> operatorIds, Set<String> orgIds, Set<String> expandOperatorIds, InstanceContext instanceContext) {
        if (operatorService != null) {
            return operatorService.handleParallelApproval(operatorIds, orgIds, expandOperatorIds, instanceContext);
        }

        var parallelApprovalMap = new HashMap<String, ApprovalStatus>();

        for (var expandOperatorId : expandOperatorIds) {
            parallelApprovalMap.put(expandOperatorId, new ApprovalStatus(expandOperatorId, false));
        }

        return parallelApprovalMap;
    }

    public Map<String, ApprovalStatus> handleRetrieveParallelApproval(Set<String> operatorIds, Set<String> orgIds, Set<String> expandOperatorIds, String operatorId, InstanceContext instanceContext) {
        if (operatorService != null) {
            return operatorService.handleRetrieveParallelApproval(operatorIds, orgIds, expandOperatorIds, retrieveResetParallelApproval, operatorId, instanceContext);
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

    public Map<String, ApprovalStatus> handleModificationParallelApproval(Set<String> operatorIds, Set<String> orgIds, Set<String> expandOperatorIds, Map<String, ApprovalStatus> existParallelApproval, InstanceContext instanceContext) {
        if (operatorService != null) {
            return operatorService.handleModificationParallelApproval(operatorIds, orgIds, expandOperatorIds, existParallelApproval, instanceContext);
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

    public void registerAdminActionRestriction(AdminActionRestriction restriction) {
        this.adminActionRestriction = restriction;
    }

    public void registerApplicantActionPermissionProvider(ApplicantActionPermissionProvider provider) {
        this.applicantActionPermissionProvider = provider;
    }

    public Set<Action> generateCustomRemovalActionsByOperator(Definition definition, Instance instance, String operatorId) {
        var actions = new HashSet<Action>();
        if (actionRestriction != null) {
            actions.addAll(actionRestriction.generateCustomRemovalActionsByOperator(definition, instance, operatorId));
        }
        return actions;
    }

    public Set<Action> generateCustomRemovalActionsByAdmin(Definition definition, Instance instance, String operatorId) {
        var actions = new HashSet<Action>();
        if (adminActionRestriction != null) {
            actions.addAll(adminActionRestriction.generateCustomRemovalActionsByAdmin(definition, instance, operatorId));
        }
        return actions;
    }

    /**
     * Delegates applicant-side action checks to the registered business provider.
     *
     * @param definition workflow definition for the instance
     * @param instance workflow instance being checked
     * @param operatorId operator requesting the action
     * @param action applicant-side action to check
     * @param context extra values used by the business permission check
     * @return true when a provider is registered and allows the action
     */
    public boolean canPerformApplicantAction(Definition definition, Instance instance, String operatorId, Action action, ApplicantActionContext context) {
        if (applicantActionPermissionProvider != null) {
            return applicantActionPermissionProvider.canPerformApplicantAction(definition, instance, operatorId, action, context);
        }
        return false;
    }

    // === Configuration and registration for Operator Log  ===
    public void registerOperatorLogService(OperateLogService operateLogService) {
        this.operateLogService = operateLogService;
        operateLogService.removeActionsWithNotLogged();
    }

    public void handlingActionResultLog(OperateLog log, ActionResult actionResult) {
        if (operateLogService != null) {
            operateLogService.handleActionResult(log, actionResult);
        }
    }

    // === Configuration and registration for Context Param  ===
    public void registerContextParamService(ContextParamService contextParamService) {
        this.contextParamService = contextParamService;
    }

    public void generateContextParam4Bulk(ContextParam contextParam, Definition definition, Instance instance, String operatorId) {
        if (contextParamService != null) {
            contextParamService.generateContextParam4Bulk(contextParam, definition, instance, operatorId);
        }
    }

    // === Configuration and registration for Validations  ===
    public void registerValidationsService(Validations validations) {
        this.validations = validations;
    }

    public void definitionValidation(Definition definition) throws Exception {
        if (validations != null) {
            validations.definitionValidation(definition);
        }
    }
}
