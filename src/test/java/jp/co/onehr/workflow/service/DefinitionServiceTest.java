package jp.co.onehr.workflow.service;

import jp.co.onehr.workflow.base.BaseTest;
import jp.co.onehr.workflow.dto.Definition;
import org.junit.jupiter.api.Test;


public class DefinitionServiceTest extends BaseTest {

    @Test
    void create_should_work() throws Exception {
        var definition = new Definition();
        definition.name = "test_create";
        DefinitionService.singleton.create(host, definition);
    }
}
