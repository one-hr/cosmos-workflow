package jp.co.onehr.workflow.dto.node;

import java.util.HashSet;
import java.util.Set;

import jp.co.onehr.workflow.ProcessEngineConfiguration;
import jp.co.onehr.workflow.dto.Instance;
import org.apache.commons.collections4.CollectionUtils;

/**
 * Single-user node
 * <p>
 * There is only one approver, and if the approver is empty, it will be automatically skipped
 * Department approval is not supported
 */
public class SingleNode extends ManualNode {

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
    public Set<String> resetCurrentOperators(Instance instance) {
        clearOperators(instance);
        instance.operatorIdSet.add(this.operatorId);

        var expandOperatorIds = new HashSet<String>();
        if (CollectionUtils.isNotEmpty(instance.operatorIdSet)) {
            expandOperatorIds.addAll(ProcessEngineConfiguration.getConfiguration().handleExpandOperators(instance.operatorIdSet));
            instance.expandOperatorIdSet.addAll(expandOperatorIds);
        }
        return expandOperatorIds;
    }
}
