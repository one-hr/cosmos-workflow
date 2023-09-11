package jp.co.onehr.workflow.service;


import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import jp.co.onehr.workflow.constant.NodeType;
import jp.co.onehr.workflow.constant.WorkflowErrors;
import jp.co.onehr.workflow.contract.context.InstanceContext;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.PreviousNodeInfo;
import jp.co.onehr.workflow.dto.node.Node;
import jp.co.onehr.workflow.exception.WorkflowException;

public class NodeService {

    /**
     * Get the start node of the workflow.
     * <p>
     * The reject and cancel actions will reset the instance's node to the start node.
     *
     * @param definition
     * @return
     */
    public static Node getStartNode(Definition definition) {
        var nodes = definition.nodes;
        return nodes.get(0);
    }

    /**
     * Retrieve the first actionable node of the workflow
     * excluding the 'startNode' which is used as a marker for the beginning
     * but not considered as the first actionable node
     *
     * @param definition
     * @return
     */
    public static Node getFirstNode(Definition definition) {
        var nodes = definition.nodes;
        return nodes.get(1);
    }

    /**
     * Retrieve all the nodes in the workflow
     *
     * @param definition
     * @return
     */
    public static List<Node> getAllNodes(Definition definition) {
        return definition.nodes;
    }

    /**
     * Get the index of the current node
     *
     * @param definition
     * @param nodeId
     * @return
     */
    public static Integer getNodeIndexByNodeId(Definition definition, String nodeId) {
        var nodes = getAllNodes(definition);
        return getNodeIndexByNodeId(nodes, nodeId);
    }

    /**
     * Get the index of the current node
     *
     * @param nodes
     * @param nodeId
     * @return
     */
    public static Integer getNodeIndexByNodeId(List<Node> nodes, String nodeId) {
        var nodeIds = nodes.stream().map(i -> i.nodeId).toList();
        var nodeIdSet = new HashSet<>(nodeIds);

        if (!nodeIdSet.contains(nodeId)) {
            throw new WorkflowException(WorkflowErrors.NODE_ID_INVALID, "The current node's ID does not exist in the definition", nodeId);
        }

        return nodeIds.indexOf(nodeId);
    }

    /**
     * Retrieve the node from the definition based on the nodeId
     *
     * @param definition
     * @param nodeId
     * @return
     */
    public static Node getNodeByNodeId(Definition definition, String nodeId) {
        var nodes = getAllNodes(definition);
        var currentNodeIndex = getNodeIndexByNodeId(nodes, nodeId);
        return nodes.get(currentNodeIndex);
    }

    public static Node getNodeByInstance(Definition definition, Instance instance) {
        return getNodeByNodeId(definition, instance.nodeId);
    }

    public static boolean checkNodeExists(Definition definition, String nodeId) {
        var nodeIdSet = definition.nodes.stream().map(i -> i.nodeId).collect(Collectors.toSet());
        return nodeIdSet.contains(nodeId);
    }

    /**
     * @param definition
     * @param nodeId
     * @return
     */
    public static boolean isFirstNode(Definition definition, String nodeId) {
        var allNodes = getAllNodes(definition);
        return allNodes.get(1).nodeId.equals(nodeId);
    }

    /**
     * Determining if a node is the last node in the workflow
     *
     * @param definition
     * @param nodeId
     * @return
     */
    public static boolean isLastNode(Definition definition, String nodeId) {
        var allNodes = getAllNodes(definition);
        return allNodes.get(allNodes.size() - 1).nodeId.equals(nodeId);
    }

    public static boolean isManualNode(String type) {
        if (NodeType.SingleNode.isEqual(type) || NodeType.MultipleNode.isEqual(type)) {
            return true;
        }
        return false;
    }

    /**
     * Get the information of the previous manual node in the instance.
     *
     * @param definition
     * @param instance
     * @return
     */
    public static PreviousNodeInfo getPreviousNodeInfo(Definition definition, Instance instance, InstanceContext instanceContext) {
        var nodeIndex = NodeService.getNodeIndexByNodeId(definition, instance.nodeId);

        return recursivePreviousNode(definition, instance, instanceContext, nodeIndex, 0);
    }

    /**
     * Recursively find the previous manual node.
     *
     * @param definition
     * @param instance
     * @param nodeIndex
     * @param count
     * @return
     */
    private static PreviousNodeInfo recursivePreviousNode(Definition definition, Instance instance, InstanceContext instanceContext, int nodeIndex, int count) {

        if (count > 100) {
            throw new WorkflowException(WorkflowErrors.INSTANCE_OPERATOR_INVALID, "Too many recursion when finding previous node's operator ", instance.id);
        }
        count++;

        if (nodeIndex - 1 >= 0) {
            var currentNode = definition.nodes.get(nodeIndex - 1);
            if (!NodeService.isManualNode(currentNode.getType())) {
                return recursivePreviousNode(definition, instance, instanceContext, nodeIndex - 1, count);
            }

            var expandOperatorIds = currentNode.generateExpandOperatorIds(instanceContext);
            if (expandOperatorIds.isEmpty()) {
                return recursivePreviousNode(definition, instance, instanceContext, nodeIndex - 1, count);
            }

            return new PreviousNodeInfo(currentNode.nodeId, expandOperatorIds);
        }
        return new PreviousNodeInfo();
    }
}
