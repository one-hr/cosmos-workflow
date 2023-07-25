package jp.co.onehr.workflow.dto.node;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import jp.co.onehr.workflow.ProcessEngineConfiguration;
import jp.co.onehr.workflow.constant.Action;
import jp.co.onehr.workflow.constant.ApprovalType;
import jp.co.onehr.workflow.constant.NotificationMode;
import jp.co.onehr.workflow.dto.ApprovalStatus;
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

    /**
     * The node has the notification feature enabled
     */
    public boolean enableNotification = false;

    /**
     * The notification mode for the custom action is defined when the notification feature is enabled.
     */
    public Map<Action, NotificationMode> notificationModes = Maps.newHashMap();

    protected Node() {
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
    public abstract Set<String> resetCurrentOperators(Instance instance);

    /**
     * Resetting the parallel approval status
     *
     * @param instance
     * @param approvalType
     * @param action
     * @param operatorId
     */
    public void resetParallelApproval(Instance instance, ApprovalType approvalType, Action action, String operatorId) {
        if (ApprovalType.AND.equals(approvalType)) {
            var parallelApprovalMap = new HashMap<String, ApprovalStatus>();
            if (action.equals(Action.RETRIEVE)) {
                parallelApprovalMap.putAll(ProcessEngineConfiguration
                        .getConfiguration()
                        .handleRetrieveParallelApproval(instance.operatorIdSet, instance.operatorOrgIdSet, instance.expandOperatorIdSet, operatorId));
            } else {
                parallelApprovalMap.putAll(ProcessEngineConfiguration
                        .getConfiguration()
                        .handleParallelApproval(instance.operatorIdSet, instance.operatorOrgIdSet, instance.expandOperatorIdSet));
            }

            instance.parallelApproval.putAll(parallelApprovalMap);
        }
    }

    /**
     * Node settings validation
     */
    public abstract void checkNodeSetting();

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

    @JsonIgnore
    public NotificationMode getNotificationModesByAction(Action action) {
        if (enableNotification) {
            return this.notificationModes.getOrDefault(action, NotificationMode.ALWAYS);
        } else {
            return NotificationMode.NEVER;
        }
    }

}
