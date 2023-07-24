package jp.co.onehr.workflow.service;


import java.util.List;

import jp.co.onehr.workflow.constant.NodeType;
import jp.co.onehr.workflow.constant.WorkflowErrors;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.PreviousNodeInfo;
import jp.co.onehr.workflow.dto.node.Node;
import jp.co.onehr.workflow.exception.WorkflowException;

public class NodeService {

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
    public static int getNodeIndexByNodeId(Definition definition, String nodeId) {
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
    public static int getNodeIndexByNodeId(List<Node> nodes, String nodeId) {
        var nodeIds = nodes.stream().map(i -> i.nodeId).toList();
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
        var currentNode = nodes.get(currentNodeIndex);
        return currentNode;
    }

    public static Node getNodeByInstance(Definition definition, Instance instance) {
        return getNodeByNodeId(definition, instance.nodeId);
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
    public static PreviousNodeInfo getPreviousNodeInfo(Definition definition, Instance instance) {
        var nodeIndex = NodeService.getNodeIndexByNodeId(definition, instance.nodeId);

        return recursivePreviousNode(definition, instance, nodeIndex, 0);
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
    private static PreviousNodeInfo recursivePreviousNode(Definition definition, Instance instance, int nodeIndex, int count) {

        if (count > 100) {
            throw new WorkflowException(WorkflowErrors.INSTANCE_OPERATOR_INVALID, "Too many recursion when finding previous node's operator ", instance.id);
        }
        count++;

        if (nodeIndex - 1 >= 0) {
            var currentNode = definition.nodes.get(nodeIndex - 1);
            if (!NodeService.isManualNode(currentNode.getType())) {
                return recursivePreviousNode(definition, instance, nodeIndex - 1, count);
            }

            var expandOperatorIds = currentNode.resetCurrentOperators(instance);
            if (expandOperatorIds.isEmpty()) {
                return recursivePreviousNode(definition, instance, nodeIndex - 1, count);
            }

            return new PreviousNodeInfo(currentNode.nodeId, expandOperatorIds);
        }
        return new PreviousNodeInfo();
    }
}
