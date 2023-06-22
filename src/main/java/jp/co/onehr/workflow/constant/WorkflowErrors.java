package jp.co.onehr.workflow.constant;


public enum WorkflowErrors implements ErrorInterface {
    /**
     * exception while registering the database with the workflow engine
     */
    WORKFLOW_ENGINE_REGISTER_INVALID(400),//
    /**
     * The workflow does not exist in the database
     */
    WORKFLOW_NOT_EXIST(400),//
    /**
     * The definition should have at least one start node and one end node.
     */
    DEFINITION_NODE_SIZE_INVALID(400),
    /**
     * The first node of the definition is not a start node.
     */
    DEFINITION_FIRST_NODE_INVALID(400),
    /**
     * The last node of the definition is not an end node.
     */
    DEFINITION_END_NODE_INVALID(400),
    /**
     * Workflow definition not found
     */
    DEFINITION_NOT_EXIST(400),
    /**
     * The name of a node cannot be empty.
     */
    NODE_NAME_INVALID(400),
    /**
     * The node type in the workflow is invalid.
     */
    NODE_TYPE_INVALID(400),
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
