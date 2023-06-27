package jp.co.onehr.workflow.dto;

import java.util.Set;

import com.google.common.collect.Sets;
import jp.co.onehr.workflow.constant.ApplicationMode;
import jp.co.onehr.workflow.constant.Status;
import jp.co.onehr.workflow.dto.base.BaseData;
import jp.co.onehr.workflow.dto.param.ApplicationParam;

/**
 * Workflow instance
 * <p>
 * Record of the running workflow initiated by the applicant
 */
public class Instance extends BaseData {

    /**
     * ID of the Workflow for the Running Instance
     */
    public String workflowId = "";

    /**
     * ID of the Workflow Definition for the Running Instance
     */
    public String definitionId = "";

    /**
     * ID of the Current Node in the Instance
     */
    public String nodeId = "";

    /**
     * IDs of all operable operators for the current node of the instance
     */
    public Set<String> operatorIdSet = Sets.newHashSet();

    /**
     * IDs of all operable organizations for the current node of the instance
     */
    public Set<String> operatorOrgIdSet = Sets.newHashSet();

    /**
     * Application mode of the instance, whether it is a proxy application
     */
    public ApplicationMode applicationMode;

    /**
     * Applicant's ID
     */
    public String applicant = "";

    /**
     * Proxy Applicant's ID
     */
    public String proxyApplicant = "";

    /**
     * Current status of the instance
     */
    public Status status;

    public Instance() {

    }

    public Instance(String workflowId, String definitionId) {
        this.workflowId = workflowId;
        this.definitionId = definitionId;
    }

    /**
     * Set the content of the application instance based on the application parameters
     *
     * @param param
     */
    public void setApplicationInfo(ApplicationParam param) {
        this.applicationMode = param.applicationMode;
        this.applicant = param.applicant;
        this.proxyApplicant = param.proxyApplicant;
    }
}