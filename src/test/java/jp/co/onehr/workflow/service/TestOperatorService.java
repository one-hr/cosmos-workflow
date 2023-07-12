package jp.co.onehr.workflow.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jp.co.onehr.workflow.constant.Action;
import jp.co.onehr.workflow.constant.NodeType;
import jp.co.onehr.workflow.constant.Status;
import jp.co.onehr.workflow.dto.ApprovalStatus;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.node.Node;


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
    public Set<Action> handleAllowingActions(Definition definition, Instance instance, Set<Action> actions, String operatorId) {
        var currentNode = NodeService.getCurrentNode(definition, instance.nodeId);

        var baseActions = handleAllowingActions(definition, instance, currentNode, actions, operatorId);

        // test The user can customize the "allowingAction".
        // To remove the permission for "save" on a single node.
        if (NodeType.SingleNode.isEqual(currentNode.getType())) {
            baseActions.remove(Action.SAVE);
        }

        return baseActions;
    }

    private Set<Action> handleAllowingActions(Definition definition, Instance instance, Node currentNode, Set<Action> actions, String operatorId) {

        if (NodeType.SingleNode.isEqual(currentNode.getType()) || NodeType.MultipleNode.isEqual(currentNode.getType())) {
            if (!instance.expandOperatorIdSet.contains(operatorId)) {
                return actions;
            }
        }

        // If status is processing, approver can reject the instance
        if (instance.status.equals(Status.PROCESSING)) {
            actions.add(Action.CANCEL);
            actions.add(Action.REJECT);
        }

        // If status is canceled, applicant can withdraw the instance
        if (instance.status.equals(Status.CANCELED)) {
            actions.add(Action.WITHDRAW);
            return actions;
        }

        // The first node only allows "save" and "next" actions
        if (NodeService.isFirstNode(definition, instance.nodeId)) {
            actions.add(Action.NEXT);
            actions.add(Action.SAVE);
            return actions;
        }

        // The last node only allows "save" and "back" actions
        if (NodeService.isLastNode(definition, instance.nodeId)) {
            actions.add(Action.SAVE);
            actions.add(Action.BACK);
            return actions;
        }

        // In other cases, the node allows all actions
        actions.add(Action.NEXT);
        actions.add(Action.SAVE);
        actions.add(Action.BACK);

        return actions;
    }
}
