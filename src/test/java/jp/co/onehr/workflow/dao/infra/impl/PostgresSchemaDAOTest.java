package jp.co.onehr.workflow.dao.infra.impl;

import io.github.thunderz99.cosmos.impl.postgres.util.TableUtil;
import jp.co.onehr.workflow.ProcessConfiguration;
import jp.co.onehr.workflow.service.base.BaseNoSqlService;
import org.apache.commons.lang3.RandomStringUtils;
import org.atteo.evo.inflector.English;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import static jp.co.onehr.workflow.dao.infra.impl.PostgresSchemaDAO.getDataSource;
import static org.assertj.core.api.Assertions.assertThat;

class PostgresSchemaDAOTest {

    String host = "localhost";

    @Test
    @EnabledIf("jp.co.onehr.workflow.util.InfraUtil#isPostgres")
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

        try(var conn = dataSource.getConnection()){
            TableUtil.dropTableIfExists(conn, schemaName, partitionName);
        }
    }
}