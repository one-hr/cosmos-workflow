package jp.co.onehr.workflow.service;


import java.util.List;

import jp.co.onehr.workflow.dto.Definition;
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
     * @param currentNodeId
     * @return
     */
    public static int getCurrentNodeIndex(List<Node> nodes, String currentNodeId) {
        var nodeIds = nodes.stream().map(i -> i.nodeId).toList();
        return nodeIds.indexOf(currentNodeId);
    }

    /**
     * Retrieve the node from the definition based on the nodeId
     *
     * @param definition
     * @param currentNodeId
     * @return
     */
    public static Node getCurrentNode(Definition definition, String currentNodeId) {
        var nodes = getAllNodes(definition);
        var currentNodeIndex = getCurrentNodeIndex(nodes, currentNodeId);
        var currentNode = nodes.get(currentNodeIndex);
        return currentNode;
    }

    /**
     * @param definition
     * @param currentNodeId
     * @return
     */
    public static boolean isFirstNode(Definition definition, String currentNodeId) {
        var allNodes = getAllNodes(definition);
        return allNodes.get(1).nodeId.equals(currentNodeId);
    }

    /**
     * Determining if a node is the last node in the workflow
     *
     * @param definition
     * @param currentNodeId
     * @return
     */
    public static boolean isLastNode(Definition definition, String currentNodeId) {
        var allNodes = getAllNodes(definition);
        return allNodes.get(allNodes.size() - 1).nodeId.equals(currentNodeId);
    }

}
