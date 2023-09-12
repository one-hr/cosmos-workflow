package jp.co.onehr.workflow.service;

import jp.co.onehr.workflow.base.BaseTest;
import jp.co.onehr.workflow.constant.NodeType;
import jp.co.onehr.workflow.dto.node.SingleNode;
import jp.co.onehr.workflow.dto.param.WorkflowCreationParam;
import org.junit.jupiter.api.Test;

import static jp.co.onehr.workflow.service.DefinitionService.DEFAULT_END_NODE_NAME;
import static jp.co.onehr.workflow.service.DefinitionService.DEFAULT_START_NODE_NAME;
import static org.assertj.core.api.Assertions.assertThat;


public class NodeServiceTest extends BaseTest {

    @Test
    void getFirstNode_should_work() throws Exception {
        var creationParam = new WorkflowCreationParam();
        creationParam.name = "getFirstNode_should_work";
        var workflowId = "";

        try {
            var workflow = processDesign.createWorkflow(host, creationParam);
            workflowId = workflow.getId();

            var definition = DefinitionService.singleton.getCurrentDefinition(host, workflow.id, 0);

            {
                var result = NodeService.getFirstNode(definition);
                assertThat(result.getType()).isEqualTo(NodeType.EndNode.name());
            }

            definition.nodes.add(1, new SingleNode("DEFAULT_SINGLE_NODE_NAME-1"));

            {
                var result = NodeService.getFirstNode(definition);
                assertThat(result.getType()).isEqualTo(NodeType.SingleNode.name());
                assertThat(result.nodeName).isEqualTo("DEFAULT_SINGLE_NODE_NAME-1");
            }
        } finally {
            WorkflowService.singleton.purge(host, workflowId);
        }
    }

    @Test
    void getAllNodes_should_work() throws Exception {
        var creationParam = new WorkflowCreationParam();
        creationParam.name = "getAllNodes_should_work";
        var workflowId = "";
        try {
            var workflow = processDesign.createWorkflow(host, creationParam);
            workflowId = workflow.getId();
            var definition = DefinitionService.singleton.getCurrentDefinition(host, workflow.id, 0);

            {
                var result = NodeService.getAllNodes(definition);
                assertThat(result).hasSize(2);
                assertThat(result.get(0).getType()).isEqualTo(NodeType.StartNode.name());
                assertThat(result.get(0).nodeName).isEqualTo(DEFAULT_START_NODE_NAME);
                assertThat(result.get(1).getType()).isEqualTo(NodeType.EndNode.name());
                assertThat(result.get(1).nodeName).isEqualTo(DEFAULT_END_NODE_NAME);
            }

            definition.nodes.add(1, new SingleNode("DEFAULT_SINGLE_NODE_NAME-1"));

            {
                var result = NodeService.getAllNodes(definition);
                assertThat(result).hasSize(3);
                assertThat(result.get(0).getType()).isEqualTo(NodeType.StartNode.name());
                assertThat(result.get(0).nodeName).isEqualTo(DEFAULT_START_NODE_NAME);
                assertThat(result.get(1).getType()).isEqualTo(NodeType.SingleNode.name());
                assertThat(result.get(1).nodeName).isEqualTo("DEFAULT_SINGLE_NODE_NAME-1");
                assertThat(result.get(2).getType()).isEqualTo(NodeType.EndNode.name());
                assertThat(result.get(2).nodeName).isEqualTo(DEFAULT_END_NODE_NAME);
            }
        } finally {
            WorkflowService.singleton.purge(host, workflowId);
        }
    }

    @Test
    void getNodeIndexByNodeId_should_work() throws Exception {
        var creationParam = new WorkflowCreationParam();
        creationParam.name = "getNodeIndexByNodeId_should_work";
        var workflowId = "";
        try {
            var workflow = processDesign.createWorkflow(host, creationParam);
            workflowId = workflow.getId();

            var definition = DefinitionService.singleton.getCurrentDefinition(host, workflow.id, 0);

            var startNodeId = definition.nodes.get(0).nodeId;
            var endNodeId = definition.nodes.get(1).nodeId;

            {
                var nodes = NodeService.getAllNodes(definition);
                var result1 = NodeService.getNodeIndexByNodeId(nodes, startNodeId);
                assertThat(result1).isEqualTo(0);
                var result2 = NodeService.getNodeIndexByNodeId(nodes, endNodeId);
                assertThat(result2).isEqualTo(1);
            }

            var singleNode = new SingleNode("DEFAULT_SINGLE_NODE_NAME-1");
            definition.nodes.add(1, singleNode);

            {
                var nodes = NodeService.getAllNodes(definition);
                var result1 = NodeService.getNodeIndexByNodeId(nodes, startNodeId);
                assertThat(result1).isEqualTo(0);
                var result2 = NodeService.getNodeIndexByNodeId(nodes, endNodeId);
                assertThat(result2).isEqualTo(2);

                var result3 = NodeService.getNodeIndexByNodeId(nodes, singleNode.nodeId);
                assertThat(result3).isEqualTo(1);
            }
        } finally {
            WorkflowService.singleton.purge(host, workflowId);
        }
    }

