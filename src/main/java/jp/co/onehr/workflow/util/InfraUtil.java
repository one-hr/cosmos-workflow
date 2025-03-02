package jp.co.onehr.workflow.util;

import org.apache.commons.lang3.StringUtils;

/**
 * Util class that get dbType(cosmosdb or mongodb), storageType, etc
 *
 * <p>
 *   In order to support SaaS and On-premise both;
 * </p>
 */
public class InfraUtil {

    public static final String FW_INFRA_DATABASE_TYPE = "FW_INFRA_DATABASE_TYPE";

    /**
     * Get the dbType of current infrastructure("cosmosdb" or "mongodb"). "cosmosdb" is default value
     * @return dbType
     */
    public static String getDbType() {
        return EnvUtil.getOrDefault(FW_INFRA_DATABASE_TYPE, "cosmosdb");
    }

    /**
     * Whether current infra uses CosmosDB
     * @return true/false
     */
    public static boolean isCosmosDB(){
        return StringUtils.equals(getDbType(), "cosmosdb");
    }

    /**
     * Whether current infra uses MongoDB
     * @return true/false
     */
    public static boolean isMongoDB(){
        return StringUtils.equals(getDbType(), "mongodb");
    }

    /**
     * Whether current infra uses Postgres
     * @return true/false
     */
    public static boolean isPostgres(){
        return StringUtils.equals(getDbType(), "postgres");
    }

}

