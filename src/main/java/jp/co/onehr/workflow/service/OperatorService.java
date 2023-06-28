package jp.co.onehr.workflow.service;

import java.util.Map;
import java.util.Set;

import jp.co.onehr.workflow.dto.ApprovalStatus;

public interface OperatorService {

    /**
     * @param operatorIds
     * @return
     */
    Set<String> handleOperators(Set<String> operatorIds);

    /**
     * @param orgIds
     * @return
     */
    Set<String> handleOrganizations(Set<String> orgIds);

    Map<String, ApprovalStatus> handleParallelApproval(Set<String> operatorIds, Set<String> orgIds, Set<String> expandOperatorId);
}
