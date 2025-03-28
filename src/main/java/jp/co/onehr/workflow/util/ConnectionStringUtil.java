package jp.co.onehr.workflow.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionStringUtil {

    static Logger log = LoggerFactory.getLogger(ConnectionStringUtil.class);

    /**
     * get the dbType of current db("cosmosdb" or "mongodb" or "postgres") from connectionString
     *
     * @param connectionString
     * @return dbType
     */
    public static String getDbTypeByConnectionString(String connectionString) {

        if (StringUtils.isEmpty(connectionString)) {
            throw new IllegalArgumentException("connectionString is empty:" + connectionString);
        }

        if (StringUtils.containsAny(connectionString, "postgres://", "postgresql://")) {
            return "postgres";
        }

        if (StringUtils.containsAny(connectionString, "mongodb://", "mongodb+srv://")) {
            return "mongodb";
        }

        if (StringUtils.contains(connectionString, "AccountEndpoint=")) {
            return "cosmosdb";
        }

        throw new IllegalStateException("connectionString is invalid. Make sure connectionString contains 'AccountEndpoint=' or 'mongodb://' or 'mongodb+srv://' or 'postgresql://. " + connectionString);
    }
}
