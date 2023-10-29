package jp.co.onehr.workflow.contract.operator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jp.co.onehr.workflow.contract.context.InstanceContext;
import jp.co.onehr.workflow.contract.context.TestInstanceContext;
import jp.co.onehr.workflow.dto.ApprovalStatus;


public class TestOperatorService implements OperatorService {

    public static final TestOperatorService singleton = new TestOperatorService();

    public static final String SKIP_OPERATOR = "skip_operator";
    public static final String CONTEXT_SKIP_OPERATOR = "context_skip_operator";

    @Override
    public Set<String> handleOperators(Set<String> operatorIds, InstanceContext instanceContext) {
        var result = new HashSet<String>();

        if (operatorIds.contains(CONTEXT_SKIP_OPERATOR)) {
            if (instanceContext instanceof TestInstanceContext testContext) {
                if (!testContext.generatorOperator) {
                    return Set.of();
                }
            }
        }

        if (operatorIds.contains(SKIP_OPERATOR)) {
            operatorIds.remove(SKIP_OPERATOR);
        }

        result.addAll(operatorIds);
        
        return result;
    }

    @Override
    public Set<String> handleOrganizations(Set<String> orgIds, InstanceContext instanceContext) {
        return orgIds;
    }

    @Override
    public Map<String, ApprovalStatus> handleParallelApproval(Set<String> operatorIds, Set<String> orgIds, Set<String> expandOperatorIds, InstanceContext instanceContext) {
        var parallelApprovalMap = new HashMap<String, ApprovalStatus>();

        for (var expandOperatorId : expandOperatorIds) {
            parallelApprovalMap.put(expandOperatorId, new ApprovalStatus(expandOperatorId, false));
        }

        return parallelApprovalMap;
    }

    @Override
    public Map<String, ApprovalStatus> handleRetrieveParallelApproval(Set<String> operatorIds, Set<String> orgIds, Set<String> expandOperatorIds, boolean reset, String operatorId, InstanceContext instanceContext) {
        var parallelApprovalMap = new HashMap<String, ApprovalStatus>();

        // If the retrieval reset is enabled, then each operator of the concurrent approval needs to approve.
        if (reset) {
            for (var expandOperatorId : expandOperatorIds) {
                parallelApprovalMap.put(expandOperatorId, new ApprovalStatus(expandOperatorId, false));
            }
        } else {
            // Other operators are already approved by default, only the operator needs to approve
            for (var expandOperatorId : expandOperatorIds) {
                parallelApprovalMap.put(expandOperatorId, new ApprovalStatus(expandOperatorId, true));
                if (expandOperatorId.equals(operatorId)) {
                    parallelApprovalMap.put(expandOperatorId, new ApprovalStatus(operatorId, false));
                }
            }
        }

        return parallelApprovalMap;
    }

    @Override
    public Map<String, ApprovalStatus> handleModificationParallelApproval(Set<String> operatorIds, Set<String> orgIds, Set<String> expandOperatorIds, Map<String, ApprovalStatus> existParallelApproval, InstanceContext instanceContext) {
        var parallelApprovalMap = new HashMap<String, ApprovalStatus>();

        for (var expandOperatorId : expandOperatorIds) {
            if (existParallelApproval.containsKey(expandOperatorId)) {
                parallelApprovalMap.put(expandOperatorId, existParallelApproval.get(expandOperatorId));
            } else {
                parallelApprovalMap.put(expandOperatorId, new ApprovalStatus(expandOperatorId, false));
            }
        }

        return parallelApprovalMap;
    }


}
