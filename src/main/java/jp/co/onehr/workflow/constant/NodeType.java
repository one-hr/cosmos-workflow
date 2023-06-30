package jp.co.onehr.workflow.constant;

import jp.co.onehr.workflow.dto.node.*;
import jp.co.onehr.workflow.exception.WorkflowException;
import org.apache.commons.lang3.EnumUtils;

/**
 * The supported node types in the workflow.
 */
public enum NodeType {
    /**
     * Start
     */
    StartNode(StartNode.class),
    /**
     * END
     */
    EndNode(EndNode.class),
    /**
     * Single-user node
     */
    SingleNode(SingleNode.class),
    /**
     * Multi-user node
     */
    MultipleNode(MultipleNode.class),
    /**
     * Robot node
     */
    RobotNode(RobotNode.class);

    public final Class<? extends Node> node;

    NodeType(Class<? extends Node> node) {
        this.node = node;
    }

    public static Class<? extends Node> getNodeClass(String node) {
        var nodeType = EnumUtils.getEnum(NodeType.class, node);
        if (nodeType == null) {
            throw new WorkflowException(WorkflowErrors.NODE_TYPE_MISMATCH, "node type is invalid", node);
        }
        return nodeType.node;
    }

    public boolean isEqual(String type) {
        return this.name().equals(type);
    }

    public static NodeType getNodeType(String type) {
        var nodeType = EnumUtils.getEnum(NodeType.class, type);
        if (nodeType == null) {
            throw new WorkflowException(WorkflowErrors.NODE_TYPE_MISMATCH, "node type is invalid", type);
        }
        return nodeType;
    }
}
