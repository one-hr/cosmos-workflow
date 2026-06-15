package jp.co.onehr.workflow.service.base;

import java.util.List;

import io.github.thunderz99.cosmos.impl.postgres.PostgresImpl;
import io.github.thunderz99.cosmos.impl.postgres.util.TableUtil;
import jp.co.onehr.workflow.ProcessConfiguration;
import jp.co.onehr.workflow.dao.CosmosDB;
import jp.co.onehr.workflow.dto.base.SimpleData;
import jp.co.onehr.workflow.dto.base.index.IndexCustomizable;
import jp.co.onehr.workflow.dto.base.index.IndexDefinition;
import jp.co.onehr.workflow.dto.base.index.IndexField;
import jp.co.onehr.workflow.dto.base.index.IndexFieldType;
import jp.co.onehr.workflow.util.InfraUtil;
import jp.co.onehr.workflow.util.PGTableTestUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import static org.assertj.core.api.Assertions.assertThat;

class BaseNoSqlServiceGinIndexTest {

    private static final String HOST = "localhost";
    private static final String PARTITION_NAME = "GinIndexSampleEntities";

    @Test
    @EnabledIf("isPostgres")
    void getColl_should_create_custom_gin_index() throws Exception {
        var service = new GinIndexSampleService();
        var schemaName = ProcessConfiguration.getConfiguration().getCollectionName(HOST);
        var db = ProcessConfiguration.getConfiguration().getDatabase(HOST);
        var postgresAccount = (PostgresImpl) db.getCosmosAccount();
        var partitionName = service.getPartition();

        try {
            service.getColl(HOST);

            try (var conn = postgresAccount.getDataSource().getConnection()) {
                var indexes = PGTableTestUtil.findIndexes(conn, schemaName, partitionName);
                var indexName = "idx_%s_targetIdList_1".formatted(partitionName);

                assertThat(indexes).containsKey(indexName);
                assertThat(indexes.get(indexName))
                        .containsIgnoringCase("USING GIN")
                        .contains("data -> 'targetIdList'::text")
                        .doesNotContain("UNIQUE INDEX");
            }
        } finally {
            try (var conn = postgresAccount.getDataSource().getConnection()) {
                TableUtil.dropTableIfExists(conn, schemaName, partitionName);
                TableUtil.dropTableIfExists(conn, schemaName, partitionName + "_recycle");
            }
        }
    }

    static boolean isPostgres() {
        var db = ProcessConfiguration.getConfiguration().getDatabase(HOST);
        if (ObjectUtils.isEmpty(db)) {
            db = CosmosDB.registerDefaultWorkflowDB(HOST);
        }
        return InfraUtil.isPostgres(db);
    }

    private static class GinIndexSampleService extends BaseNoSqlService<GinIndexSampleEntity> {

        private GinIndexSampleService() {
            super(GinIndexSampleEntity.class, PARTITION_NAME);
        }
    }

    public static class GinIndexSampleEntity extends SimpleData implements IndexCustomizable {

        @Override
        public List<IndexDefinition> getCustomIndexDefinitions() {
            return List.of(IndexDefinition.ofGin(IndexField.of("targetIdList", IndexFieldType.JSONB)));
        }
    }
}
