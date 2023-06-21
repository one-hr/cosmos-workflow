package jp.co.onehr.workflow.dto.node;

/**
 * The last node of the workflow, used to mark the beginning of the process.
 */
public class EndNode extends Node {

    public EndNode() {

    }

    public EndNode(String nodeName) {
        this.nodeName = nodeName;
    }

}
