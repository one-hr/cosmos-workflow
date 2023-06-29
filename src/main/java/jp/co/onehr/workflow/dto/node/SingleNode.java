package jp.co.onehr.workflow.dto.node;

import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.WorkflowEngine;
import org.apache.commons.collections4.CollectionUtils;

/**
 * Single-user node
 * <p>
 * There is only one approver, and if the approver is empty, it will be automatically skipped
 * Department approval is not supported
 */
public class SingleNode extends Node {

    /**
     * Operator of the node
     */
    public String operatorId = "";

    public SingleNode() {

    }

    public SingleNode(String nodeName) {
        this.nodeName = nodeName;
    }

    @Override
    public void resetCurrentOperators(Instance instance) {
        clearOperators(instance);
        instance.operatorIdSet.add(this.operatorId);

        if (CollectionUtils.isNotEmpty(instance.operatorIdSet)) {
            var expandOperatorIds = WorkflowEngine.handleExpandOperators(instance.operatorIdSet);
            instance.expandOperatorIdSet.addAll(expandOperatorIds);
        }
        
    }
}
