package jp.co.onehr.workflow.dto.node;

import java.util.Set;

import com.azure.cosmos.implementation.guava25.collect.Sets;
import jp.co.onehr.workflow.EngineConfiguration;
import jp.co.onehr.workflow.constant.ApprovalType;
import jp.co.onehr.workflow.dto.Instance;
import org.apache.commons.collections4.CollectionUtils;

import static jp.co.onehr.workflow.constant.ApprovalType.OR;

/**
 * Multi-user node
 */
public class MultipleNode extends ManualNode {

    /**
     * The ids of all operators who can perform operations on the node.
     */
    public Set<String> operatorIdSet = Sets.newHashSet();

    /**
     * The ids of all organizations that can perform operations on the node.
     */
    public Set<String> operatorOrgIdSet = Sets.newHashSet();

    /**
     * Node approval type
     * <p>
     * OR: Any Approval
     * AND: All Approval
     * The default value is "OR".
     */
    public ApprovalType approvalType = OR;

    public MultipleNode() {

    }

    public MultipleNode(String nodeName) {
        this.nodeName = nodeName;
    }

    public MultipleNode(String nodeName, ApprovalType approvalType) {
        this.nodeName = nodeName;
        this.approvalType = approvalType;
    }

    public MultipleNode(String nodeName, Set<String> operatorIdSet, Set<String> operatorOrgIdSet) {
        this.nodeName = nodeName;
        this.operatorIdSet = operatorIdSet;
        this.operatorOrgIdSet = operatorOrgIdSet;
    }

    public MultipleNode(String nodeName, ApprovalType approvalType, Set<String> operatorIdSet, Set<String> operatorOrgIdSet) {
        this.nodeName = nodeName;
        this.approvalType = approvalType;
        this.operatorIdSet = operatorIdSet;
        this.operatorOrgIdSet = operatorOrgIdSet;
    }

    /**
     * Override the base class's approval type, and multiple-person nodes will use the configured type
     *
     * @return
     */
    @Override
    public ApprovalType getApprovalType() {
        return approvalType;
    }

    @Override
    public void resetCurrentOperators(Instance instance) {
        clearOperators(instance);
        instance.operatorIdSet.addAll(this.operatorIdSet);
        instance.operatorOrgIdSet.addAll(this.operatorOrgIdSet);

        if (CollectionUtils.isNotEmpty(instance.operatorIdSet)) {
            var expandOperatorIds = EngineConfiguration.getConfiguration().handleExpandOperators(instance.operatorIdSet);
            instance.expandOperatorIdSet.addAll(expandOperatorIds);
        }

        if (CollectionUtils.isNotEmpty(instance.operatorOrgIdSet)) {
            var expandOperatorIds = EngineConfiguration.getConfiguration().handleExpandOrganizations(instance.operatorOrgIdSet);
            instance.expandOperatorIdSet.addAll(expandOperatorIds);
        }

        if (ApprovalType.AND.equals(approvalType)) {
            var parallelApprovalMap = EngineConfiguration.getConfiguration().handleParallelApproval(instance.operatorIdSet, instance.operatorOrgIdSet, instance.expandOperatorIdSet);
            instance.parallelApproval.putAll(parallelApprovalMap);
        }
    }
}
