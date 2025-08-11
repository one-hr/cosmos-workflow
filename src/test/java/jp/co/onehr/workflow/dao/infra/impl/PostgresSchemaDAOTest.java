package jp.co.onehr.workflow.dao.infra.impl;

import io.github.thunderz99.cosmos.impl.postgres.PostgresDatabaseImpl;
import io.github.thunderz99.cosmos.impl.postgres.util.TTLUtil;
import io.github.thunderz99.cosmos.impl.postgres.util.TableUtil;
import jp.co.onehr.workflow.ProcessConfiguration;
import jp.co.onehr.workflow.dao.CosmosDB;
import jp.co.onehr.workflow.service.base.BaseNoSqlService;
import jp.co.onehr.workflow.util.InfraUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.atteo.evo.inflector.English;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import static jp.co.onehr.workflow.dao.infra.impl.PostgresSchemaDAO.getDataSource;
import static org.assertj.core.api.Assertions.assertThat;

class PostgresSchemaDAOTest {

    static String host = "localhost";

    @Test
    @EnabledIf("isPostgres")
    void createTableIfNotExist_should_work() throws Exception {

        var dao = new PostgresSchemaDAO();
        var className = "PostgresSchemaDAO" + RandomStringUtils.randomAlphanumeric(4) + "Test";

        var partitionName = BaseNoSqlService.addSuffixToPartition(English.plural(className));

        try {
            // SimpleData

            var tableName = dao.createTableIfNotExist(host, partitionName);
            assertThat(tableName).contains(".\"%s\"".formatted(partitionName));

        } finally {
            dropTableIfExists(host, className);
        }

    }

    @Test
    @EnabledIf("isPostgres")
    void createIndexesIfNotExist_should_work() throws Exception {
        var dao = new PostgresSchemaDAO();
        var className = "PostgresSchemaDAO" + RandomStringUtils.randomAlphanumeric(4) + "Test";
        var partitionName = BaseNoSqlService.addSuffixToPartition(English.plural(className)) + "_recycle";

        var db = (PostgresDatabaseImpl) ProcessConfiguration.getConfiguration().getDatabase(host);
        var schemaName = ProcessConfiguration.getConfiguration().getCollectionName(host);

        try {
            // recycle partition
            var tableName = dao.createTableIfNotExist(host, partitionName);
            assertThat(tableName).contains(".\"%s\"".formatted(partitionName));
            var result = dao.createIndexesIfNotExist(host, partitionName);
            assertThat(result).hasSize(2);
            try (var conn = db.getDataSource().getConnection()) {
                var job = TTLUtil.findJobByName(conn, schemaName, StringUtils.substringAfter(tableName, "."));
                assertThat(job).isNotNull();
                assertThat(job.schedule).contains("0 21 * * *");
            }
            // pg_cron job is created for recycle partition
            // job name should be "Data_xxx_ttl_job_PostgresSchemaDAOTests_recycle"
            assertThat(result.get(0)).contains("%s_ttl_job_%s".formatted(schemaName, partitionName));

            // index name should contain "idx_PostgresSchemaDAOTests_recycle__expireAt_1"
            assertThat(result.get(1)).contains("idx_%s__expireAt_1".formatted(partitionName));

            // delete pg_cron job
            assertThat(db.disableTTL(schemaName, partitionName)).isTrue();
        } finally {
            db.disableTTL(schemaName, partitionName);
            dropTableIfExists(host, partitionName);
        }
    }

    /**
     * Delete the table for the specified partitionName(helper method only used in unit test)
     *
     * @param host
     * @param partitionName
     * @throws Exception
     */
    void dropTableIfExists(String host, String partitionName) throws Exception {

        var dataSource = getDataSource(host);
        var schemaName = ProcessConfiguration.getConfiguration().getCollectionName(host);

        try (var conn = dataSource.getConnection()) {
            TableUtil.dropTableIfExists(conn, schemaName, partitionName);
        }
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