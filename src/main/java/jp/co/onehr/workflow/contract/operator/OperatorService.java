package jp.co.onehr.workflow.contract.operator;

import java.util.Map;
import java.util.Set;

import jp.co.onehr.workflow.contract.context.InstanceContext;
import jp.co.onehr.workflow.dto.ApprovalStatus;

public interface OperatorService {

    /**
     * Custom implementation to operator IDs into expandOperator IDs
     *
     * @param operatorIds
     * @param instanceContext
     * @return
     */
    Set<String> handleOperators(Set<String> operatorIds, InstanceContext instanceContext);

    /**
     * Custom implementation to organization IDs into expandOperator IDs
     *
     * @param orgIds
     * @param instanceContext
     * @return
     */
    Set<String> handleOrganizations(Set<String> orgIds, InstanceContext instanceContext);

    /**
     * Custom approval status for multi-user approvals
     *
     * @param operatorIds
     * @param orgIds
     * @param expandOperatorIds
     * @param instanceContext
     * @return
     */
    Map<String, ApprovalStatus> handleParallelApproval(Set<String> operatorIds, Set<String> orgIds, Set<String> expandOperatorIds, InstanceContext instanceContext);

    /**
     * Custom handling of parallel approval during retrieval
     *
     * @param operatorIds
     * @param orgIds
     * @param expandOperatorIds
     * @param reset
     * @param operatorId
     * @param instanceContext
     * @return
     */
    Map<String, ApprovalStatus> handleRetrieveParallelApproval(Set<String> operatorIds, Set<String> orgIds, Set<String> expandOperatorIds, boolean reset, String operatorId, InstanceContext instanceContext);

    /**
     * Handling of co-approver status when there is a change in operators.
     *
     * @param operatorIds
     * @param orgIds
     * @param expandOperatorIds
     * @param existParallelApproval
     * @param instanceContext
     * @return
     */
    Map<String, ApprovalStatus> handleModificationParallelApproval(Set<String> operatorIds, Set<String> orgIds, Set<String> expandOperatorIds, Map<String, ApprovalStatus> existParallelApproval, InstanceContext instanceContext);
}
