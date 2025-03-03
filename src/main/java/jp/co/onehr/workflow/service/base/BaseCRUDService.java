package jp.co.onehr.workflow.service.base;

import java.util.*;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import io.github.thunderz99.cosmos.Cosmos;
import io.github.thunderz99.cosmos.CosmosDocument;
import io.github.thunderz99.cosmos.CosmosException;
import io.github.thunderz99.cosmos.condition.Condition;
import io.github.thunderz99.cosmos.util.JsonUtil;
import jp.co.onehr.workflow.constant.DatabaseErrors;
import jp.co.onehr.workflow.dto.base.BaseData;
import jp.co.onehr.workflow.dto.base.DeletedObject;
import jp.co.onehr.workflow.dto.base.UniqueKeyCapable;
import jp.co.onehr.workflow.exception.WorkflowException;
import jp.co.onehr.workflow.util.DateUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;


public abstract class BaseCRUDService<T extends BaseData> extends BaseNoSqlService<T> {

    public static final List<String> DEFAULT_SORT = List.of("_ts", "DESC");

    public static final int DEFAULT_RECYCLE_LIFETIME = 90 * 24 * 60 * 60;

    public static final String ORIGINAL_DATA = "originalData";
    public static final String CREATED_AT = "createdAt";
    public static final String DELETED_AT = "deletedAt";

    /**
     * Combining unique constraints and related keys to generate a clear error message during output
     * id, partition_key, _uniqueKey1, _uniqueKey2, _uniqueKey3
     */
    public static final Set<String> CONSTRAINT_KEYS = Sets.newLinkedHashSet();

    static {
        CONSTRAINT_KEYS.addAll(UniqueKeyCapable.uniqueKeys);
        CONSTRAINT_KEYS.addAll(Set.of("id", Cosmos.getDefaultPartitionKey()));
    }

    public BaseCRUDService(Class<T> classOfT) {
        super(classOfT);
    }

    public BaseCRUDService(Class<T> classOfT, String partition) {
        super(classOfT, partition);
    }

    public BaseCRUDService(Class<T> classOfT, String defaultCollection, String partition) {
        super(classOfT, defaultCollection, partition);
    }

    /**
     * Before the Mutation (Update/Upsert/Create), an additional process is performed<br/>
     * The default is to do nothing
     *
     * <p>
     * It can be achieved by subclass overriding
     * </p>
     *
     * @param map
     * @return
     */
    public Map<String, Object> beforeMutation(T data, Map<String, Object> map) throws Exception {
        // add uniqueKeys for data
        map = processUniqueKeys(data, map);
        return map;
    }

    private Map<String, Object> stripId(Map<String, Object> map) {
        // Trim whitespace such as \s, \t, \n around the ID.
        map.put("id", StringUtils.strip(map.getOrDefault("id", "").toString()));
        return map;
    }

    public List<String> getDefaultSort() {
        return DEFAULT_SORT;
    }

    protected T create(String host, T data) throws Exception {

        data.createdAt = DateUtil.nowDateTimeStringUTC();
        data.updatedAt = data.createdAt;

        var map = JsonUtil.toMap(data);
        map = beforeMutation(data, map);
        return createMap(host, map);
    }

    protected T createMap(String host, Map<String, Object> map) throws Exception {

        var db = getDatabase(host);

        map = stripId(map);

        T newData = null;
        try {
            CosmosDocument cd = db.create(getColl(host), map, getPartition());
            try {
                newData = cd.toObject(classOfT);
            } catch (Exception e) {
                throw new WorkflowException(DatabaseErrors.JSON_FROM_DB_PROCESS_ERROR);
            }
        } catch (CosmosException e) {
            throw wrapConflictError(e, map);
        }
        return newData;
    }

    /**
     * Read from the database.
     * If it doesn't exist, throw a 404 Exception
     *
     * @param host
     * @param id
     * @return
     * @throws Exception
     */
    protected T read(String host, String id) throws Exception {
        var db = getDatabase(host);
        CosmosDocument cd = db.read(getColl(host), id, getPartition());
        try {
            return cd.toObject(classOfT);
        } catch (Exception e) {
            throw new WorkflowException(DatabaseErrors.JSON_FROM_DB_PROCESS_ERROR);
        }
    }

