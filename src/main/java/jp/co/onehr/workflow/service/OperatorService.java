package jp.co.onehr.workflow.service;

import java.util.Map;
import java.util.Set;

import jp.co.onehr.workflow.dto.ApprovalStatus;

public interface OperatorService {

    /**
     * Custom implementation to operator IDs into expandOperator IDs
     *
     * @param operatorIds
     * @return
     */
    Set<String> handleOperators(Set<String> operatorIds);

    /**
     * Custom implementation to organization IDs into expandOperator IDs
     *
     * @param orgIds
     * @return
     */
    Set<String> handleOrganizations(Set<String> orgIds);

    /**
     * Custom approval status for multi-user approvals
     *
     * @param operatorIds
     * @param orgIds
     * @param expandOperatorId
     * @return
     */
    Map<String, ApprovalStatus> handleParallelApproval(Set<String> operatorIds, Set<String> orgIds, Set<String> expandOperatorId);
}
