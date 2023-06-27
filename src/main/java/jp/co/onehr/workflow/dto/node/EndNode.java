package jp.co.onehr.workflow.dto.node;

import jp.co.onehr.workflow.constant.Status;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;

/**
 * The last node of the workflow, used to mark the beginning of the process.
 */
public class EndNode extends Node {

    public EndNode() {

    }

    public EndNode(String nodeName) {
        this.nodeName = nodeName;
    }

    /**
     * If the type of the first node is endNode
     * it means that the workflow directly reaches the last node
     * Modify the instance status to Finished
     *
     * @param definition
     * @param instance
     */
    @Override
    public void handleFirstNode(Definition definition, Instance instance) {
        instance.status = Status.Finished;
    }

    @Override
    public void resetCurrentOperators(Instance instance) {
        clearOperators(instance);
    }
}
