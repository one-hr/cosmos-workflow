package jp.co.onehr.workflow.dto.node;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import jp.co.onehr.workflow.ProcessConfiguration;
import jp.co.onehr.workflow.constant.Action;
import jp.co.onehr.workflow.constant.ApprovalType;
import jp.co.onehr.workflow.constant.NotificationMode;
import jp.co.onehr.workflow.contract.context.InstanceContext;
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

    /**
     * Internationalized Node Name
     */
    public LinkedHashMap<String, String> localNames = Maps.newLinkedHashMap();

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
    public abstract void resetCurrentOperators(Instance instance, InstanceContext instanceContext);

    /**
     * Generate the actual operator IDs based on the node definition.
     *
     * @return
     */
    public abstract Set<String> generateExpandOperatorIds(InstanceContext instanceContext);

    /**
     * Resetting the parallel approval status
     *
     * @param instance
     * @param approvalType
     * @param action
     * @param operatorId
     */
    public void resetParallelApproval(Instance instance, ApprovalType approvalType, Action action, String operatorId, InstanceContext instanceContext) {
        if (ApprovalType.AND.equals(approvalType)) {
            var parallelApprovalMap = new HashMap<String, ApprovalStatus>();
            if (action.equals(Action.RETRIEVE)) {
                parallelApprovalMap.putAll(ProcessConfiguration
                        .getConfiguration()
                        .handleRetrieveParallelApproval(instance.operatorIdSet, instance.operatorOrgIdSet, instance.expandOperatorIdSet, operatorId, instanceContext));
            } else {
                parallelApprovalMap.putAll(ProcessConfiguration
                        .getConfiguration()
                        .handleParallelApproval(instance.operatorIdSet, instance.operatorOrgIdSet, instance.expandOperatorIdSet, instanceContext));
            }

            instance.parallelApproval.putAll(parallelApprovalMap);
        }
    }

    /**
     * Node settings validation
     */
    public abstract void checkNodeSetting();

    /**
     * When operator restriction is enabled in the definition, check if the node operator is allowed to use it
     */
    public abstract void checkOperators(List<String> allowedOperatorIds);

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

    /**
     * Determine whether the execution result of the current node should be added to the operation log.
     * True: Add to the log.
     * False: Do not add to the log.
     * <p>
     * todo:Node needs to add a field to control whether logging is recorded.
     *
     * @param action
     * @return
     */
    @JsonIgnore
    public boolean whetherAddOperationLog(Action action) {
        return true;
    }
}
