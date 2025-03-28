package jp.co.onehr.workflow.util;

import io.github.thunderz99.cosmos.CosmosDatabase;
import io.github.thunderz99.cosmos.impl.cosmosdb.CosmosDatabaseImpl;
import io.github.thunderz99.cosmos.impl.mongo.MongoDatabaseImpl;
import io.github.thunderz99.cosmos.impl.postgres.PostgresDatabaseImpl;
import jp.co.onehr.workflow.ProcessConfiguration;
import jp.co.onehr.workflow.dao.CosmosDB;
import org.apache.commons.lang3.ObjectUtils;

import static jp.co.onehr.workflow.service.base.BaseNoSqlService.judgeEnableDefaultWorkflowDB;

/**
 * Util class that get dbType(cosmosdb or mongodb), storageType, etc
 *
 * <p>
 * In order to support SaaS and On-premise both;
 * </p>
 */
public class InfraUtil {

    public static final String FW_INFRA_DATABASE_TYPE = "FW_INFRA_DATABASE_TYPE";

    /**
     * Get the dbType of current infrastructure("cosmosdb" or "mongodb"). "cosmosdb" is default value
     *
     * @return dbType
     */
    public static String getDbType(String connectionString) {
        return ConnectionStringUtil.getDbTypeByConnectionString(connectionString);
    }

    /**
     * Get whether the db of the specified host is CosmosDB or not
     *
     * @param host
     * @return true/false
     * @throws Exception
     */
    public static boolean isCosmosDB(String host) {
        var db = ProcessConfiguration.getConfiguration().getDatabase(host);
        return isCosmosDB(db);
    }

    /**
     * Get whether the db of the specified host is MongoDB or not
     *
     * @param host
     * @return true/false
     * @throws Exception
     */
    public static boolean isMongoDB(String host) {
        var db = ProcessConfiguration.getConfiguration().getDatabase(host);
        return isMongoDB(db);
    }

    /**
     * Get whether the db of the specified host is Postgres or not
     *
     * @param host
     * @return true/false
     * @throws Exception
     */
    public static boolean isPostgres(String host) {
        var db = ProcessConfiguration.getConfiguration().getDatabase(host);
        return isPostgres(db);
    }

    /**
     * Whether current infra uses CosmosDB
     *
     * @return true/false
     */
    public static boolean isCosmosDB(CosmosDatabase db) {
        return db instanceof CosmosDatabaseImpl;
    }

    /**
     * Whether current infra uses MongoDB
     *
     * @return true/false
     */
    public static boolean isMongoDB(CosmosDatabase db) {
        return db instanceof MongoDatabaseImpl;
    }

    /**
     * Whether current infra uses postgres
     *
     * @return true/false
     */
    public static boolean isPostgres(CosmosDatabase db) {
        return db instanceof PostgresDatabaseImpl;
    }

    /**
     * Retrieve the corresponding database type based on the host
     *
     * @param host
     * @return
     */
    public static String getDbTypeByHost(String host) {
        var db = ProcessConfiguration.getConfiguration().getDatabase(host);

        if (ObjectUtils.isEmpty(db) && judgeEnableDefaultWorkflowDB()) {
            db = CosmosDB.registerDefaultWorkflowDB(host);
        }

        if (isCosmosDB(db)) {
            return "cosmosdb";
        }

        if (isMongoDB(db)) {
            return "mongodb";
        }

        if (isPostgres(db)) {
            return "postgres";
        }

        throw new IllegalStateException("The database type corresponding to the host is invalid.");
    }

}