    /**
     * Read from the database.
     * If it doesn't exist, return null
     *
     * @param host
     * @param id
     * @return
     * @throws Exception
     */
    protected T readSuppressing404(String host, String id) throws Exception {
        return readSuppressing404(host, id, getPartition());
    }

    protected T readSuppressing404(String host, String id, String partition) throws Exception {
        if (StringUtils.isEmpty(id)) {
            return null;
        }
        var db = getDatabase(host);
        var ret = db.readSuppressing404(getColl(host), id, partition);

        if (ret == null) {
            return null;
        } else {
            try {
                return ret.toObject(classOfT);
            } catch (Exception e) {
                throw new WorkflowException(DatabaseErrors.JSON_FROM_DB_PROCESS_ERROR);
            }
        }
    }

    protected T update(String host, T data) throws Exception {

        T existData = null;
        if (StringUtils.isNotEmpty(data.getId())) {
            existData = this.read(host, data.getId());
        }

        data.createdAt = StringUtils.isNotEmpty(existData.createdAt) ? existData.createdAt
                : DateUtil.nowDateTimeStringUTC();
        data.updatedAt = DateUtil.nowDateTimeStringUTC();

        var map = JsonUtil.toMap(data);
        map = beforeMutation(data, map);
        return updateMap(host, map);
    }

    protected T updateMap(String host, Map<String, Object> map) throws Exception {
        map = stripId(map);
        var db = getDatabase(host);
        T data;
        try {
            CosmosDocument cd = db.update(getColl(host), map, getPartition());
            try {
                data = cd.toObject(classOfT);
            } catch (Exception e) {
                throw new WorkflowException(DatabaseErrors.JSON_FROM_DB_PROCESS_ERROR);
            }
        } catch (CosmosException e) {
            throw wrapConflictError(e, map);
        }
        return data;
    }

    protected T upsert(String host, T data) throws Exception {

        T existData = null;
        if (StringUtils.isNotEmpty(data.getId())) {
            existData = this.readSuppressing404(host, data.getId());
        } else {
            data.setId(generateId(data));
        }

        if (existData == null) {
            data.createdAt = DateUtil.nowDateTimeStringUTC();
        } else {
            data.createdAt = StringUtils.isNotEmpty(existData.createdAt) ? existData.createdAt
                    : DateUtil.nowDateTimeStringUTC();
        }

        data.updatedAt = DateUtil.nowDateTimeStringUTC();

        var map = JsonUtil.toMap(data);
        map = beforeMutation(data, map);
        return upsertMap(host, map);
    }

    protected T upsertMap(String host, Map<String, Object> map) throws Exception {
        var db = getDatabase(host);
        map = stripId(map);
        T data;
        try {
            CosmosDocument cd = db.upsert(getColl(host), map, getPartition());
            try {
                data = cd.toObject(classOfT);
            } catch (Exception e) {
                throw new WorkflowException(DatabaseErrors.JSON_FROM_DB_PROCESS_ERROR);
            }

        } catch (CosmosException e) {
            throw wrapConflictError(e, map);
        }
        return data;
    }

    /**
     * Physical deletion
     */
    protected DeletedObject purge(String host, String id) throws Exception {
        var db = getDatabase(host);
        db.delete(getColl(host), StringUtils.strip(id), getPartition());
        return new DeletedObject(id);
    }

    /**
     * soft deletion
     */
    protected DeletedObject delete(String host, String id) throws Exception {
        move2Recycle(host, id);

        return new DeletedObject(id);
    }

    /**
     * move origin data to recycle bin
     *
     * @param host
     * @param id
     * @return origin data
     * @throws Exception
     */
    private T move2Recycle(String host, String id) throws Exception {
        // origin data
        var oldMap = this.readRaw(host, id, false);

        if (oldMap == null) {
            return null;
        }
        var recycle = new HashMap<String, Object>();
        recycle.put("ttl", getTtlLifeTime());
        recycle.put(ORIGINAL_DATA, oldMap);
        recycle.put("id", id);
        recycle.put(CREATED_AT, DateUtil.nowDateTimeStringUTC());
        recycle.put(UniqueKeyCapable.UNIQUE_KEY_1, id);
        recycle.put(UniqueKeyCapable.UNIQUE_KEY_2, id);
        recycle.put(UniqueKeyCapable.UNIQUE_KEY_3, id);

        recycle.put(DELETED_AT, DateUtil.nowDateTimeStringUTC());

        upsertRaw(host, recycle, true);
        // hard delete
        purge(host, StringUtils.strip(id));

        return JsonUtil.fromMap(oldMap, this.classOfT);
    }

