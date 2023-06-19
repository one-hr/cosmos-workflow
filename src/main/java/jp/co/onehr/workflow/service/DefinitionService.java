package jp.co.onehr.workflow.service;

import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.service.base.BaseCRUDService;

public class DefinitionService extends BaseCRUDService<Definition> {

    public static final DefinitionService singleton = new DefinitionService();

    DefinitionService() {
        super(Definition.class);
    }

}
