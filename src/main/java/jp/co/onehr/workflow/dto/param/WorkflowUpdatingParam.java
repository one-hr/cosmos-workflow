package jp.co.onehr.workflow.dto.param;

import jp.co.onehr.workflow.dto.base.SimpleData;

/**
 * The parameters required to update a workflow
 */
public class WorkflowUpdatingParam extends SimpleData {

    /**
     * Workflow ID
     */
    public String id;

    /**
     * The name of the workflow.
     */
    public String name;

    /**
     * Whether to enable workflow version control.
     */
    public Boolean enableVersion;
}
