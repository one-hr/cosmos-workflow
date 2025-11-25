package jp.co.onehr.workflow.dao.infra.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import io.github.thunderz99.cosmos.impl.postgres.PostgresDatabaseImpl;
import io.github.thunderz99.cosmos.impl.postgres.PostgresImpl;
import io.github.thunderz99.cosmos.impl.postgres.dto.IndexOption;
import io.github.thunderz99.cosmos.impl.postgres.dto.PGFieldType;
import io.github.thunderz99.cosmos.impl.postgres.dto.PGIndexField;
import io.github.thunderz99.cosmos.impl.postgres.util.TableUtil;
import jp.co.onehr.workflow.ProcessConfiguration;
import jp.co.onehr.workflow.dao.CosmosDB;
import jp.co.onehr.workflow.dao.infra.DBSchemaInitializer;
import jp.co.onehr.workflow.dto.base.index.IndexCustomizable;
import jp.co.onehr.workflow.dto.base.index.IndexDefinition;
import jp.co.onehr.workflow.dto.base.index.IndexField;
import jp.co.onehr.workflow.dto.base.index.IndexFieldType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static jp.co.onehr.workflow.service.base.BaseNoSqlService.judgeEnableDefaultWorkflowDB;

/**
 * Implementation of DBSchemaInitializer interface for Postgres.
 *
 * <p>
 * Postgres has to initialize table definitions in code
 * </p>
 */
public class PostgresSchemaDAO implements DBSchemaInitializer {

    static final Logger log = LoggerFactory.getLogger(PostgresSchemaDAO.class);

    /**
     * Cron definition for a table with a regular TTL (runs periodically to delete expired data every minute)
     */
    static final String CRON_EXPRESSION_FOR_TTL = "*/1 * * * *";

    /**
     * Cron definition for the recycle table (runs daily at 6 AM JST to delete expired data)
     */
    static final String CRON_EXPRESSION_FOR_RECYCLE = "0 21 * * *";


    @Override
    public String createTableIfNotExist(String host, String partitionName) throws Exception {

        var dataSource = getDataSource(host);
        var schemaName = ProcessConfiguration.getConfiguration().getCollectionName(host);

        try (var conn = dataSource.getConnection()) {
            return TableUtil.createTableIfNotExists(conn, schemaName, partitionName);
        }
    }

    @Override
    public List<String> createIndexesIfNotExist(String host, String partitionName) throws Exception {
        var ret = new ArrayList<String>();

        var isRecycle = Strings.CS.endsWith(partitionName, "_recycle");

        if (isRecycle) {
            // If the recycle partition has a TTL field, register it in pg_cron to enable the automatic deletion job
            var cronExpression = isRecycle ? CRON_EXPRESSION_FOR_RECYCLE : CRON_EXPRESSION_FOR_TTL;
            ret.add(_enableTTLJob(host, partitionName, cronExpression));

            // create the related indexes
            var indexDefinitions = List.of(
                    IndexDefinition.of(IndexField.of("_expireAt", IndexFieldType.BIGINT))
            );

            ret.addAll(_createIndexIfNotExist(host, partitionName, indexDefinitions));
        }
        return ret;
    }

    /**
     * Enable the TTL feature for the specified partitionName
     *
     * @param host
     * @param partitionName
     * @return
     * @throws Exception
     */
    static String _enableTTLJob(String host, String partitionName, String cronExpression) throws Exception {
        var db = (PostgresDatabaseImpl) ProcessConfiguration.getConfiguration().getDatabase(host);
        var schemaName = ProcessConfiguration.getConfiguration().getCollectionName(host);
        return db.enableTTL(schemaName, partitionName, cronExpression);
    }


    @Override
    public List<String> createCustomIndexIfNotExist(String host, String partitionName, IndexCustomizable dto) throws Exception {
        var ret = new ArrayList<String>();

        // Create the customized indexes defined in the DTO class (e.g., Instance).
        if (dto != null) {
            var indexDefinitions = dto.getCustomIndexDefinitions();
            ret.addAll(_createIndexIfNotExist(host, partitionName, indexDefinitions));
        }

        return ret;
    }

    /**
     * create specific indexes for dto classes
     *
     * @param host
     * @param partitionName
     * @param indexDefinitions index definitions, including fieldName, fieldType, uniqueness
     * @return list of partition.fieldName  that created Indexes
     */
    List<String> _createIndexIfNotExist(String host, String partitionName, List<IndexDefinition> indexDefinitions) throws Exception {

        if (StringUtils.isEmpty(partitionName)) {
            return List.of();
        }

        if (CollectionUtils.isEmpty(indexDefinitions)) {
            return List.of();
        }

        var schemaName = ProcessConfiguration.getConfiguration().getCollectionName(host);

        var db = (PostgresDatabaseImpl) ProcessConfiguration.getConfiguration().getDatabase(host);
        var postgresAccount = (PostgresImpl) db.getCosmosAccount();
        var dataSource = postgresAccount.getDataSource();

        var ret = new ArrayList<String>();

        try (var conn = dataSource.getConnection()) {

            var partition = partitionName;
            // Create the index only if the table exists
            if (TableUtil.tableExist(conn, schemaName, partition)) {
                for (var indexDef : indexDefinitions) {
                    var indexFields = indexDef.fields.stream().map(f -> PGIndexField.of(f.fieldName, PGFieldType.valueOf(f.type.name()))).collect(Collectors.toList());
                    var indexOption = IndexOption.unique(indexDef.unique);
                    ret.add(TableUtil.createIndexIfNotExist4MultiFields(conn, schemaName, partition, indexFields, indexOption));
                }
            }

        }

        return ret.stream().filter(StringUtils::isNotEmpty).collect(Collectors.toList());
    }

    /**
     * Get the native postgres data source to do operations for postgres
     *
     * @param host
     * @return data source from connection pool
     * @throws Exception
     */
    static DataSource getDataSource(String host) throws Exception {
        var db = ProcessConfiguration.getConfiguration().getDatabase(host);

        if (ObjectUtils.isEmpty(db) && judgeEnableDefaultWorkflowDB()) {
            db = CosmosDB.registerDefaultWorkflowDB(host);
        }

        // get the native postgres dataSource to do db operations
        return ((PostgresImpl) db.getCosmosAccount()).getDataSource();
    }

}
