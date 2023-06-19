package jp.co.onehr.workflow.dto;

import com.google.common.collect.Maps;
import io.github.thunderz99.cosmos.CosmosDatabase;
import jp.co.onehr.workflow.dto.base.SimpleData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;


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

}
