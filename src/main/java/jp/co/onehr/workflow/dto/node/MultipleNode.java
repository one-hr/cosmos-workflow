package jp.co.onehr.workflow.dto.node;

import java.util.Set;

import com.azure.cosmos.implementation.guava25.collect.Sets;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jp.co.onehr.workflow.constant.ApproveType;
import jp.co.onehr.workflow.dto.Instance;

import static jp.co.onehr.workflow.constant.ApproveType.OR;

/**
 * Multi-user node
 */
public class MultipleNode extends Node {

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
    public ApproveType approveType = OR;

    public MultipleNode() {

    }

    /**
     * Override the base class's approval type, and multiple-person nodes will use the configured type
     *
     * @return
     */
    @JsonIgnore
    @Override
    public ApproveType getApproveType() {
        return approveType;
    }

    @Override
    public void resetCurrentOperators(Instance instance) {
        clearOperators(instance);
        instance.operatorIdSet.addAll(this.operatorIdSet);
        instance.operatorOrgIdSet.addAll(this.operatorOrgIdSet);
    }
}
