package jp.co.onehr.workflow.dto.node;

/**
 * A user-involved node
 */
public abstract class ManualNode extends Node {

    /**
     * A human-operated node, which typically requires user intervention.
     * By default, notifications are sent for this type of node.
     */
    protected ManualNode() {
        enableNotification = true;
    }
}
