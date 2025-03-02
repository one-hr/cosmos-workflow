package jp.co.onehr.workflow.dao;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import io.github.thunderz99.cosmos.impl.mongo.MongoImpl;
import io.github.thunderz99.cosmos.util.Checker;
import jp.co.onehr.workflow.ProcessConfiguration;
import jp.co.onehr.workflow.dto.base.BaseData;
import jp.co.onehr.workflow.util.InfraUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.atteo.evo.inflector.English;
import org.bson.Document;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static jp.co.onehr.workflow.service.base.BaseNoSqlService.addSuffixToPartition;
import static jp.co.onehr.workflow.service.base.BaseNoSqlService.judgeEnableDefaultWorkflowDB;

/**
 * DAO class used to initialize Database's indexes(only used in mongodb)
 *
 * <p>
 *     every partition(mongo collection) should create index
 * </p>
 * <p>
 *     use DBSchemaService instead
 * </p>
 */
@Deprecated
public class IndexDAO {

    static Logger log = LoggerFactory.getLogger(IndexDAO.class);

    public static IndexDAO singleton = new IndexDAO();

    IndexDAO(){
    }

    /**
     * check and create indexes needed for mongodb
     * <p>
     *     do not have effects when using cosmos
     * </p>
     * @param host
     */
    public void checkAndInit(String host) {

        if(!InfraUtil.isMongoDB()){
            // only works for mongodb
            return;
        }

        var classNames = getSubTypeClassNames(BaseData.class, "jp.co.onehr.workflow.dto");
        try {
            createIfNotExistBasicIndexes(host, classNames);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * create basic indexes("id", "_ts") for dto classes
     * @param host
     * @param classNames
     * @return list of indexes created (format: "partition.fieldName")
     */
    public List<String> createIfNotExistBasicIndexes(String host, List<String> classNames) throws Exception {

        List<String> ret = new ArrayList<>();

        if(!InfraUtil.isMongoDB()){
            // only works for mongodb
            return ret;
        }

        if(CollectionUtils.isEmpty(classNames)){
            return ret;
        }

        var partitionNames = classNames.stream().map(name ->
            addSuffixToPartition(English.plural(name)))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for(var partition : partitionNames){
            ret.add(createIndexIfNotExist(host, partition, "id", true));
            ret.add(createIndexIfNotExist(host, partition, "_ts", false));
        }

        return ret.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * Create needed unique indexes for specific partition for mongodb
     * @param partition
     * @return index if created. otherwise, null
     */
    public String createIndexIfNotExist(String host, String partition, String fieldName, boolean unique) throws Exception {

        if(!InfraUtil.isMongoDB()){
            // only works for mongodb
            return null;
        }

        if(indexExist(host, partition, fieldName)){
            return null;
        }

        // Define the index key. 1 for ascending order
        var index = new Document(fieldName, 1);

        var database = getMongoDatabase(host);

        // Create the unique indexes needed by core
        database.getCollection(partition).createIndex(index, new IndexOptions().unique(unique));
        log.info("index created: partition:{}, field:{}, index:{}", partition, fieldName, index.toJson());

        return partition + "." + fieldName;

    }

    /**
     * Check indexes whether exist for specific partition and fieldName
     *
     * @param host
     * @param partition
     * @param fieldName
     * @return true/false
     * @throws Exception
     */
    public boolean indexExist(String host, String partition, String fieldName) throws Exception {
        if(!InfraUtil.isMongoDB()){
            // only works for mongodb
            return false;
        }

        Checker.checkNotEmpty(host, "host");
        Checker.checkNotEmpty(partition, "partition");
        Checker.checkNotEmpty(fieldName, "fieldName");

        var indexName = fieldName;
        if(!StringUtils.endsWith(indexName, "_1")){
            // naming rule for mongo index. name -> name_1, id -> id_1
            indexName = indexName + "_1";
        }

        var database = getMongoDatabase(host);

        var iter = database.getCollection(partition).listIndexes().iterator();

        while(iter.hasNext()){
            var index = iter.next();
            if(StringUtils.equals(indexName, index.get("name").toString())){
                // already exist
                log.debug("index exists: partition:{}, field:{}, index:{}", partition, fieldName, index.toJson());
                return true;
            }
        }

        return false;

    }


    /**
     * delete indexes for specific partition
     *
     * @param host
     * @param partition
     * @param fieldName
     * @throws Exception
     */
    public void deleteIndex(String host, String partition, String fieldName) throws Exception {
        if(!InfraUtil.isMongoDB()){
            // only works for mongodb
            return;
        }

        Checker.checkNotEmpty(host, "host");
        Checker.checkNotEmpty(partition, "partition");
        Checker.checkNotEmpty(fieldName, "fieldName");

        var indexName = fieldName;
        if(!StringUtils.endsWith(indexName, "_1")){
            // naming rule for mongo index. name -> name_1, id -> id_1
            indexName = indexName + "_1";
        }

        var database = getMongoDatabase(host);

        database.getCollection(partition).dropIndex(indexName);

    }


    /**
     * Get the native mongo database instance to do special operations for mongo
     *
     * @param host
     * @return native mongo database instance
     * @throws Exception
     */
    static MongoDatabase getMongoDatabase(String host) throws Exception {
        var db = ProcessConfiguration.getConfiguration().getDatabase(host);

        if (ObjectUtils.isEmpty(db) && judgeEnableDefaultWorkflowDB()) {
            db = CosmosDB.registerDefaultWorkflowDB(host);
        }

        var collectionName = ProcessConfiguration.getConfiguration().getCollectionName(host);

        // get the native mongo client to do index operations
        var client = ((MongoImpl)db.getCosmosAccount()).getClient();
        var database = client.getDatabase(collectionName);
        return database;
    }


    /**
     * Get all children classes by parent class and package name
     * @param parent
     * @param packageName
     * @return a set of subTypes classes
     */
    List<String> getSubTypeClassNames(Class parent, String packageName) {

        // Specify the package to scan
        var reflections = new Reflections(packageName);

        // Specify the interface for which you want to find all implementations
        Set<Class<?>> childClasses = reflections.getSubTypesOf(parent);

        return childClasses.stream().map(clazz -> clazz.getSimpleName()).collect(Collectors.toList());

    }
}
