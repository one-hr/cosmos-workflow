package jp.co.onehr.workflow.dto.node;

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
    public void resetCurrentOperators(Instance instance) {
        clearOperators(instance);
    }
}
