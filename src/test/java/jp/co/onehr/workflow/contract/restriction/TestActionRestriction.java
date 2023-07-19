package jp.co.onehr.workflow.contract.restriction;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Sets;
import jp.co.onehr.workflow.constant.Action;
import jp.co.onehr.workflow.constant.NodeType;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.service.NodeService;


public class TestActionRestriction implements ActionRestriction {

    public static final TestActionRestriction singleton = new TestActionRestriction();


    @Override
    public Set<Action> handleProcessingByOperator(Definition definition, Instance instance, String operatorId) {

        var actions = new HashSet<Action>();
        var currentNode = NodeService.getNodeByNodeId(definition, instance.nodeId);
        // test The user can customize the "allowingAction".
        // To remove the permission for "save" on a single node.
        if (NodeType.SingleNode.isEqual(currentNode.getType())) {
            actions.add(Action.SAVE);
        }
        return actions;
    }

    @Override
    public Set<Action> handleRejectedByOperator(Definition definition, Instance instance, String operatorId) {
        return Sets.newHashSet();
    }

    @Override
    public Set<Action> handleCanceledByOperator(Definition definition, Instance instance, String operatorId) {
        return Sets.newHashSet();
    }

    @Override
    public Set<Action> handleApprovedByOperator(Definition definition, Instance instance, String operatorId) {
        return Sets.newHashSet();
    }

    @Override
    public Set<Action> handleFinishedByOperator(Definition definition, Instance instance, String operatorId) {
        return Sets.newHashSet();
    }
}
