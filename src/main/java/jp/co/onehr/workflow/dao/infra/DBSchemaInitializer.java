package jp.co.onehr.workflow.dao.infra;

import java.util.List;

import jp.co.onehr.workflow.dto.base.index.IndexCustomizable;

/**
 * Interface used to initialize Database's table schemas and indexes(mainly used for postgres/mongodb)
 *
 * <p>
 *     <ul>
 *       <li>postgres: partition(=postgres table) should be created. and every partition should create index for specific fields</li>
 *       <li>mongodb: partition(=mongo collection) are not needed to create in code. but every partition should create index for specific fields</li>
 *       <li>cosmosdb: partition(=cosmos partition) are not needed to create in code. and every partition does not need indexes creation in code(cosmosdb created it automatically)</li>
 *     </ul>
 * </p>
 */
public interface DBSchemaInitializer {


    /**
     * Creates a table for the specified partitionName (e.g., "Nodes"). Does nothing if it already exists. Only applicable for Postgres. Does nothing for CosmosDB and MongoDB.
     *
     * @param host
     * @param partitionName
     * @return created table name
     * @throws Exception
     */
    default public String createTableIfNotExist(String host, String partitionName) throws Exception {
        return "";
    }


    /**
     * Creates indexes for the specified partitionName (e.g., "Nodes"). Does nothing if they already exist. Only applicable for Postgres and MongoDB. Does nothing for CosmosDB.
     *
     * @param host
     * @param partitionName
     * @return created index names
     * @throws Exception
     */
    default public List<String> createIndexesIfNotExist(String host, String partitionName) throws Exception {
        return List.of();
    }


    /**
     * Create custom indexes for the specified partitionName (e.g., "Definitions").
     * If the indexes already exist, no action is taken.
     * Currently supported only for postgres.
     * For CosmosDB or MongoDB, this operation is skipped.
     *
     * @param host
     * @param partitionName
     * @param dto           DTO instance
     *
     *                      <p>
     *                      classOfT refers to the DTO class.
     *                      If the DTO implements IndexCustomizable, create custom indexes specific to that DTO.
     *                      </p>
     * @return a list of created index names
     * @throws Exception
     */
    default public List<String> createCustomIndexIfNotExist(String host, String partitionName, IndexCustomizable dto) throws Exception {
        return List.of();
    }
}