    /**
     * upsert original data or copy in recycle bin
     *
     * @param host
     * @param map
     * @param isRecycle
     * @return
     * @throws Exception
     */
    protected final Map<String, Object> upsertRaw(String host, Map<String, Object> map, boolean isRecycle) throws Exception {
        var partition = isRecycle ? getRecyclePartition() : getPartition();
        var db = getDatabase(host);
        try {
            return db.upsert(getColl(host), map, partition).toMap();
        } catch (CosmosException e) {
            // id conflict will be error
            throw wrapConflictError(e, map);
        }
    }

    private int getTtlLifeTime() {
        // デフォルト保存期間 90日
        return DEFAULT_RECYCLE_LIFETIME;
    }

    protected final Map<String, Object> readRaw(String host, String id, boolean isRecycle) throws Exception {
        var partition = isRecycle ? getRecyclePartition() : getPartition();

        if (StringUtils.isEmpty(id)) {
            return null;
        }
        var db = getDatabase(host);
        var ret = db.readSuppressing404(getColl(host), id, partition);

        return ret == null ? null : ret.toMap();
    }

    protected List<T> find(String host, Condition cond) throws Exception {
        var db = getDatabase(host);
        if (CollectionUtils.isEmpty(cond.sort)) {
            cond.sort(getDefaultSort().toArray(new String[]{}));
        }

        return db.find(getColl(host), cond, getPartition()).toList(classOfT);
    }

    protected String generateId(T data) {
        return StringUtils.isEmpty(data.id) ? UUID.randomUUID().toString() : StringUtils.strip(data.id);
    }

    /**
     * When encountering an ID/uniqueKey conflict error, wrap the errorObj in a more informative manner
     *
     * @param e
     * @param map
     * @return
     * @throws CosmosException
     */
    static Exception wrapConflictError(CosmosException e, Map<String, Object> map) throws CosmosException {
        //When encountering a CONFLICT error, return a more informative errorObj
        if (e.getStatusCode() == 409) {
            // Minimum map containing id, partition_key, _uniqueKey1, _uniqueKey2, _uniqueKey3
            var errorMap = map.entrySet().stream().filter(entry ->
                    CONSTRAINT_KEYS.contains(entry.getKey())).collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));
            var errorToThrow = judgeConflictErrorToThrow(map);
            return new WorkflowException(errorToThrow, e, JsonUtil.toJson(errorMap));
        }
        return e;
    }

    /**
     * Determine whether there is a conflict in the id or _uniqueKey1/2/3 and return the appropriate Error to throw.
     *
     * <p>
     * If the values of id and uniqueKey1/2/3 are all the same, it can always be determined as ID_CONFLICT <br/>
     * If they are different, it is not possible to determine whether it is an id conflict or a uniqueKey conflict,
     * so return UNIQUE_KEY_CONFLICT
     * </p>
     *
     * @param map
     * @return
     */
    static DatabaseErrors judgeConflictErrorToThrow(Map<String, Object> map) {
        // To determine if there are conflicts with the ID or _uniqueKey1/2/3
        var uniqueValuesSet = map.entrySet().stream().filter(entry -> UniqueKeyCapable.uniqueKeysWithId.contains(entry.getKey())).map(entry -> entry.getValue()).collect(Collectors.toSet());

        // If the values of id, _uniqueKey1, _uniqueKey2, and _uniqueKey3 are the same, it is considered an ID_CONFLICT.
        // If they are different, it is considered a UNIQUE_KEY_CONFLICT.
        return uniqueValuesSet.size() == 1 ? DatabaseErrors.ID_CONFLICT : DatabaseErrors.UNIQUE_KEY_CONFLICT;
    }
}
