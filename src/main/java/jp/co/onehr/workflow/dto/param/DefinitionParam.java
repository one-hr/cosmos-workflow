package jp.co.onehr.workflow.dto.param;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.azure.cosmos.implementation.guava25.collect.Sets;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.collect.Lists;
import io.github.thunderz99.cosmos.util.JsonUtil;
import jp.co.onehr.workflow.constant.ApplicationMode;
import jp.co.onehr.workflow.constant.NodeType;
import jp.co.onehr.workflow.dto.node.Node;

public class DefinitionParam {

    public DefinitionParam() {

    }

    public String workflowId = "";

    public boolean enableOperatorControl = true;

    public List<String> allowedOperatorIds = new ArrayList<>();

    public List<Node> nodes = Lists.newLinkedList();

    public Set<ApplicationMode> applicationModes = Sets.newHashSet();

    /**
     * When the user performs rejection and cancellation, the instance returns to the start node
     * true: Return to the start node.
     * false: Stay at the current node
     */
    public boolean returnToStartNode = true;

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
