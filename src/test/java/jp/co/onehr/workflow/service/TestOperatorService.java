package jp.co.onehr.workflow.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jp.co.onehr.workflow.constant.Action;
import jp.co.onehr.workflow.constant.NodeType;
import jp.co.onehr.workflow.dto.ApprovalStatus;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;


public class TestOperatorService implements OperatorService {

    public static final TestOperatorService singleton = new TestOperatorService();

    @Override
    public Set<String> handleOperators(Set<String> operatorIds) {
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

    @Override
    public Set<Action> handleAllowingActions(Definition definition, Instance instance, Set<Action> actions, String operator) {

        var currentNode = NodeService.getCurrentNode(definition, instance.nodeId);
        // test The user can customize the "allowingAction".
        // To remove the permission for "save" on a single node.
        if (NodeType.SingleNode.isEqual(currentNode.getType())) {
            actions.remove(Action.SAVE);
        }

        return actions;
    }
}
