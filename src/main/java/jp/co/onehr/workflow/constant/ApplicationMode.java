package jp.co.onehr.workflow.constant;

/**
 * Application mode for workflow instances
 */
public enum ApplicationMode {

    /**
     * Allowing the proxy to initiate the workflow on behalf of the applicant.
     */
    PROXY,
    /**
     * The applicant needs to initiate the workflow themselves
     */
    SELF,
}
