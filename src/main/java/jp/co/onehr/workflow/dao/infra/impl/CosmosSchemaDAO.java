package jp.co.onehr.workflow.dao.infra.impl;

import jp.co.onehr.workflow.dao.infra.DBSchemaInitializer;

/**
 * Implementation of DBSchemaDAO interface for CosmosDB.
 *
 * <p>
 *     Because CosmosDB creates collections automatically when data is inserted,
 *     this class does not actually create any table or indexes.
 * </p>
 */
public class CosmosSchemaDAO implements DBSchemaInitializer {
}
