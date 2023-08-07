package jp.co.onehr.workflow.dto.node;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
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
     * Modify the instance status to Approved
     *
     * @param definition
     * @param instance
     */
    @Override
    public void handleFirstNode(Definition definition, Instance instance) {
        instance.status = Status.APPROVED;
    }

    @Override
    public void resetCurrentOperators(Instance instance) {
        clearOperators(instance);
    }

    @Override
    public Set<String> generateExpandOperatorIds() {
        return Sets.newHashSet();
    }

    @Override
    public void checkNodeSetting() {

    }

    @Override
    public void checkOperators(List<String> allowedOperatorIds) {

    }
}
