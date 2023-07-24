package jp.co.onehr.workflow.contract.operator;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jp.co.onehr.workflow.dto.ApprovalStatus;


public class TestOperatorService implements OperatorService {

    public static final TestOperatorService singleton = new TestOperatorService();

    public static final String SKIP_OPERATOR = "skip_operator";

    @Override
    public Set<String> handleOperators(Set<String> operatorIds) {
        if (operatorIds.contains(SKIP_OPERATOR)) {
            operatorIds.remove(SKIP_OPERATOR);
        }
        return operatorIds;
    }

    @Override
    public Set<String> handleOrganizations(Set<String> orgIds) {
        return orgIds;
    }

    @Override
    public Map<String, ApprovalStatus> handleParallelApproval(Set<String> operatorIds, Set<String> orgIds, Set<String> expandOperatorIds) {
        var parallelApprovalMap = new HashMap<String, ApprovalStatus>();

        for (var expandOperatorId : expandOperatorIds) {
            parallelApprovalMap.put(expandOperatorId, new ApprovalStatus(expandOperatorId, false));
        }

        return parallelApprovalMap;
    }

}
