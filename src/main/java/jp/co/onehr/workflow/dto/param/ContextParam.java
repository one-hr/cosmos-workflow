package jp.co.onehr.workflow.dto.param;

import jp.co.onehr.workflow.contract.context.InstanceContext;
import jp.co.onehr.workflow.contract.context.OperatorLogContext;
import org.apache.commons.lang3.ObjectUtils;

public class ContextParam {

    /**
     * Context of business data corresponding to a workflow instance.
     */
    public InstanceContext instanceContext;

    /**
     * Context of the operation corresponding to the workflow history.
     */
    public OperatorLogContext logContext;

    public String comment = "";

    public ContextParam() {

    }

    public void formRebindingParam(RebindingParam rebindingParam) {
        if (ObjectUtils.isNotEmpty(rebindingParam)) {
            this.instanceContext = rebindingParam.instanceContext;
            this.logContext = rebindingParam.logContext;
            this.comment = rebindingParam.comment;
        }
    }
}
