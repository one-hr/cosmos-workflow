package jp.co.onehr.workflow.dao.infra.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import io.github.thunderz99.cosmos.impl.postgres.PostgresDatabaseImpl;
import io.github.thunderz99.cosmos.impl.postgres.PostgresImpl;
import io.github.thunderz99.cosmos.impl.postgres.dto.IndexOption;
import io.github.thunderz99.cosmos.impl.postgres.util.TableUtil;
import jp.co.onehr.workflow.ProcessConfiguration;
import jp.co.onehr.workflow.dao.CosmosDB;
import jp.co.onehr.workflow.dao.infra.DBSchemaInitializer;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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

        var isRecycle = StringUtils.endsWith(partitionName, "_recycle");

        if (isRecycle) {
            // If the recycle partition has a TTL field, register it in pg_cron to enable the automatic deletion job
            var cronExpression = isRecycle ? CRON_EXPRESSION_FOR_RECYCLE : CRON_EXPRESSION_FOR_TTL;
            ret.add(_enableTTLJob(host, partitionName, cronExpression));

            // create the related indexes
            var options = List.of(
                    Pair.of("_expireAt", new Options().unique(false).fieldType("bigint"))
            );
            ret.addAll(_createIndexIfNotExist(host, partitionName, options));
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

    /**
     * create specific indexes for dto classes
     *
     * @param host
     * @param partitionName
     * @param options       "fieldName, Option" pair
     * @return list of partition.fieldName  that created Indexes
     */
    List<String> _createIndexIfNotExist(String host, String partitionName, List<Pair<String, Options>> options) throws Exception {

        if (StringUtils.isEmpty(partitionName)) {
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
                for (var option : options) {
                    var fieldName = option.getLeft();
                    var indexOption = IndexOption.unique(option.getRight().unique);
                    indexOption.fieldType = option.getRight().fieldType;
                    ret.add(TableUtil.createIndexIfNotExists(conn, schemaName, partition, fieldName, indexOption));
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

    /**
     * Index options when creating indexes for mongodb
     */
    public static class Options {
        public boolean unique = false;

        /**
         * fieldType for this index. default is text. other valid value is bigint / numeric / float8 / etc
         *
         * <p>
         * example: CREATE INDEX idx_MailRecords__expireAt_1
         * ON "Data_xxx"."MailRecords" (((data->>'_expireAt')::bigint));
         * </p>
         */
        public String fieldType = "text";

        public Options() {
        }

        public Options unique(boolean unique) {
            this.unique = unique;
            return this;
        }

        /**
         * set the field type. default is "text"
         *
         * @param fieldType
         * @return Options
         */
        public Options fieldType(String fieldType) {
            this.fieldType = fieldType;
            return this;
        }
    }

}
