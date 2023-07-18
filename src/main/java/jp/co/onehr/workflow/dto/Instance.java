package jp.co.onehr.workflow.dto;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import jp.co.onehr.workflow.constant.Action;
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
     * The ID of the previous manual node.
     */
    public String preNodeId = "";

    /**
     * IDs of all operable operators for the current node of the instance
     */
    public Set<String> operatorIdSet = Sets.newHashSet();

    /**
     * IDs of all operable organizations for the current node of the instance
     */
    public Set<String> operatorOrgIdSet = Sets.newHashSet();

    /**
     * Expand the operation organization into operators and merge it with all operator IDs
     */
    public Set<String> expandOperatorIdSet = Sets.newHashSet();

    /**
     * The collection of operator IDs from the previous manual node
     */
    public Set<String> preExpandOperatorIdSet = Sets.newHashSet();

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

    /**
     * When the approval type is "AND," record whether all required operators have approved
     */
    public Map<String, ApprovalStatus> parallelApproval = Maps.newHashMap();

    /**
     * The available actions that can be used in a workflow instance
     */
    public Set<Action> allowingActions = Sets.newHashSet();

    public List<OperateLog> operateLogList = Lists.newArrayList();

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
