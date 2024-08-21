package jp.co.onehr.workflow.dto.param;


import jp.co.onehr.workflow.contract.context.InstanceContext;
import jp.co.onehr.workflow.contract.context.OperatorLogContext;

/**
 * Parameters for moving the instance to the specified node
 */
public class RelocateParam {

    public RelocateParam() {

    }

    /**
     * The ID of the instance that needs to be moved
     */
    public String instanceId = "";

    /**
     * The target node ID for the instance move
     */
    public String relocateNodeId = "";

    /**
     * Context of business data corresponding to a workflow instance.
     */
    public InstanceContext instanceContext;

    /**
     * Context of the operation corresponding to the workflow history.
     */
    public OperatorLogContext logContext;

    /**
     * comment
     */
    public String comment = "";
}
