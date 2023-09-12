package jp.co.onehr.workflow.dto.param;


import jp.co.onehr.workflow.contract.context.InstanceContext;
import jp.co.onehr.workflow.contract.context.OperatorLogContext;

/**
 * Rebind the parameters of the instance to the definition
 */
public class RebindingParam {

    /**
     * version number of the definition for instance re-binding
     */
    public Integer definitionVersion;

    /**
     * comment
     */
    public String comment = "";

    /**
     * Context of business data corresponding to a workflow instance.
     */
    public InstanceContext instanceContext;

    /**
     * Context of the operation corresponding to the workflow history.
     */
    public OperatorLogContext logContext;


}
