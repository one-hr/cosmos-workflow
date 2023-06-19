package jp.co.onehr.workflow.dao;

import io.github.thunderz99.cosmos.Cosmos;
import jp.co.onehr.workflow.dto.base.UniqueKeyCapable;
import jp.co.onehr.workflow.service.base.BaseCRUDService;
import jp.co.onehr.workflow.util.EnvUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static jp.co.onehr.workflow.dao.CosmosDB.*;
import static jp.co.onehr.workflow.service.base.BaseNoSqlService.DEFAULT_COLLECTION;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Utility class
 * suitable for unit testing only
 * used for executing container initialization.
 */
public class ContainerUtil {

    public static final String FW_TEST_COLLECTION_RECREATE = "FW_TEST_COLLECTION_RECREATE";

    public static Logger staticLogger = LoggerFactory.getLogger(ContainerUtil.class);

    public static final String host = "localhost";
    public static boolean judgeAndRecreated;

    static {
        try {
            // Determining and handling whether to delete and recreate a collection during CI/CD.
            judgeAndRecreateCollection();
            judgeAndRecreated = true;
        } catch (Exception e) {
            staticLogger.error("judgeAndRecreated failed", e);
            // Aborting the test early in case of initialization failure of the CosmosDB container.
            System.exit(-1);
        }
    }

    /**
     * Decision and initialization of the data collection.
     * If it is recreated, return true
     *
     * @throws Exception
     */
    static boolean judgeAndRecreateCollection() throws Exception {

        if (!EnvUtil.getBooleanOrDefault(FW_TEST_COLLECTION_RECREATE, false)) {
            // If environment variable is not set, do not recreate
            staticLogger.info("ignore recreate data collection. FW_TEST_COLLECTION_RECREATE:{}", false);
            return false;
        }

        String connectionString = EnvUtil.get(FW_WORKFLOW_CONNECTION_STRING);
        String dbName = EnvUtil.getOrDefault(FW_WORKFLOW_DATABASE_NAME, DEFAULT_DATABASE_NAME);

        var cosmosAccount = new Cosmos(connectionString);

        // If it is not local or CI/CD environment, do not recreate
        if (!judgeValidTestAccount(cosmosAccount.getAccount())) {
            staticLogger.info("ignore recreate data collection. cosmosAccount:{}", cosmosAccount.getAccount());
            return false;
        }

        var collectionName = BaseCRUDService.getCollectionNameByEnv(DEFAULT_COLLECTION);

        staticLogger.info("begin recreate data collection");
        // Delete collection
        cosmosAccount.deleteCollection(dbName, collectionName);
        // Recreate collection
        var uniqueKeyPolicy = Cosmos.getUniqueKeyPolicy(Set.of("/" + UniqueKeyCapable.UNIQUE_KEY_1, "/" + UniqueKeyCapable.UNIQUE_KEY_2, "/" + UniqueKeyCapable.UNIQUE_KEY_3));
        cosmosAccount.createIfNotExist(dbName, collectionName, uniqueKeyPolicy);

        var collection = cosmosAccount.readCollection(dbName, collectionName);
        assertThat(collection.getUniqueKeyPolicy().getUniqueKeys()).hasSize(3);

        staticLogger.info("end recreate data collection");

        return true;
    }

    /**
     * Determining whether the account should be recreated or not
     *
     * @param accountName
     * @return
     */
    static boolean judgeValidTestAccount(String accountName) {
        // local cosmos emulator accountName is localhost
        return accountName.contains("local") || accountName.contains("ci-cd") || accountName.contains("localhost");
    }

}