package jp.co.onehr.workflow.dao;

import jp.co.onehr.workflow.base.BaseTest;
import jp.co.onehr.workflow.service.base.BaseNoSqlService;
import jp.co.onehr.workflow.util.InfraUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.atteo.evo.inflector.English;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IndexDAOTest extends BaseTest {

    @Test
    void createIfNotExistBasicIndexes_should_work() throws Exception {

        if(!InfraUtil.isMongoDB()){
            return;
        }
        var host = "localhost";
        var className = "Index" + RandomStringUtils.randomAlphanumeric(6) + "Test";

        var partition = BaseNoSqlService.addSuffixToPartition(English.plural(className));

        try {
            var results = IndexDAO.singleton.createIfNotExistBasicIndexes(host, List.of(className));

            assertThat(results).hasSize(2);
            assertThat(results.get(0)).isEqualTo(partition + "." + "id");
            assertThat(results.get(1)).isEqualTo(partition + "." + "_ts");

        } finally {
            IndexDAO.singleton.deleteIndex(host, partition, "id");
            IndexDAO.singleton.deleteIndex(host, partition, "_ts");
        }
    }
}