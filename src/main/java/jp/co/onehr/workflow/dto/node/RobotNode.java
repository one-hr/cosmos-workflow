package jp.co.onehr.workflow.dto.node;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import jp.co.onehr.workflow.constant.Action;
import jp.co.onehr.workflow.contract.context.InstanceContext;
import jp.co.onehr.workflow.dto.Instance;

/**
 * Robot node, not subject to manual intervention, can be configured as an automated processing node.
 */
public class RobotNode extends Node {

    public RobotNode() {

    }

    public RobotNode(String nodeName) {
        this.nodeName = nodeName;
    }

    @Override
    public void resetCurrentOperators(Instance instance, InstanceContext instanceContext) {
        clearOperators(instance);
    }

    @Override
    public Set<String> generateExpandOperatorIds(InstanceContext instanceContext) {
        return Sets.newHashSet();
    }

    @Override
    public void checkNodeSetting() {

    }

    @Override
    public void checkOperators(List<String> allowedOperatorIds) {

    }

    /**
     * Temporary mapping: The robot logs only the "next" action.
     *
     * @param action
     * @return
     */
    @Override
    public boolean whetherAddOperationLog(Action action) {
        if (action.equals(Action.NEXT)) {
            return true;
        }
        return false;
    }
}
