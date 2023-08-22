package jp.co.onehr.workflow.dto.node;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import jp.co.onehr.workflow.ProcessConfiguration;
import jp.co.onehr.workflow.constant.WorkflowErrors;
import jp.co.onehr.workflow.contract.context.InstanceContext;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.exception.WorkflowException;
import org.apache.commons.lang3.StringUtils;

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
    public void resetCurrentOperators(Instance instance, InstanceContext instanceContext) {
        clearOperators(instance);
        instance.operatorIdSet.add(this.operatorId);

        var expandOperatorIds = generateExpandOperatorIds(instanceContext);

        instance.expandOperatorIdSet.addAll(expandOperatorIds);
    }

    @Override
    public Set<String> generateExpandOperatorIds(InstanceContext instanceContext) {
        var expandOperatorIds = new HashSet<String>();
        if (StringUtils.isNotEmpty(this.operatorId)) {
            expandOperatorIds.addAll(ProcessConfiguration.getConfiguration().handleExpandOperators(Sets.newHashSet(this.operatorId), instanceContext));
        }
        return expandOperatorIds;
    }

    @Override
    public void checkNodeSetting() {
        if (StringUtils.isBlank(operatorId)) {
            throw new WorkflowException(WorkflowErrors.NODE_SETTING_INVALID, "The operator of a single node cannot be empty", nodeName);
        }
    }

    @Override
    public void checkOperators(List<String> allowedOperatorIds) {
        if (!allowedOperatorIds.contains(operatorId)) {
            throw new WorkflowException(WorkflowErrors.NODE_OPERATOR_INVALID, "The operator of the node is not allowed as per the definition", nodeId);
        }
    }
}
