package jp.co.onehr.workflow.constant;

/**
 * Status of the workflow instance
 */
public enum Status {
    NEW,

    /**
     * The workflow instance is currently running
     */
    PROCESSING,

    /**
     * Rejected by approver
     */
    REJECTED,

    /**
     * Canceled by applicant
     */
    CANCELED,

    /**
     * Approved by approver
     */
    APPROVED,

    /**
     * The workflow instance has already ended
     */
    FINISHED;
}
