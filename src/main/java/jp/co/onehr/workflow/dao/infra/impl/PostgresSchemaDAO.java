package jp.co.onehr.workflow.dao.infra.impl;

import io.github.thunderz99.cosmos.impl.postgres.PostgresImpl;
import io.github.thunderz99.cosmos.impl.postgres.util.TableUtil;
import jp.co.onehr.workflow.ProcessConfiguration;
import jp.co.onehr.workflow.dao.CosmosDB;
import jp.co.onehr.workflow.dao.infra.DBSchemaInitializer;
import org.apache.commons.lang3.ObjectUtils;
import org.atteo.evo.inflector.English;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

import java.util.*;

import static jp.co.onehr.workflow.service.base.BaseNoSqlService.judgeEnableDefaultWorkflowDB;

/**
 * Implementation of DBSchemaInitializer interface for Postgres.
 *
 * <p>
 *     Postgres has to initialize table definitions in code
 * </p>
 */
public class PostgresSchemaDAO implements DBSchemaInitializer {

    static final Logger log = LoggerFactory.getLogger(PostgresSchemaDAO.class);

    @Override
    public String createTableIfNotExist(String host, String partitionName) throws Exception {

        var dataSource = getDataSource(host);
        var schemaName = ProcessConfiguration.getConfiguration().getCollectionName(host);

        try(var conn = dataSource.getConnection()){
            return TableUtil.createTableIfNotExists(conn, schemaName, partitionName);
        }
    }

    @Override
    public List<String> createIndexesIfNotExist(String host, String partitionName) throws Exception {
        // Currently we do not need to create indexes for postgres.
        // Because when creating table, we already created indexes for id(text) and data(jsonb).
        // In the future, if uniqueKey1~3 's index is needed, we should create these indexes here.
        return List.of();
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
