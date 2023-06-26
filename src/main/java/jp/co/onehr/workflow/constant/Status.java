package jp.co.onehr.workflow.constant;

/**
 * Status of the workflow instance
 */
public enum Status {

    /**
     * The workflow instance is currently running
     */
    PROCESSING,
    /**
     * The workflow instance has already ended
     */
    Finished;
}
