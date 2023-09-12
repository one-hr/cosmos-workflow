package jp.co.onehr.workflow.contract.context;

import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.param.ContextParam;

/**
 * Handling of Business Data Parameters
 */
public interface ContextParamService {

    /**
     * Generating Context Parameters Based on Instances During Batch Processing
     *
     * @param contextParam
     * @param definition
     * @param instance
     * @param operatorId
     */
    void generateContextParam4Bulk(ContextParam contextParam, Definition definition, Instance instance, String operatorId);
}
