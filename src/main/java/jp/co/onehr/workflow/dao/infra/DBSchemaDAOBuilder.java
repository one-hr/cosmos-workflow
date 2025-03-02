package jp.co.onehr.workflow.dao.infra;

import io.github.thunderz99.cosmos.util.Checker;
import jp.co.onehr.workflow.dao.infra.impl.CosmosSchemaDAO;
import jp.co.onehr.workflow.dao.infra.impl.MongoSchemaDAO;
import jp.co.onehr.workflow.dao.infra.impl.PostgresSchemaDAO;
import org.apache.commons.lang3.StringUtils;

import static io.github.thunderz99.cosmos.CosmosBuilder.*;

/**
 * Builder class used to build the instance representing a DBSchemaInitializer instance.
 * <p>
 *     This class is used to build the instance of DBSchemaInitializer.
 *     DBSchemaInitializer is used to initialize Database's table schemas and indexes(mainly used for postgres/mongodb)
 * </p>
 */
public class DBSchemaDAOBuilder {


    String dbType = COSMOSDB;

    /**
     * Specify the dbType( "cosmosdb" or "mongodb" or "postgres")
     *
     * @param dbType
     * @return cosmosBuilder
     */
    public DBSchemaDAOBuilder withDatabaseType(String dbType) {
        this.dbType = dbType;
        return this;
    }


    /**
     * Build the instance representing a DBSchemaDAO instance.
     *
     * @return DBSchemaDAO instance
     */
    public DBSchemaInitializer build() {
        Checker.checkNotBlank(dbType, "dbType");

        if (StringUtils.equals(dbType, COSMOSDB)) {
            return new CosmosSchemaDAO();
        }

        if (StringUtils.equals(dbType, MONGODB)) {
            return new MongoSchemaDAO();
        }

        if (StringUtils.equals(dbType, POSTGRES)) {
            return new PostgresSchemaDAO();
        }

        throw new IllegalArgumentException("Not supported dbType: " + dbType);

    }

}

