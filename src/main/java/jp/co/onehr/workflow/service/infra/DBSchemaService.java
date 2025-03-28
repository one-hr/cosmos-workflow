package jp.co.onehr.workflow.service.infra;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.co.onehr.workflow.dao.infra.DBSchemaDAOBuilder;
import jp.co.onehr.workflow.dao.infra.DBSchemaInitializer;
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
        ret.addAll(schemaDAO.createIndexesIfNotExist(host, partitionName));

        return ret.stream().filter(StringUtils::isNotEmpty).toList();
    }


}
