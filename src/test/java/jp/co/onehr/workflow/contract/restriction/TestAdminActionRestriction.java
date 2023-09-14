package jp.co.onehr.workflow.contract.restriction;

import java.util.HashSet;
import java.util.Set;

import jp.co.onehr.workflow.constant.Action;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.service.NodeService;


public class TestAdminActionRestriction implements AdminActionRestriction {

    public static final TestAdminActionRestriction singleton = new TestAdminActionRestriction();

    @Override
    public Set<Action> handleProcessingByAdmin(Definition definition, Instance instance, String operatorId) {
        var actions = new HashSet<Action>();
        var currentNode = NodeService.getNodeByNodeId(definition, instance.nodeId);
        if ("DEFAULT_MULTIPLE_NODE_NAME-2".equals(currentNode.nodeName) || "DEFAULT_MULTIPLE_NODE_NAME-1".equals(currentNode.nodeName)) {
            actions.add(Action.CANCEL);
        }
        return actions;
    }

    @Override
    public Set<Action> handleRejectedByAdmin(Definition definition, Instance instance, String operatorId) {
        return null;
    }

    @Override
    public Set<Action> handleCanceledByAdmin(Definition definition, Instance instance, String operatorId) {
        return null;
    }

    @Override
    public Set<Action> handleApprovedByAdmin(Definition definition, Instance instance, String operatorId) {
        return null;
    }

    @Override
    public Set<Action> handleFinishedByAdmin(Definition definition, Instance instance, String operatorId) {
        return null;
    }
}
