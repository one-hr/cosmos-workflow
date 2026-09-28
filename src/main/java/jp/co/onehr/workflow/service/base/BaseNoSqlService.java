package jp.co.onehr.workflow.service.base;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.thunderz99.cosmos.CosmosDatabase;
import jp.co.onehr.workflow.ProcessConfiguration;
import jp.co.onehr.workflow.constant.WorkflowErrors;
import jp.co.onehr.workflow.dao.CosmosDB;
import jp.co.onehr.workflow.dto.base.UniqueKeyCapable;
import jp.co.onehr.workflow.exception.WorkflowException;
import jp.co.onehr.workflow.service.infra.DBSchemaService;
import jp.co.onehr.workflow.util.EnvUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.atteo.evo.inflector.English;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseNoSqlService<T> {

    public static final String ENABLE_WORKFLOW_DEFAULT_DB = "ENABLE_WORKFLOW_DEFAULT_DB";

    public static final String FW_WORKFLOW_COLLECTION_NAME = "FW_WORKFLOW_COLLECTION_NAME";

    public static final String DEFAULT_COLLECTION = "Data";

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    protected Class<T> classOfT;

    protected String defaultCollection;

    protected String partition;

    /**
     * Live services whose schema state {@link #invalidateSchemaCaches(String)} drops.
     * Weak references let services that are no longer used be collected.
     */
    private static final Set<BaseNoSqlService<?>> instances = Collections.newSetFromMap(new WeakHashMap<>());

    /**
     * host -> whether the schema has been initialized. Each value is also the lock of its host.
     * Invalidation removes only the entry of its host, so an initialization that holds the removed state records
     * its result only there, and the states and locks of other hosts are kept.
     */
    private final ConcurrentHashMap<String, AtomicBoolean> schemaStates = new ConcurrentHashMap<>();

    public BaseNoSqlService(Class<T> classOfT) {
        this.classOfT = classOfT;
        this.defaultCollection = DEFAULT_COLLECTION;
        this.partition = addSuffixToPartition(English.plural(classOfT.getSimpleName()));
        registerInstance();
    }

    public BaseNoSqlService(Class<T> classOfT, String partition) {
        this.classOfT = classOfT;
        this.defaultCollection = DEFAULT_COLLECTION;
        this.partition = addSuffixToPartition(partition);
        registerInstance();
    }

    public BaseNoSqlService(Class<T> classOfT, String defaultCollection, String partition) {
        this.classOfT = classOfT;
        this.defaultCollection = defaultCollection;
        this.partition = addSuffixToPartition(partition);
        registerInstance();
    }

    /**
     * Processing of unique keys other than the ID
     *
     * @param data
     * @param map
     * @return
     */
    public Map<String, Object> processUniqueKeys(T data, Map<String, Object> map) {
        if (data instanceof UniqueKeyCapable capable) {
            // if supporting UniqueKeyCapable, return custom fields.
            map.put(UniqueKeyCapable.UNIQUE_KEY_1, capable.getUniqueKey1());
            map.put(UniqueKeyCapable.UNIQUE_KEY_2, capable.getUniqueKey2());
            map.put(UniqueKeyCapable.UNIQUE_KEY_3, capable.getUniqueKey3());
        } else {
            // in normal cases, add the id field
            var id = map.getOrDefault("id", "").toString();
            if (StringUtils.isEmpty(id)) {
                id = UUID.randomUUID().toString();
                map.put("id", id);
            }
            map.put(UniqueKeyCapable.UNIQUE_KEY_1, id);
            map.put(UniqueKeyCapable.UNIQUE_KEY_2, id);
            map.put(UniqueKeyCapable.UNIQUE_KEY_3, id);
        }
        return map;
    }

    private void registerInstance() {
        synchronized (instances) {
            instances.add(this);
        }
    }

    /**
     * Drops this process's schema state of the host in every live service. Performs no I/O and does not
     * wait for an initialization in progress. Use {@link ProcessConfiguration#invalidateSchemaCache(String)}.
     *
     * @param host tenant identifier
     */
    public static void invalidateSchemaCaches(String host) {
        synchronized (instances) {
            for (var service : instances) {
                service.schemaStates.remove(host);
            }
        }
    }

    public String getPartition() {
        return partition;
    }

    public CosmosDatabase getDatabase(String host) throws Exception {
        var db = ProcessConfiguration.getConfiguration().getDatabase(host);
        if (ObjectUtils.isEmpty(db) && judgeEnableDefaultWorkflowDB()) {
            db = CosmosDB.registerDefaultWorkflowDB(host);
        }
        return db;
    }

    /**
     * Returning the name of the collection to be used
     *
     * @param host
     * @return
     */
    public String getColl(String host) throws Exception {
        // The collection, the database read by the caller and the schema state are separate reads, not one snapshot.
        // During a switch, a request may combine the old and new targets, or read the new target before its state
        // is invalidated and skip the initialization. See ProcessConfiguration#registerDB.
        var coll = ProcessConfiguration.getConfiguration().getCollectionName(host);

        if (StringUtils.isEmpty(coll)) {
            throw new WorkflowException(WorkflowErrors.WORKFLOW_ENGINE_REGISTER_INVALID, "Failed to retrieve the name of the collection.", host);
        }

        initializeSchema(host, false);
        return coll;
    }

    /**
     * Forces synchronous schema verification for this service, bypassing the successful schema cache.
     * Intended for {@link ProcessConfiguration#ensureTables(String)}.
     *
     * @return the main and recycle logical partition names, including existing objects
     * @throws Exception if the registration is incomplete or initialization fails; later calls may retry
     */
    public final List<String> ensureTables(String host) throws Exception {
        // Resolve the database first; this registers the default database when enabled, as a CRUD access does.
        var db = getDatabase(host);
        var coll = ProcessConfiguration.getConfiguration().getCollectionName(host);

        if (ObjectUtils.isEmpty(db) || StringUtils.isEmpty(coll)) {
            throw new WorkflowException(WorkflowErrors.WORKFLOW_ENGINE_REGISTER_INVALID, "Failed to retrieve the registered database and collection.", host);
        }

        initializeSchema(host, true);
        return List.of(getPartition(), getRecyclePartition());
    }

    /**
     * Initializes the main table, recycle table and custom indexes once per host until it is invalidated.
     * Concurrent first calls for the same host wait for the running initialization. A forced call always
     * reruns the checks.
     */
    private void initializeSchema(String host, boolean force) throws Exception {
        // The initialization reads the registration after taking this state, and registerDB publishes a new target
        // before removing the state, so a state created after an invalidation never records the old target.
        var initialized = schemaStates.computeIfAbsent(host, key -> new AtomicBoolean());
        if (!force && initialized.get()) {
            return;
        }
        // The per-host flag doubles as the lock, so only calls for the same host wait for each other.
        synchronized (initialized) {
            if (!force && initialized.get()) {
                return;
            }
            initialized.set(false);
            log.info("host:{}, Starting table and index initialization, partition:{}.", host, partition);
            try {
                DBSchemaService.singleton.createSchemaIfNotExist(host, getPartition());
                DBSchemaService.singleton.createSchemaIfNotExist(host, getRecyclePartition());
                DBSchemaService.singleton.createCustomIndexIfNotExist(host, getPartition(), classOfT);
                initialized.set(true);
                log.info("host:{}, Finished table and index initialization, partition:{}.", host, partition);
            } catch (Exception e) {
                // The exception is rethrown, so only its type and message are logged here to keep repeated retries readable.
                log.warn("host:{}, Schema initialization failed; a later call may retry, partition:{}, cause:{}: {}",
                        host, partition, e.getClass().getSimpleName(), e.getMessage());
                throw e;
            }
        }
    }

    /**
     * Whether to enable the default database for the workflow
     *
     * @return
     */
    public static boolean judgeEnableDefaultWorkflowDB() {
        var enable = EnvUtil.getBooleanOrDefault(ENABLE_WORKFLOW_DEFAULT_DB, false);
        return enable;
    }

    /**
     * Returning the CollectionName to be used based on environment variables, and defaultCollection
     *
     * @param defaultCollection
     * @return
     */
    public static String getCollectionNameByEnv(String defaultCollection) {
        var collectionName = EnvUtil.getOrDefault(FW_WORKFLOW_COLLECTION_NAME, "");
        return StringUtils.isEmpty(collectionName) ? defaultCollection : collectionName;
    }

    /**
     * Add a custom suffix to the partition.
     *
     * @param partition
     * @return
     */
    public static String addSuffixToPartition(String partition) {
        var suffix = ProcessConfiguration.getConfiguration().getPartitionSuffix();
        if (StringUtils.isNotEmpty(suffix)) {
            partition = partition + "_" + suffix;
        }
        return partition;
    }

    protected final String getRecyclePartition() {
        return getPartition() + "_recycle";
    }

}
