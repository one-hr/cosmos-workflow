package jp.co.onehr.workflow.constant;


public enum WorkflowErrors implements ErrorInterface {
    /**
     * exception while registering the database with the workflow engine
     */
    WORKFLOW_ENGINE_REGISTER_INVALID(400),//
    /**
     * The node type does not match the type in the system
     */
    NODE_TYPE_MISMATCH(400);//

    private final int httpStatus;

    WorkflowErrors(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
