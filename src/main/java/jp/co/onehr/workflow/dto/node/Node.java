package jp.co.onehr.workflow.dto.node;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jp.co.onehr.workflow.constant.ApproveType;
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

    @JsonGetter
    public String getType() {
        return this.getClass().getSimpleName();
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
    @JsonIgnore
    public ApproveType getApproveType() {
        return ApproveType.SIMPLE;
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
    }
}
