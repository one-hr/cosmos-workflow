package jp.co.onehr.workflow.service.infra;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.co.onehr.workflow.dao.infra.DBSchemaDAOBuilder;
import jp.co.onehr.workflow.dao.infra.DBSchemaInitializer;
import jp.co.onehr.workflow.dto.base.index.IndexCustomizable;
import jp.co.onehr.workflow.util.InfraUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service class used to initialize Database's table definition and indexes(mainly used in postgres/mongodb)
 *
 * <p>
 *     <ul>
 *         <li>Postgres has to manually initialize table definitions and indexes</li>
 *         <li>MongoDB has to manually initialize indexes. table definitions do not need to manually initialize</li>
 *         <li>CosmosDB do not need to manually initialize neither table definitions nor indexes(all is automatically created)</li>
 *     </ul>
 * </p>
 */
public class DBSchemaService {

    Logger log = LoggerFactory.getLogger(DBSchemaService.class);

    public static DBSchemaService singleton = new DBSchemaService();

    Map<String, DBSchemaInitializer> schemaDAOMap = new HashMap<>();

    DBSchemaService() {
    }


    /**
     * Creates the table/index for the specified partitionName (e.g., "Nodes") if it does not exist. Only applicable for Postgres and MongoDB. Does nothing for CosmosDB.
     *
     * @param host
     * @param partitionName className. e.g. "Node"
     * @return
     * @throws Exception
     */
    public List<String> createSchemaIfNotExist(String host, String partitionName) throws Exception {
        var ret = new ArrayList<String>();

        var dbType = InfraUtil.getDbTypeByHost(host);

        var schemaDAO = schemaDAOMap.computeIfAbsent(dbType, (type) -> new DBSchemaDAOBuilder().withDatabaseType(type).build());

        ret.add(schemaDAO.createTableIfNotExist(host, partitionName));
        log.info("host:{}, The table in the database has been created, table name: {}.", host, partitionName);
        ret.addAll(schemaDAO.createIndexesIfNotExist(host, partitionName));
        log.info("host:{}, The index for the table in the database has been created, table name: {}.", host, partitionName);
        return ret.stream().filter(StringUtils::isNotEmpty).toList();
    }


    /**
     * Creates the customized indexes required for the DTO.
     *
     * @param host      Host name or identifier.
     * @param partition Partition name (e.g., "Definitions").
     * @param classOfT  DTO class (e.g., Definition.class). The class must have a public no-arg constructor.
     * @return List of created index names.
     * @throws Exception If index creation fails.
     */
    public List<String> createCustomIndexIfNotExist(String host, String partition, Class classOfT) throws Exception {
        var dbType = InfraUtil.getDbTypeByHost(host);
        var schemaDAO = schemaDAOMap.computeIfAbsent(dbType, (type) -> new DBSchemaDAOBuilder().withDatabaseType(type).build());

        // If classOfT directly implements IndexCustomizable, create its custom indexes.
        // Note: If the parent implements IndexCustomizable but the child does not override it,　no custom indexes will be created.
        if (IndexCustomizable.class.isAssignableFrom(classOfT)) {
            // Check whether the getCustomIndexDefinitions method is declared in classOfT or in one of its parent classes.
            try {
                // getDeclaredMethod searches only for methods that are declared directly in the class.
                classOfT.getDeclaredMethod("getCustomIndexDefinitions");
            } catch (NoSuchMethodException e) {
                // Skip when the method is not declared directly in the subclass (i.e., not overridden).
                log.debug("domain:{}, getCustomIndexDefinitions method is not overridden in {}, skipping custom index creation.", host, classOfT.getSimpleName());
                return List.of();
            }

            // Create an instance of classOfT. A default public constructor is required.
            var obj = classOfT.getDeclaredConstructor().newInstance();

            if (obj instanceof IndexCustomizable dto) {
                return schemaDAO.createCustomIndexIfNotExist(host, partition, dto);
            }
        }

        return List.of();

    }

}
