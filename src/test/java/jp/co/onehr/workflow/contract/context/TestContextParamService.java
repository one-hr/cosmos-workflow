package jp.co.onehr.workflow.contract.context;

import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.param.ContextParam;


public class TestContextParamService implements ContextParamService {

    public static final TestContextParamService singleton = new TestContextParamService();

    @Override
    public void generateContextParam4Bulk(ContextParam contextParam, Definition definition, Instance instance, String operatorId) {
        contextParam.comment = "bulk:" + operatorId;
    }
}
