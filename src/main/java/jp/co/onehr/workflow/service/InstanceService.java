package jp.co.onehr.workflow.service;

import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.service.base.BaseCRUDService;


public class InstanceService extends BaseCRUDService<Instance> {

    public static final InstanceService singleton = new InstanceService();

    InstanceService() {
        super(Instance.class);
    }

}
