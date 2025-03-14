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
     * The workflow id is invalid.
     */
    WORKFLOW_ID_INVALID(400),//
    /**
     * The workflow name is invalid.
     */
    WORKFLOW_NANE_INVALID(400),//
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
    DEFINITION_NOT_EXIST(404),
    /**
     * The extended parameters of the action are invalid
     */
    ACTION_EXTEND_PARAM_INVALID(400),
    /**
     * The operator of the instance is invalid
     */
    INSTANCE_OPERATOR_INVALID(400),
    /**
     * Workflow instance not found
     */
    INSTANCE_NOT_EXIST(404),
    /**
     * instance invalid
     */
    INSTANCE_INVALID(404),
    /**
     * The movement action used by the node is invalid
     */
    NODE_ACTION_INVALID(400),
    /**
     * The back action's node is invalid.
     */
    BACK_NODE_INVALID(400),
    /**
     * The plugin's handle method is invalid
     */
    PLUGIN_HANDLE_INVALID(400),
    /**
     * The name of a node cannot be empty.
     */
    NODE_NAME_INVALID(400),
    /**
     * The node type in the workflow is invalid.
     */
    NODE_TYPE_INVALID(400),
    /**
     * unable to find the node ID in the definition
     */
    NODE_ID_INVALID(400),
    /**
     * The node configuration is invalid.
     */
    NODE_SETTING_INVALID(400),
    /**
     * The operator of the node is invalid.
     */
    NODE_OPERATOR_INVALID(400),
    /**
     * The node type does not match the type in the system
     */
    NODE_TYPE_MISMATCH(400),
    /**
     * Relocate param cannot be null
     */
    RELOCATE_PARAM_NOT_EXIST(400),
    /**
     * The ID of the instance to be relocated does not exist
     */
    RELOCATE_INSTANCE_ID_NOT_EXIST(400),
    /**
     * The ID of the node to be relocated does not exist
     */
    RELOCATE_NODE_ID_NOT_EXIST(400),
    /**
     * Instance that has been approved cannot be relocated
     */
    RELOCATE_INSTANCE_STATUS_INVALID(400),
    /**
     * The instance and definition ID for relocation do not match
     */
    RELOCATE_DEFINITION_MISMATCH(400);

    private final int httpStatus;

    WorkflowErrors(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
