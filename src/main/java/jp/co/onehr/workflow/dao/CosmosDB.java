package jp.co.onehr.workflow.dao;

import io.github.thunderz99.cosmos.Cosmos;
import io.github.thunderz99.cosmos.CosmosDatabase;
import jp.co.onehr.workflow.ProcessEngineConfiguration;
import jp.co.onehr.workflow.service.base.BaseNoSqlService;
import jp.co.onehr.workflow.util.EnvUtil;

import static jp.co.onehr.workflow.service.base.BaseNoSqlService.DEFAULT_COLLECTION;


public class CosmosDB {

    public static final String FW_WORKFLOW_CONNECTION_STRING = "FW_WORKFLOW_CONNECTION_STRING";
    public static final String FW_WORKFLOW_DATABASE_NAME = "FW_WORKFLOW_DATABASE_NAME";

    public static final String DEFAULT_DATABASE_NAME = "Data";

    /**
     * get the default database from the environment variables
     *
     * @return
     */
    public static CosmosDatabase getDefaultDatabaseByEnv() {
        String connectionString = EnvUtil.get(FW_WORKFLOW_CONNECTION_STRING);
        String dbName = EnvUtil.getOrDefault(FW_WORKFLOW_DATABASE_NAME, DEFAULT_DATABASE_NAME);

        return new Cosmos(connectionString).getDatabase(dbName);
    }

    /**
     * Register the default database with the workflow engine
     *
     * @param host
     * @return
     */
    public static CosmosDatabase registerDefaultWorkflowDB(String host) {
        var collectionName = BaseNoSqlService.getCollectionNameByEnv(DEFAULT_COLLECTION);
        var db = getDefaultDatabaseByEnv();
        ProcessEngineConfiguration.getConfiguration().registerDB(host, db, collectionName);
        return ProcessEngineConfiguration.getConfiguration().getDatabase(host);
    }
}
