package jp.co.onehr.workflow;

import java.util.UUID;

import io.github.thunderz99.cosmos.impl.cosmosdb.CosmosDatabaseImpl;
import jp.co.onehr.workflow.exception.WorkflowException;
import jp.co.onehr.workflow.service.DefinitionService;
import jp.co.onehr.workflow.service.InstanceService;
import jp.co.onehr.workflow.service.WorkflowService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessConfigurationTest {

    @Test
    void registerDB_should_preserve_or_replace_target_success() {
        var configuration = ProcessConfiguration.getConfiguration();
        var first = new CosmosDatabaseImpl(null, "first");

        // scenario: Re-registering a wrapper for the same database and collection keeps the current registration.
        {
            var host = "schema-registration-" + UUID.randomUUID();
            configuration.registerDB(host, first, "schema_a");

            configuration.registerDB(host, new CosmosDatabaseImpl(null, "first"), "schema_a");

            assertThat(configuration.getDatabase(host)).isSameAs(first);
            assertThat(configuration.getCollectionName(host)).isEqualTo("schema_a");
        }

        // scenario: A collection change replaces the registered collection.
        {
            var host = "schema-collection-" + UUID.randomUUID();
            configuration.registerDB(host, first, "schema_a");

            configuration.registerDB(host, first, "schema_b");

            assertThat(configuration.getDatabase(host)).isSameAs(first);
            assertThat(configuration.getCollectionName(host)).isEqualTo("schema_b");
        }

        // scenario: A database change replaces the registered database.
        {
            var host = "schema-database-" + UUID.randomUUID();
            configuration.registerDB(host, first, "schema_b");

            configuration.registerDB(host, new CosmosDatabaseImpl(null, "second"), "schema_b");

            assertThat(configuration.getDatabase(host).getDatabaseName()).isEqualTo("second");
            assertThat(configuration.getCollectionName(host)).isEqualTo("schema_b");
        }
    }

    @Test
    void ensureTables_should_return_cosmos_logical_partitions_success() throws Exception {
        var configuration = ProcessConfiguration.getConfiguration();

        // scenario: Cosmos DB completes the no-op schema path without contacting an account.
        {
            var host = "schema-cosmos-" + UUID.randomUUID();
            configuration.registerDB(host, new CosmosDatabaseImpl(null, "database"), "container");
            var workflow = WorkflowService.singleton.getPartition();
            var definition = DefinitionService.singleton.getPartition();
            var instance = InstanceService.singleton.getPartition();

            var tables = configuration.ensureTables(host);

            assertThat(tables).containsExactly(workflow, workflow + "_recycle", definition,
                    definition + "_recycle", instance, instance + "_recycle");
            assertThat(configuration.ensureTables(host)).isEqualTo(tables);
            configuration.invalidateSchemaCache(host);
            assertThat(configuration.ensureTables(host)).isEqualTo(tables);
        }
    }

    @Test
    void ensureTables_should_reject_incomplete_registration_failed() {
        var configuration = ProcessConfiguration.getConfiguration();

        // scenario: An explicitly registered database without a collection retains the registration error contract.
        {
            var host = "schema-invalid-" + UUID.randomUUID();
            configuration.registerDB(host, new CosmosDatabaseImpl(null, "database"), "");

            assertThatThrownBy(() -> configuration.ensureTables(host)).isInstanceOf(WorkflowException.class);
        }
    }

    @Test
    void invalidateSchemaCache_should_keep_registration_success() {
        var configuration = ProcessConfiguration.getConfiguration();

        // scenario: Invalidating a host keeps its registered target.
        {
            var host = "schema-invalidate-" + UUID.randomUUID();
            var database = new CosmosDatabaseImpl(null, "shared");
            configuration.registerDB(host, database, "schema");

            configuration.invalidateSchemaCache(host);

            assertThat(configuration.getDatabase(host)).isSameAs(database);
            assertThat(configuration.getCollectionName(host)).isEqualTo("schema");
        }

        // scenario: Invalidating an unknown host does not register it.
        {
            var unknown = "schema-unknown-" + UUID.randomUUID();

            configuration.invalidateSchemaCache(unknown);

            assertThat(configuration.getDatabase(unknown)).isNull();
            assertThat(configuration.getCollectionName(unknown)).isNull();
        }
    }
}
