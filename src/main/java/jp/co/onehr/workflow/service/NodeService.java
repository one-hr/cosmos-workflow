package jp.co.onehr.workflow.service;


import java.util.List;

import jp.co.onehr.workflow.constant.NodeType;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.node.Node;

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
}
