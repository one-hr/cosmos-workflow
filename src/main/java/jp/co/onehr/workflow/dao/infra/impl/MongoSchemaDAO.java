package jp.co.onehr.workflow.dao.infra.impl;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import io.github.thunderz99.cosmos.impl.mongo.MongoImpl;
import io.github.thunderz99.cosmos.util.Checker;
import jp.co.onehr.workflow.ProcessConfiguration;
import jp.co.onehr.workflow.dao.CosmosDB;
import jp.co.onehr.workflow.dao.infra.DBSchemaInitializer;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.atteo.evo.inflector.English;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static jp.co.onehr.workflow.service.base.BaseNoSqlService.judgeEnableDefaultWorkflowDB;

/**
 * DAO class used to initialize Database's indexes(only used in mongodb)
 *
 * <p>
 *     every partition(mongo collection) should create index
 * </p>
 */
public class MongoSchemaDAO implements DBSchemaInitializer {

    static Logger log = LoggerFactory.getLogger(MongoSchemaDAO.class);

    public static MongoSchemaDAO singleton = new MongoSchemaDAO();

    public MongoSchemaDAO() {
    }


    @Override
    public List<String> createIndexesIfNotExist(String host, String partitionName) throws Exception {
        var ret = new ArrayList<String>();

        // create basic indexes
        {
            var options = List.of(
                    Pair.of("id", new Options().unique(true)),
                    Pair.of("_ts", new Options().unique(false)),
                    Pair.of("_expireAt", new Options().expireAfter(0L))
            );
            ret.addAll(_createIndexIfNotExistByOptions(host, partitionName, options));
        }
        return ret;
    }



    /**
     * create specific indexes for dto classes
     *
     * @param host
     * @param partitionName
     * @param options
     * @return list of partition.fieldName  that created Indexes
     */
    List<String> _createIndexIfNotExistByOptions(String host, String partitionName, List<Pair<String, Options>> options) throws Exception {

        if (StringUtils.isEmpty(partitionName)) {
            return List.of();
        }

        var ret = new ArrayList<String>();

        for (var option : options) {
            ret.add(_createIndexIfNotExistByFieldName(host, partitionName, option.getLeft(), option.getRight()));
        }

        return ret.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * Create needed unique indexes for specific partition for mongodb
     *
     * @param host
     * @param partition
     * @param fieldName
     * @param options
     * @return "partition.fieldName" if index created. Otherwise, null
     * @throws Exception
     */
    String _createIndexIfNotExistByFieldName(String host, String partition, String fieldName, Options options) throws Exception {

        if (indexExist(host, partition, fieldName)) {
            return null;
        }

        // Define the index key. 1 for ascending order
        var index = new Document(fieldName, 1);

        var database = getMongoDatabase(host);

        // Create the unique indexes needed by core

        var indexOptions = new IndexOptions().unique(options.unique);

        if (options.expireAfter != null) {
            indexOptions.expireAfter(options.expireAfter, TimeUnit.SECONDS);
        }

        database.getCollection(partition).createIndex(index, indexOptions);
        log.info("index created: partition:{}, field:{}, index:{}", partition, fieldName, index.toJson());

        return partition + "." + fieldName;

    }

    /**
     * Check indexes whether exist for specific partition
     *
     * @param host
     * @param partition
     * @return true/false
     * @throws Exception
     */
    public boolean indexExist(String host, String partition, String fieldName) throws Exception {

        Checker.checkNotEmpty(host, "host");
        Checker.checkNotEmpty(partition, "partition");
        Checker.checkNotEmpty(fieldName, "fieldName");

        var indexName = fieldName;
        if (!StringUtils.endsWith(indexName, "_1")) {
            // naming rule for mongo index. name -> name_1, id -> id_1
            indexName = indexName + "_1";
        }

        var database = getMongoDatabase(host);

        var iter = database.getCollection(partition).listIndexes().iterator();

        while (iter.hasNext()) {
            var index = iter.next();
            log.debug("index exists: partition:{}, field:{}, index:{}", partition, fieldName, index.toJson());

            if (StringUtils.equals(indexName, index.get("name").toString())) {
                // already exist
                return true;
            }
        }

        return false;
    }

    public void deleteIndex(String host, String partition, String fieldName) throws Exception {
        Checker.checkNotEmpty(host, "host");
        Checker.checkNotEmpty(partition, "partition");
        Checker.checkNotEmpty(fieldName, "fieldName");

        var indexName = fieldName;
        if (!StringUtils.endsWith(indexName, "_1")) {
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
     * Index options when creating indexes for mongodb
     */
    public static class Options {
        public boolean unique = false;
        public Long expireAfter = null;

        public Options() {
        }

        public Options unique(boolean unique) {
            this.unique = unique;
            return this;
        }

        public Options expireAfter(Long expireAfter) {
            this.expireAfter = expireAfter;
            return this;
        }

    }

}