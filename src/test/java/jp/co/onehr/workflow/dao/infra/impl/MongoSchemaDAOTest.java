package jp.co.onehr.workflow.dao.infra.impl;

import jp.co.onehr.workflow.ProcessConfiguration;
import jp.co.onehr.workflow.dao.CosmosDB;
import jp.co.onehr.workflow.service.base.BaseNoSqlService;
import jp.co.onehr.workflow.util.InfraUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.atteo.evo.inflector.English;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import static org.assertj.core.api.Assertions.assertThat;

class MongoSchemaDAOTest {
    static String host = "localhost";

    @Test
    @EnabledIf("isMongoDB")
    void createTablesIfNotExist_should_work() throws Exception {
        var dao = new MongoSchemaDAO();
        assertThat(dao.createTableIfNotExist(host, "Nodes")).isEmpty();
    }


    @Test
    @EnabledIf("isMongoDB")
    void createIndexIfNotExist_should_work() throws Exception {

        {
            // index not exist for not exist collection
            assertThat(MongoSchemaDAO.singleton.indexExist(host,
                    "NotExist_" + RandomStringUtils.randomAlphanumeric(9), "id")).isFalse();
        }

        {
            var className = "Index" + RandomStringUtils.randomAlphanumeric(6) + "Test";

            var partition = BaseNoSqlService.addSuffixToPartition(English.plural(className));

            try {
                var results = MongoSchemaDAO.singleton.createIndexesIfNotExist(host, partition);

                assertThat(results).hasSize(3);
                assertThat(results.get(0)).isEqualTo(partition + "." + "id");
                assertThat(results.get(1)).isEqualTo(partition + "." + "_ts");
                assertThat(results.get(2)).isEqualTo(partition + "." + "_expireAt");

            } finally {
                MongoSchemaDAO.singleton.deleteIndex(host, partition, "id");
                MongoSchemaDAO.singleton.deleteIndex(host, partition, "_ts");
                MongoSchemaDAO.singleton.deleteIndex(host, partition, "_expireAt");
            }

        }

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
}