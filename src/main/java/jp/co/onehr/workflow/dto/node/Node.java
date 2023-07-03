package jp.co.onehr.workflow.dto.node;

import java.util.Set;
import java.util.UUID;

import com.google.common.collect.Sets;
import jp.co.onehr.workflow.constant.ApprovalType;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.base.SimpleData;

/**
 * The basic definition of a workflow node
 * all type nodes need to extend this class.
 */
public abstract class Node extends SimpleData {

    public String nodeId = UUID.randomUUID().toString();

    public String nodeName = "";

    private String type;

    /**
     * The type of plugin used by the node
     */
    public Set<String> plugins = Sets.newHashSet();

    /**
     * The configuration information of the node can be specified with any type.
     */
    public Object configuration;

    public Node() {
        type = this.getClass().getSimpleName();
    }

    public String getType() {
        return type;
    }

    /**
     * Special handling when the node is the first node of the workflow definition
     *
     * @param definition
     * @param instance
     */
    public void handleFirstNode(Definition definition, Instance instance) {

    }

    /**
     * To obtain the approval type of node, with the default being "simple"
     *
     * @return
     */
    public ApprovalType getApprovalType() {
        return ApprovalType.SIMPLE;
    }

    /**
     * Reset the operator of the current node
     *
     * @param instance
     * @return
     */
    public abstract void resetCurrentOperators(Instance instance);

    /**
     * Clear all operators of the instance.
     *
     * @param instance
     */
    protected void clearOperators(Instance instance) {
        instance.operatorIdSet.clear();
        instance.operatorOrgIdSet.clear();
        instance.expandOperatorIdSet.clear();
        instance.parallelApproval.clear();
    }
}
