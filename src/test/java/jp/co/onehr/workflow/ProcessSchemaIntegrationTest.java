package jp.co.onehr.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.github.thunderz99.cosmos.impl.postgres.PostgresDatabaseImpl;
import io.github.thunderz99.cosmos.impl.postgres.util.TableUtil;
import io.github.thunderz99.cosmos.impl.postgres.util.TTLUtil;
import jp.co.onehr.workflow.dao.CosmosDB;
import jp.co.onehr.workflow.service.DefinitionService;
import jp.co.onehr.workflow.service.InstanceService;
import jp.co.onehr.workflow.service.WorkflowService;
import jp.co.onehr.workflow.util.InfraUtil;
import jp.co.onehr.workflow.util.PGTableTestUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessSchemaIntegrationTest {

    /**
     * Third test for {@link ProcessConfiguration#ensureTables(String)}: unlike the database-free cases in
     * {@code ProcessConfigurationTest}, this path verifies physical PostgreSQL tables, indexes and TTL jobs
     * in isolated schemas, so it needs a real PostgreSQL backend.
     */
    @Test
    @EnabledIf("isPostgres")
    void ensureTables_should_restore_built_in_schema_success() throws Exception {
        var configuration = ProcessConfiguration.getConfiguration();
        var database = (PostgresDatabaseImpl) CosmosDB.getDefaultDatabaseByEnv();
        var suffix = UUID.randomUUID().toString().replace("-", "");
        var firstSchema = "wf_schema_" + suffix + "_a";
        var secondSchema = "wf_schema_" + suffix + "_b";
        var otherSchema = "wf_schema_" + suffix + "_c";
        var firstHost = "wf-schema-" + suffix;
        var otherHost = "wf-schema-other-" + suffix;
        var workflow = WorkflowService.singleton.getPartition();
        var definition = DefinitionService.singleton.getPartition();
        var instance = InstanceService.singleton.getPartition();
        var expected = List.of(workflow, workflow + "_recycle",
                definition, definition + "_recycle", instance, instance + "_recycle");
        var createdSchemas = new ArrayList<String>();

        try {
            try (var conn = database.getDataSource().getConnection(); var stmt = conn.createStatement()) {
                stmt.execute("CREATE SCHEMA " + TableUtil.checkAndNormalizeValidEntityName(firstSchema));
                createdSchemas.add(firstSchema);
                stmt.execute("CREATE SCHEMA " + TableUtil.checkAndNormalizeValidEntityName(secondSchema));
                createdSchemas.add(secondSchema);
                stmt.execute("CREATE SCHEMA " + TableUtil.checkAndNormalizeValidEntityName(otherSchema));
                createdSchemas.add(otherSchema);
            }
            configuration.registerDB(firstHost, database, firstSchema);
            configuration.registerDB(otherHost, database, otherSchema);

            // scenario: The public entry creates all six built-in tables and required indexes.
            {
                assertThat(configuration.ensureTables(firstHost)).containsExactlyElementsOf(expected);
                try (var conn = database.getDataSource().getConnection()) {
                    for (var table : expected) {
                        assertThat(TableUtil.tableExist(conn, firstSchema, table)).isTrue();
                        var indexes = PGTableTestUtil.findIndexes(conn, firstSchema, table);
                        var dataIndex = TableUtil.removeQuotes(TableUtil.getIndexName(
                                TableUtil.checkAndNormalizeValidEntityName(table), TableUtil.DATA));
                        assertThat(indexes).containsKey(dataIndex);
                        assertThat(indexes.get(dataIndex)).containsIgnoringCase("USING gin (data)");
                    }
                    var definitionIndexes = PGTableTestUtil.findIndexes(conn, firstSchema, definition);
                    assertThat(definitionIndexes).containsKey(TableUtil.removeQuotes(TableUtil.getIndexName(
                            TableUtil.checkAndNormalizeValidEntityName(definition), "workflowId")));
                    for (var table : List.of(workflow + "_recycle", definition + "_recycle", instance + "_recycle")) {
                        var indexes = PGTableTestUtil.findIndexes(conn, firstSchema, table);
                        assertThat(indexes).containsKey(TableUtil.removeQuotes(TableUtil.getIndexName(
                                TableUtil.checkAndNormalizeValidEntityName(table), "_expireAt")));
                        try (var query = conn.prepareStatement(
                                "SELECT active, schedule, command FROM cron.job WHERE jobname = ?")) {
                            query.setString(1, TTLUtil.getJobName(firstSchema, table));
                            try (var jobs = query.executeQuery()) {
                                assertThat(jobs.next()).isTrue();
                                assertThat(jobs.getBoolean("active")).isTrue();
                                assertThat(jobs.getString("schedule").trim()).isEqualTo("0 21 * * *");
                                assertThat(jobs.getString("command")).contains("DELETE FROM "
                                        + TableUtil.checkAndNormalizeValidEntityName(firstSchema) + "."
                                        + TableUtil.checkAndNormalizeValidEntityName(table));
                            }
                        }
                    }
                }
            }

            // scenario: Repeating the public entry returns all logical partitions.
            {
                assertThat(configuration.ensureTables(firstHost)).containsExactlyElementsOf(expected);
            }

            // scenario: A forced check restores a deleted table after a successful initialization.
            {
                try (var conn = database.getDataSource().getConnection()) {
                    TableUtil.dropTableIfExists(conn, firstSchema, definition);
                    assertThat(TableUtil.tableExist(conn, firstSchema, definition)).isFalse();
                }
                assertThat(configuration.ensureTables(firstHost)).containsExactlyElementsOf(expected);
                try (var conn = database.getDataSource().getConnection()) {
                    assertThat(TableUtil.tableExist(conn, firstSchema, definition)).isTrue();
                    assertThat(PGTableTestUtil.findIndexes(conn, firstSchema, definition)).containsKey(
                            TableUtil.removeQuotes(TableUtil.getIndexName(
                                    TableUtil.checkAndNormalizeValidEntityName(definition), "workflowId")));
                }
            }

            // scenario: A forced check tolerates TTL jobs unscheduled or deactivated by another component.
            {
                var unscheduled = workflow + "_recycle";
                var deactivated = definition + "_recycle";
                database.disableTTL(firstSchema, unscheduled);
                try (var conn = database.getDataSource().getConnection(); var query = conn.prepareStatement(
                        "SELECT cron.alter_job(jobid, active := false) FROM cron.job WHERE jobname = ?")) {
                    query.setString(1, TTLUtil.getJobName(firstSchema, deactivated));
                    try (var rows = query.executeQuery()) {
                        assertThat(rows.next()).isTrue();
                    }
                }
                assertThat(configuration.ensureTables(firstHost)).containsExactlyElementsOf(expected);
                try (var conn = database.getDataSource().getConnection()) {
                    assertThat(TTLUtil.jobExists(conn, firstSchema, unscheduled)).isTrue();
                }
            }

            // scenario: Invalidating a rebuilt schema with the same name restores its tables.
            {
                for (var table : List.of(workflow + "_recycle", definition + "_recycle", instance + "_recycle")) {
                    database.disableTTL(firstSchema, table);
                }
                try (var conn = database.getDataSource().getConnection(); var stmt = conn.createStatement()) {
                    var name = TableUtil.checkAndNormalizeValidEntityName(firstSchema);
                    stmt.execute("DROP SCHEMA " + name + " CASCADE");
                    stmt.execute("CREATE SCHEMA " + name);
                }
                configuration.invalidateSchemaCache(firstHost);
                // Lazy access re-creates the tables only because the cached success was invalidated.
                WorkflowService.singleton.getColl(firstHost);
                DefinitionService.singleton.getColl(firstHost);
                InstanceService.singleton.getColl(firstHost);
                try (var conn = database.getDataSource().getConnection()) {
                    for (var table : expected) {
                        assertThat(TableUtil.tableExist(conn, firstSchema, table)).isTrue();
                    }
                }
            }

            // scenario: Switching one host's schema leaves another host's registration and tables separate.
            {
                assertThat(configuration.ensureTables(otherHost)).containsExactlyElementsOf(expected);
                try (var conn = database.getDataSource().getConnection()) {
                    TableUtil.dropTableIfExists(conn, otherSchema, workflow);
                }
                configuration.registerDB(firstHost, database, secondSchema);
                // Lazy access uses the new schema only because the registration change invalidated the cache.
                WorkflowService.singleton.getColl(firstHost);
                DefinitionService.singleton.getColl(firstHost);
                InstanceService.singleton.getColl(firstHost);
                assertThat(configuration.getDatabase(otherHost)).isSameAs(database);
                assertThat(configuration.getCollectionName(otherHost)).isEqualTo(otherSchema);
                try (var conn = database.getDataSource().getConnection()) {
                    for (var table : expected) {
                        assertThat(TableUtil.tableExist(conn, secondSchema, table)).isTrue();
                    }
                    assertThat(TableUtil.tableExist(conn, otherSchema, workflow)).isFalse();
                }
                assertThat(configuration.ensureTables(otherHost)).containsExactlyElementsOf(expected);
            }
        } finally {
            for (var schema : createdSchemas) {
                for (var table : List.of(workflow + "_recycle", definition + "_recycle", instance + "_recycle")) {
                    database.disableTTL(schema, table);
                }
                try (var conn = database.getDataSource().getConnection(); var stmt = conn.createStatement()) {
                    stmt.execute("DROP SCHEMA " + TableUtil.checkAndNormalizeValidEntityName(schema) + " CASCADE");
                }
            }
        }
    }

    static boolean isPostgres() {
        return InfraUtil.isPostgres(CosmosDB.getDefaultDatabaseByEnv());
    }
}
