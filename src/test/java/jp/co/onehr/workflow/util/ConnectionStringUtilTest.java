package jp.co.onehr.workflow.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ConnectionStringUtilTest {

    @Test
    void getDbTypeByConnectionString_should_work() {

        // normal
        {
            assertThat(ConnectionStringUtil.getDbTypeByConnectionString("AccountEndpoint=https://sc-dev.documents.azure.com:443/;AccountKey=xxx==;"))
                    .isEqualTo("cosmosdb");

            assertThat(ConnectionStringUtil.getDbTypeByConnectionString("mongodb+srv://xxx:yyy@localhost:27017/?replicaSet=rs0"))
                    .isEqualTo("mongodb");
            assertThat(ConnectionStringUtil.getDbTypeByConnectionString("mongodb://xxx:yyy@op-db1:20402,op-db2:20402,op-db3:20402/?replicaSet=rs0"))
                    .isEqualTo("mongodb");

            assertThat(ConnectionStringUtil.getDbTypeByConnectionString("postgresql://c-pg-cluster-onehr-test-0003.postgres.cosmos.azure.com:5432/citus?user=citus&password=xxx&sslmode=require"))
                    .isEqualTo("postgres");
            assertThat(ConnectionStringUtil.getDbTypeByConnectionString("jdbc:postgresql://localhost:5432/postgres?user=postgres&password=postgres"))
                    .isEqualTo("postgres");
        }

        // irregular
        {
            // null
            assertThatThrownBy(() -> ConnectionStringUtil.getDbTypeByConnectionString(null))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("connectionString is empty:");
        }

        {
            // empty
            assertThatThrownBy(() -> ConnectionStringUtil.getDbTypeByConnectionString(""))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("connectionString is empty:");
        }
    }
}
