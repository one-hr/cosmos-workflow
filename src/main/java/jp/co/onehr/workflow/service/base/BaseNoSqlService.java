package jp.co.onehr.workflow.service.base;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
     * key: host
     * value: schemaInitialized
     * flag to record whether the schema has been initialized
     */
    protected final ConcurrentHashMap<String, Boolean> schemaInitializedMap = new ConcurrentHashMap<>();

    public BaseNoSqlService(Class<T> classOfT) {
        this.classOfT = classOfT;
        this.defaultCollection = DEFAULT_COLLECTION;
        this.partition = addSuffixToPartition(English.plural(classOfT.getSimpleName()));
    }

    public BaseNoSqlService(Class<T> classOfT, String partition) {
        this.classOfT = classOfT;
        this.defaultCollection = DEFAULT_COLLECTION;
        this.partition = addSuffixToPartition(partition);
    }

    public BaseNoSqlService(Class<T> classOfT, String defaultCollection, String partition) {
        this.classOfT = classOfT;
        this.defaultCollection = defaultCollection;
        this.partition = addSuffixToPartition(partition);
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

        var coll = ProcessConfiguration.getConfiguration().getCollectionName(host);

        if (StringUtils.isEmpty(coll)) {
            throw new WorkflowException(WorkflowErrors.WORKFLOW_ENGINE_REGISTER_INVALID, "Failed to retrieve the name of the collection.", host);
        }

        // Ensure that each host has initialized the schema.
        if (schemaInitializedMap.putIfAbsent(host, true) == null) {
            log.info("host:{}, Database started creating table and index, table name: {}.", host, partition);

            // for partition table
            DBSchemaService.singleton.createSchemaIfNotExist(host, this.getPartition());
            // for recycle table
            DBSchemaService.singleton.createSchemaIfNotExist(host, this.getRecyclePartition());

            log.info("host:{}, Database finished creating table and index, table name: {}.", host, partition);
        }

        return coll;
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
