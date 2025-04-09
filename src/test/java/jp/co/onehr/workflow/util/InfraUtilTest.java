package jp.co.onehr.workflow.util;

import jp.co.onehr.workflow.ProcessConfiguration;
import jp.co.onehr.workflow.dao.CosmosDB;
import org.apache.commons.lang3.ObjectUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import static org.assertj.core.api.Assertions.assertThat;

public class InfraUtilTest {

    static String host = "localhost";

    @Test
    @EnabledIf("isCosmosDB")
    void getDbTypeByHost_cosmosdb_should_work() throws Exception {
        var dbType = InfraUtil.getDbTypeByHost(host);
        assertThat(dbType).isEqualTo("cosmosdb");
    }

    @Test
    @EnabledIf("isMongoDB")
    void getDbTypeByHost_mongodb_should_work() throws Exception {
        var dbType = InfraUtil.getDbTypeByHost(host);
        assertThat(dbType).isEqualTo("mongodb");
    }

    @Test
    @EnabledIf("isPostgres")
    void getDbTypeByHost_postgres_should_work() throws Exception {
        var dbType = InfraUtil.getDbTypeByHost(host);
        assertThat(dbType).isEqualTo("postgres");
    }

    /**
     * condition method.
     */
    static boolean isCosmosDB() {
        var db = ProcessConfiguration.getConfiguration().getDatabase(host);
        if (ObjectUtils.isEmpty(db)) {
            db = CosmosDB.registerDefaultWorkflowDB(host);
        }
        return InfraUtil.isCosmosDB(db);
    }

    /**
     * condition method.
     */
    static boolean isMongoDB() {
        var db = ProcessConfiguration.getConfiguration().getDatabase(host);
        if (ObjectUtils.isEmpty(db)) {
            db = CosmosDB.registerDefaultWorkflowDB(host);
        }
        return InfraUtil.isMongoDB(host);
    }

    /**
     * condition method.
     */
    static boolean isPostgres() {
        var db = ProcessConfiguration.getConfiguration().getDatabase(host);
        if (ObjectUtils.isEmpty(db)) {
            db = CosmosDB.registerDefaultWorkflowDB(host);
        }
        return InfraUtil.isPostgres(db);
    }
}
