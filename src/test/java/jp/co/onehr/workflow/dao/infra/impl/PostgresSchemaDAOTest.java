package jp.co.onehr.workflow.dao.infra.impl;

import jp.co.onehr.workflow.service.base.BaseNoSqlService;
import org.apache.commons.lang3.RandomStringUtils;
import org.atteo.evo.inflector.English;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

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
            dao.dropTableIfExists(host, className);
        }

    }
}