package jp.co.onehr.workflow.dto.node;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.azure.cosmos.implementation.guava25.collect.Sets;
import jp.co.onehr.workflow.ProcessConfiguration;
import jp.co.onehr.workflow.constant.ApprovalType;
import jp.co.onehr.workflow.constant.WorkflowErrors;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.exception.WorkflowException;
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

        var expandOperatorIds = generateExpandOperatorIds();

        instance.expandOperatorIdSet.addAll(expandOperatorIds);
    }

    @Override
    public Set<String> generateExpandOperatorIds() {
        var expandOperatorIds = new HashSet<String>();

        if (CollectionUtils.isNotEmpty(this.operatorIdSet)) {
            expandOperatorIds.addAll(ProcessConfiguration.getConfiguration().handleExpandOperators(this.operatorIdSet));
        }

        if (CollectionUtils.isNotEmpty(this.operatorOrgIdSet)) {
            expandOperatorIds.addAll(ProcessConfiguration.getConfiguration().handleExpandOrganizations(this.operatorOrgIdSet));
        }

        return expandOperatorIds;
    }

    @Override
    public void checkNodeSetting() {
        if (CollectionUtils.isEmpty(operatorIdSet) && CollectionUtils.isEmpty(operatorOrgIdSet)) {
            throw new WorkflowException(WorkflowErrors.NODE_SETTING_INVALID,
                    "The node's operator and operator organization cannot both be empty.", nodeName);
        }
    }

    @Override
    public void checkOperators(List<String> allowedOperatorIds) {
        if (!allowedOperatorIds.containsAll(operatorIdSet) || !allowedOperatorIds.containsAll(operatorOrgIdSet)) {
            throw new WorkflowException(WorkflowErrors.NODE_OPERATOR_INVALID, "The operator of the node is not allowed as per the definition", nodeId);
        }
    }

}
