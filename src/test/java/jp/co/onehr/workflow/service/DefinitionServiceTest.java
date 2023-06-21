package jp.co.onehr.workflow.service;

import jp.co.onehr.workflow.base.BaseTest;
import jp.co.onehr.workflow.constant.NodeType;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Workflow;
import jp.co.onehr.workflow.dto.node.EndNode;
import jp.co.onehr.workflow.dto.node.SingleNode;
import jp.co.onehr.workflow.dto.node.StartNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class DefinitionServiceTest extends BaseTest {

    @Test
    void create_should_work() throws Exception {
        var workflow = new Workflow(getUuid(), "definition-test-create");
        var definition = new Definition(getUuid(), workflow.getId());
        try {
            WorkflowService.singleton.create(host, workflow);

            var startNode = new StartNode("start-name");

            var singleNode = new SingleNode("single-name");
            singleNode.operatorId = "operator-1";

            var endNode = new EndNode("end-name");

            definition.nodes.add(startNode);
            definition.nodes.add(singleNode);
            definition.nodes.add(endNode);

            DefinitionService.singleton.create(host, definition);

            // test node type
            {
                var result = DefinitionService.singleton.readSuppressing404(host, definition.getId());
                assertThat(result).isNotNull();

                var nodes = result.nodes;
                assertThat(nodes).hasSize(3);
                assertThat(nodes.get(0).getType()).isEqualTo(NodeType.StartNode.name());
                assertThat(nodes.get(0).nodeName).isEqualTo("start-name");
                assertThat(nodes.get(1).getType()).isEqualTo(NodeType.SingleNode.name());
                assertThat(nodes.get(1).nodeName).isEqualTo("single-name");
                assertThat(((SingleNode) nodes.get(1)).operatorId).isEqualTo("operator-1");
                assertThat(nodes.get(2).getType()).isEqualTo(NodeType.EndNode.name());
                assertThat(nodes.get(2).nodeName).isEqualTo("end-name");
            }
        } finally {
            WorkflowService.singleton.purge(host, workflow.id);
            DefinitionService.singleton.purge(host, definition.id);
        }
    }
}
