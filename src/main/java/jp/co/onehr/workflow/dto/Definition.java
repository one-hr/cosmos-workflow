package jp.co.onehr.workflow.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.collect.Sets;
import com.google.common.collect.Lists;
import io.github.thunderz99.cosmos.util.JsonUtil;
import jp.co.onehr.workflow.constant.ApplicationMode;
import jp.co.onehr.workflow.constant.NodeType;
import jp.co.onehr.workflow.dto.base.BaseData;
import jp.co.onehr.workflow.dto.node.Node;

/**
 * Workflow definition
 * <p>
 * Metadata information of the workflow definition
 * It includes the order of all nodes, the defined version
 * the version number recorded by the workflow for corresponding to the definition
 */
public class Definition extends BaseData {

    /**
     * The ID of the associated workflow.
     */
    public String workflowId = "";

    /**
     * The current version number of the definition.
     */
    public Integer version = 0;

    /**
     * The order and path of nodes in the workflow.
     */
    public List<Node> nodes = Lists.newLinkedList();

    /**
     * The allowed application modes for the workflow: proxy/self.
     */
    public Set<ApplicationMode> applicationModes = Sets.newHashSet();

    /**
     * Enable restricting operators for workflow definition
     */
    public boolean enableOperatorControl = true;

    /**
     * Allowed operator IDs for workflow definition
     */
    public List<String> allowedOperatorIds = new ArrayList<>();

    /**
     * When the user performs rejection and cancellation, the instance returns to the start node
     * true: Return to the start node.
     * false: Stay at the current node
     */
    public boolean returnToStartNode = true;

    public Definition() {

    }

    public Definition(String id, String workflowId) {
        this.id = id;
        this.workflowId = workflowId;
    }

    @JsonSetter
    public void setNodes(List<Map<String, Object>> nodeMapList) {
        this.nodes.clear();
        for (var nodeMap : nodeMapList) {
            var type = nodeMap.getOrDefault("type", "").toString();
            Node node = JsonUtil.fromJson(JsonUtil.toJson(nodeMap), NodeType.getNodeClass(type));
            this.nodes.add(node);
        }
    }

}
