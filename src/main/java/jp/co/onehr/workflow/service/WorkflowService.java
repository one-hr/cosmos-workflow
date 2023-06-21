package jp.co.onehr.workflow.service;

import jp.co.onehr.workflow.dto.Workflow;
import jp.co.onehr.workflow.service.base.BaseCRUDService;


public class WorkflowService extends BaseCRUDService<Workflow> {

    public static final WorkflowService singleton = new WorkflowService();

    WorkflowService() {
        super(Workflow.class);
    }
}