    @Test
    void getNodeByNodeId_should_work() throws Exception {
        var creationParam = new WorkflowCreationParam();
        creationParam.name = "getNodeByNodeId_should_work";
        var workflowId = "";
        try {
            var workflow = processDesign.createWorkflow(host, creationParam);
            workflowId = workflow.getId();

            var definition = DefinitionService.singleton.getCurrentDefinition(host, workflow.id, 0);

            var startNodeId = definition.nodes.get(0).nodeId;
            var endNodeId = definition.nodes.get(1).nodeId;

            {
                var result1 = NodeService.getNodeByNodeId(definition, startNodeId);
                assertThat(result1.nodeName).isEqualTo(DEFAULT_START_NODE_NAME);
                assertThat(result1.getType()).isEqualTo(NodeType.StartNode.name());
                var result2 = NodeService.getNodeByNodeId(definition, endNodeId);
                assertThat(result2.nodeName).isEqualTo(DEFAULT_END_NODE_NAME);
                assertThat(result2.getType()).isEqualTo(NodeType.EndNode.name());
            }

            var singleNode = new SingleNode("DEFAULT_SINGLE_NODE_NAME-1");
            definition.nodes.add(1, singleNode);

            {
                var result1 = NodeService.getNodeByNodeId(definition, startNodeId);
                assertThat(result1.nodeName).isEqualTo(DEFAULT_START_NODE_NAME);
                assertThat(result1.getType()).isEqualTo(NodeType.StartNode.name());
                var result2 = NodeService.getNodeByNodeId(definition, singleNode.nodeId);
                assertThat(result2.nodeName).isEqualTo("DEFAULT_SINGLE_NODE_NAME-1");
                assertThat(result2.getType()).isEqualTo(NodeType.SingleNode.name());
            }
        } finally {
            WorkflowService.singleton.purge(host, workflowId);
        }
    }

    @Test
    void checkNodeExists_should_work() throws Exception {
        var creationParam = new WorkflowCreationParam();
        creationParam.name = "isFirstNode_should_work";
        var workflowId = "";
        try {
            var workflow = processDesign.createWorkflow(host, creationParam);
            workflowId = workflow.getId();

            var definition = DefinitionService.singleton.getCurrentDefinition(host, workflow.id, 0);

            var startNodeId = definition.nodes.get(0).nodeId;
            var endNodeId = definition.nodes.get(1).nodeId;

            var result1 = NodeService.checkNodeExists(definition, startNodeId);
            assertThat(result1).isTrue();

            var singleNode = new SingleNode("DEFAULT_SINGLE_NODE_NAME-1");

            var result2 = NodeService.checkNodeExists(definition, singleNode.nodeId);
            assertThat(result2).isFalse();

            definition.nodes.add(1, singleNode);

            var result3 = NodeService.checkNodeExists(definition, singleNode.nodeId);
            assertThat(result3).isTrue();
            
        } finally {
            WorkflowService.singleton.purge(host, workflowId);
        }
    }

    @Test
    void isFirstNode_should_work() throws Exception {
        var creationParam = new WorkflowCreationParam();
        creationParam.name = "isFirstNode_should_work";
        var workflowId = "";
        try {
            var workflow = processDesign.createWorkflow(host, creationParam);
            workflowId = workflow.getId();

            var definition = DefinitionService.singleton.getCurrentDefinition(host, workflow.id, 0);

            var startNodeId = definition.nodes.get(0).nodeId;
            var endNodeId = definition.nodes.get(1).nodeId;

            var result1 = NodeService.isFirstNode(definition, startNodeId);
            assertThat(result1).isFalse();

            var result2 = NodeService.isFirstNode(definition, endNodeId);
            assertThat(result2).isTrue();


            var singleNode = new SingleNode("DEFAULT_SINGLE_NODE_NAME-1");
            definition.nodes.add(1, singleNode);

            var result3 = NodeService.isFirstNode(definition, endNodeId);
            assertThat(result3).isFalse();

            var result4 = NodeService.isFirstNode(definition, singleNode.nodeId);
            assertThat(result4).isTrue();
        } finally {
            WorkflowService.singleton.purge(host, workflowId);
        }
    }

    @Test
    void isLastNode_should_work() throws Exception {
        var creationParam = new WorkflowCreationParam();
        creationParam.name = "isLastNode_should_work";
        var workflowId = "";
        try {
            var workflow = processDesign.createWorkflow(host, creationParam);
            workflowId = workflow.getId();

            var definition = DefinitionService.singleton.getCurrentDefinition(host, workflow.id, 0);

            var startNodeId = definition.nodes.get(0).nodeId;
            var endNodeId = definition.nodes.get(1).nodeId;

            var result1 = NodeService.isLastNode(definition, startNodeId);
            assertThat(result1).isFalse();

            var result2 = NodeService.isLastNode(definition, endNodeId);
            assertThat(result2).isTrue();


            var singleNode = new SingleNode("DEFAULT_SINGLE_NODE_NAME-1");
            definition.nodes.add(1, singleNode);

            var result3 = NodeService.isLastNode(definition, startNodeId);
            assertThat(result3).isFalse();

            var result4 = NodeService.isLastNode(definition, singleNode.nodeId);
            assertThat(result4).isFalse();

            var result5 = NodeService.isLastNode(definition, endNodeId);
            assertThat(result5).isTrue();

        } finally {
            WorkflowService.singleton.purge(host, workflowId);
        }
    }

}
