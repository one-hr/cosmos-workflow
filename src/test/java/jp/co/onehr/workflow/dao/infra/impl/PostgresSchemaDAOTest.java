package jp.co.onehr.workflow.dao.infra.impl;

import java.util.List;
import java.util.UUID;

import io.github.thunderz99.cosmos.impl.postgres.PostgresDatabaseImpl;
import io.github.thunderz99.cosmos.impl.postgres.util.TTLUtil;
import io.github.thunderz99.cosmos.impl.postgres.util.TableUtil;
import jp.co.onehr.workflow.ProcessConfiguration;
import jp.co.onehr.workflow.dao.CosmosDB;
import jp.co.onehr.workflow.dto.base.BaseData;
import jp.co.onehr.workflow.dto.base.index.IndexCustomizable;
import jp.co.onehr.workflow.dto.base.index.IndexDefinition;
import jp.co.onehr.workflow.dto.base.index.IndexField;
import jp.co.onehr.workflow.dto.base.index.IndexFieldType;
import jp.co.onehr.workflow.service.base.BaseNoSqlService;
import jp.co.onehr.workflow.util.InfraUtil;
import jp.co.onehr.workflow.util.PGTableTestUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.atteo.evo.inflector.English;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import static jp.co.onehr.workflow.dao.infra.impl.PostgresSchemaDAO.getDataSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
     * Test implementation of IndexCustomizable (normal case).
     */
    static class MemberCustomIndexTest extends BaseData implements IndexCustomizable {
        @Override
        public List<IndexDefinition> getCustomIndexDefinitions() {
            return List.of(
                    IndexDefinition.of(IndexField.of("employeeCode", IndexFieldType.TEXT), true),
                    IndexDefinition.of(
                            List.of(IndexField.of("mail"),
                                    IndexField.of("photo.version", IndexFieldType.NUMERIC)),
                            false)
            );
        }
    }


    /**
     * Tests for CustomIndex (normal case).
     *
     * @throws Exception
     */
    @Test
    @EnabledIf("isPostgres")
    void createCustomIndexIfNotExist_should_work() throws Exception {
        var dao = new PostgresSchemaDAO();
        var partitionName = "MemberCustomIndexTest";
        var dataSource = getDataSource(host);
        var schemaName = ProcessConfiguration.getConfiguration().getCollectionName(host);
        try {
            // Create table first
            var tableName = dao.createTableIfNotExist(host, partitionName);
            assertThat(tableName).contains(".\"" + partitionName + "\"");

            // Create custom indexes
            var result = dao.createCustomIndexIfNotExist(host, partitionName, new MemberCustomIndexTest());

            // Verify two indexes were created
            assertThat(result).hasSize(2);

            // Verify index names contain expected field names
            var indexNames = String.join(",", result);
            assertThat(indexNames).contains("employeeCode");
            assertThat(indexNames).contains("mail_photo_version");

            // Verify index creation in database
            try (var conn = dataSource.getConnection()) {
                var indexes = PGTableTestUtil.findIndexes(conn, schemaName, partitionName);
                // 1. pk,
                // 2. data(gin) index
                // 3. custom index employeeCode
                // 4. custom index mail_photo_version
                assertThat(indexes).hasSize(4);

                // employeeCode index
                var indexName1 = "idx_" + partitionName + "_employeeCode_1";
                assertThat(indexes).containsKey(indexName1);
                var indexDef1 = indexes.get(indexName1);
                assertThat(indexDef1).contains("UNIQUE INDEX");
                assertThat(indexDef1).contains("::text");

                // mail_photo_version index
                var indexName2 = "idx_" + partitionName + "_mail_photo_version_1";
                assertThat(indexes).containsKey(indexName2);
                var indexDef2 = indexes.get(indexName2);
                assertThat(indexDef2).doesNotContain("UNIQUE INDEX");
                assertThat(indexDef2).contains("::numeric");
            }

        } finally {
            // Clean up
            try (var conn = dataSource.getConnection()) {
                TableUtil.dropTableIfExists(conn, schemaName, partitionName);
            }
        }
    }

    /**
     * // Test implementation of IndexCustomizable (boundary case: field name too long).
     */
    static class TooLongTest extends BaseData implements IndexCustomizable {
        @Override
        public List<IndexDefinition> getCustomIndexDefinitions() {
            return List.of(
                    // 75 chars, numeric, not unique
                    IndexDefinition.of(IndexField.of("field1_d6ab910d-59c0-4b64-a71d-763816ce249d_1234567890_1234567890_123456789", IndexFieldType.NUMERIC)),
                    // 62 chars, text, unique
                    IndexDefinition.of(IndexField.of("field2_d6ab910d-59c0-4b64-a71d-763816ce249d_1234567890_1234567", IndexFieldType.TEXT), true)

            );
        }
    }


    /**
     * Tests for CustomIndex (boundary test: excessively long field name).
     * <p>
     * The index name is automatically shortened.
     * </p>
     *
     * @throws Exception
     */
    @Test
    @EnabledIf("isPostgres")
    void createCustomIndexIfNotExist_should_work_4_too_long_field_name() throws Exception {
        var dao = new PostgresSchemaDAO();
        var partitionName = "TooLongTests";
        var dataSource = getDataSource(host);
        var schemaName = ProcessConfiguration.getConfiguration().getCollectionName(host);

        try {
            // Create table first
            var tableName = dao.createTableIfNotExist(host, partitionName);
            assertThat(tableName).contains(".\"" + partitionName + "\"");

            // Create custom indexes
            var result = dao.createCustomIndexIfNotExist(host, partitionName, new TooLongTest());

            // Verify 1 index were created
            assertThat(result).hasSize(2);

            // Verify index names contain expected field names
            var indexNames = String.join(",", result);
            assertThat(indexNames).contains("field1_d6");
            assertThat(indexNames).contains("field2_d6");

            // Verify index creation in database
            try (var conn = dataSource.getConnection()) {
                var indexes = PGTableTestUtil.findIndexes(conn, schemaName, partitionName);
                // 1. pk,
                // 2. data(gin) index
                // 3. custom index tooLongFieldName1_d6ab910d
                // 4. custom index tooLongFieldName2_d6ab910d

                assertThat(indexes).hasSize(4);

                // tooLongFieldName_d6ab910d index

                var indexName1 = TableUtil.removeQuotes(result.get(0).split("\\.")[1]);
                assertThat(indexes).containsKey(indexName1);
                var indexDef1 = indexes.get(indexName1);
                assertThat(indexDef1).doesNotContain("UNIQUE INDEX");
                assertThat(indexDef1).contains("::text))::numeric");

                var indexName2 = TableUtil.removeQuotes(result.get(1).split("\\.")[1]);
                assertThat(indexes).containsKey(indexName2);
                var indexDef2 = indexes.get(indexName2);
                assertThat(indexDef2).contains("UNIQUE INDEX");
                assertThat(indexDef2).contains("::text");
            }

        } finally {
            // Clean up
            try (var conn = dataSource.getConnection()) {
                TableUtil.dropTableIfExists(conn, schemaName, partitionName);
            }
        }
    }

    /**
     * Tests for _createIndexIfNotExist (error case: partitionName).
     */
    @Test
    @EnabledIf("isPostgres")
    void _createIndexIfNotExist_should_handle_invalid_partitionName() throws Exception {
        var dao = new PostgresSchemaDAO();
        var indexDefinitions = List.of(
                IndexDefinition.of(IndexField.of("testField", IndexFieldType.TEXT), true)
        );

        {
            // null
            var result = dao._createIndexIfNotExist(host, null, indexDefinitions);
            assertThat(result).isEmpty();
        }

        {
            // empty
            var result = dao._createIndexIfNotExist(host, "", indexDefinitions);
            assertThat(result).isEmpty();
        }

        {
            // blank
            assertThatThrownBy(() -> dao._createIndexIfNotExist(host, "   ", indexDefinitions))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("entityName should be non-blank");
        }

        {
            // table does not exist
            var nonExistentPartition = "NonExistentTable_" + UUID.randomUUID();
            var result = dao._createIndexIfNotExist(host, nonExistentPartition, indexDefinitions);
            assertThat(result).isEmpty();
        }
    }

    /**
     * Tests for _createIndexIfNotExist (error case: indexDefinitions).
     */
    @Test
    @EnabledIf("isPostgres")
    void _createIndexIfNotExist_should_handle_invalid_indexDefinitions() throws Exception {
        var dao = new PostgresSchemaDAO();
        var partitionName = "TestPartition";

        {
            // null
            var result = dao._createIndexIfNotExist(host, partitionName, null);
            assertThat(result).isEmpty();
        }

        {
            // empty list
            var result = dao._createIndexIfNotExist(host, partitionName, List.of());
            assertThat(result).isEmpty();
        }
    }

    /**
     * Tests for _createIndexIfNotExist (error cases: combinations of multiple abnormal conditions)
     */
    @Test
    @EnabledIf("isPostgres")
    void _createIndexIfNotExist_should_handle_multiple_edge_cases() throws Exception {
        var dao = new PostgresSchemaDAO();

        {
            // null partitionName + null indexDefinitions
            var result = dao._createIndexIfNotExist(host, null, null);
            assertThat(result).isEmpty();
        }

        {
            // empty partitionName + empty indexDefinitions
            var result = dao._createIndexIfNotExist(host, "", List.of());
            assertThat(result).isEmpty();
        }

        {
            // blank partitionName + null indexDefinitions
            var result = dao._createIndexIfNotExist(host, "  ", null);
            assertThat(result).isEmpty();
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