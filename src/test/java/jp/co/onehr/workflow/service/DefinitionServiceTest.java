package jp.co.onehr.workflow.service;

import io.github.thunderz99.cosmos.condition.Condition;
import jp.co.onehr.workflow.base.BaseCRUDServiceTest;
import jp.co.onehr.workflow.constant.ApplicationMode;
import jp.co.onehr.workflow.constant.NodeType;
import jp.co.onehr.workflow.constant.WorkflowErrors;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Workflow;
import jp.co.onehr.workflow.dto.node.EndNode;
import jp.co.onehr.workflow.dto.node.SingleNode;
import jp.co.onehr.workflow.dto.node.StartNode;
import jp.co.onehr.workflow.exception.WorkflowException;
import org.junit.jupiter.api.Test;

import static jp.co.onehr.workflow.service.DefinitionService.DEFAULT_END_NODE_NAME;
import static jp.co.onehr.workflow.service.DefinitionService.DEFAULT_START_NODE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class DefinitionServiceTest extends BaseCRUDServiceTest<Definition, DefinitionService> {

    @Override
    protected Class<Definition> getDataClass() {
        return Definition.class;
    }

    @Override
    protected DefinitionService getService() {
        return DefinitionService.singleton;
    }

    @Test
    void createInitialDefinition_should_work() throws Exception {
        var workflow = new Workflow(getUuid(), "createInitialDefinition_should_work");
        Definition definition = null;
        try {
            definition = getService().createInitialDefinition(host, workflow);

            var result = getService().readSuppressing404(host, definition.id);

            assertThat(result.workflowId).isEqualTo(workflow.id);
            assertThat(result.version).isEqualTo(0);
            assertThat(result.applicationModes).hasSize(1);
            assertThat(result.applicationModes).containsExactlyInAnyOrder(ApplicationMode.SELF);

            assertThat(result.nodes).hasSize(2);
            assertThat(result.nodes.get(0).nodeName).isEqualTo("Start_Node");
            assertThat(result.nodes.get(0).getType()).isEqualTo(NodeType.StartNode.name());
            assertThat(result.nodes.get(1).nodeName).isEqualTo("End_Node");
            assertThat(result.nodes.get(1).getType()).isEqualTo(NodeType.EndNode.name());
        } finally {
            if (definition != null) {
                DefinitionService.singleton.purge(host, definition.id);
            }
        }
    }

    @Test
    void create_should_work() throws Exception {
        var workflow = new Workflow(getUuid(), "definition-test-create");
        var definition = new Definition(getUuid(), workflow.getId());
        try {
            processEngine.createWorkflow(host, workflow);

            var startNode = new StartNode("start-name");

            var singleNode = new SingleNode("single-name");
            singleNode.operatorId = "operator-1";

            var endNode = new EndNode("end-name");

            definition.nodes.add(startNode);
            definition.nodes.add(singleNode);
            definition.nodes.add(endNode);

            getService().create(host, definition);

            // test node type
            {
                var result = processEngine.getDefinition(host, definition.getId());
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
        }
    }

    @Test
    void upsert_should_work() throws Exception {
        var workflow = new Workflow(getUuid(), "upsert_should_work");
        try {
            workflow = processEngine.createWorkflow(host, workflow);

            workflow = processEngine.getWorkflow(host, workflow.getId());
            assertThat(workflow.enableVersion).isTrue();
            assertThat(workflow.currentVersion).isEqualTo(0);

            workflow.enableVersion = false;
            processEngine.upsertWorkflow(host, workflow);

            {
                var definitions = processEngine.findDefinitions(host, Condition.filter("workflowId", workflow.id));
                assertThat(definitions).hasSize(1);
                var definition = definitions.get(0);
                assertThat(definition.nodes).hasSize(2);

                var singleNode = new SingleNode("DEFAULT_SINGLE_NODE_NAME-1");
                singleNode.operatorId = "operator-1";
                definition.nodes.add(1, singleNode);
                getService().upsert(host, definition);

                workflow = processEngine.getWorkflow(host, workflow.getId());
                assertThat(workflow.enableVersion).isFalse();
                assertThat(workflow.currentVersion).isEqualTo(0);

                definitions = processEngine.findDefinitions(host, Condition.filter("workflowId", workflow.id));
                assertThat(definitions).hasSize(1);

                var result = processEngine.getDefinition(host, definition.id);
                assertThat(result.nodes).hasSize(3);
                assertThat(result.version).isEqualTo(0);
            }

            workflow.enableVersion = true;
            processEngine.upsertWorkflow(host, workflow);

            {
                var definitions = processEngine.findDefinitions(host, Condition.filter("workflowId", workflow.id));
                assertThat(definitions).hasSize(1);

                var definition = definitions.get(0);
                assertThat(definition.nodes).hasSize(3);

                var singleNode = new SingleNode("DEFAULT_SINGLE_NODE_NAME-2");
                singleNode.operatorId = "operator-2";
                definition.nodes.add(2, singleNode);
                getService().upsert(host, definition);

                workflow = processEngine.getWorkflow(host, workflow.getId());
                assertThat(workflow.enableVersion).isTrue();
                assertThat(workflow.currentVersion).isEqualTo(1);

                definitions = processEngine.findDefinitions(host, Condition.filter("workflowId", workflow.id));
                assertThat(definitions).hasSize(2);

                var result1 = processEngine.getCurrentDefinition(host, workflow.id, 0);
                assertThat(result1.nodes).hasSize(3);
                assertThat(result1.version).isEqualTo(0);

                var result2 = processEngine.getCurrentDefinition(host, workflow.id, 1);
                assertThat(result2.nodes).hasSize(4);
                assertThat(result2.version).isEqualTo(1);
            }
        } finally {
            WorkflowService.singleton.purge(host, workflow.id);
        }
    }

    @Test
    void getDefinition_should_work() throws Exception {
        var workflow = new Workflow(getUuid(), "getDefinition_should_work");
        try {
            workflow = processEngine.createWorkflow(host, workflow);
            var definition = processEngine.getCurrentDefinition(host, workflow.id, workflow.currentVersion);
            {
                var result = processEngine.getDefinition(host, definition.getId());
                assertThat(result.id).isEqualTo(definition.id);
                assertThat(result.version).isEqualTo(definition.version);
                assertThat(result.workflowId).isEqualTo(workflow.id);
            }

            {
                assertThatThrownBy(() -> processEngine.getDefinition(host, "error-id"))
                        .isInstanceOf(WorkflowException.class)
                        .hasMessageContaining(WorkflowErrors.DEFINITION_NOT_EXIST.name());
            }
        } finally {
            WorkflowService.singleton.purge(host, workflow.id);
        }
    }

    @Test
    void getCurrentDefinition_should_work() throws Exception {
        var workflow = new Workflow(getUuid(), "getCurrentDefinition_should_work");
        try {
            workflow = processEngine.createWorkflow(host, workflow);
            workflow = processEngine.getWorkflow(host, workflow.getId());
            var workflowId = workflow.id;
            {
                var result = processEngine.getCurrentDefinition(host, workflowId, 0);
                assertThat(result.nodes).hasSize(2);
                assertThat(result.version).isEqualTo(0);
            }

            {
                assertThatThrownBy(() -> processEngine.getCurrentDefinition(host, workflowId, 1))
                        .isInstanceOf(WorkflowException.class)
                        .hasMessageContaining(WorkflowErrors.DEFINITION_NOT_EXIST.name())
                        .hasMessageContaining("The definition for the current version of the workflow was not found");
            }
        } finally {
            WorkflowService.singleton.purge(host, workflow.id);
        }
    }

    @Test
    void checkNodes_should_work() throws Exception {
        var workflow = new Workflow(getUuid(), "checkNodes_should_work");
        var definition = new Definition(getUuid(), workflow.getId());

        {
            assertThatThrownBy(() -> getService().checkNodes(definition))
                    .isInstanceOf(WorkflowException.class)
                    .hasMessageContaining(WorkflowErrors.DEFINITION_NODE_SIZE_INVALID.name())
                    .hasMessageContaining("Workflow must have at least two nodes");
        }

        {
            definition.nodes.clear();
            definition.nodes.add(new SingleNode("DEFAULT_SINGLE_NODE_NAME-1"));
            definition.nodes.add(new SingleNode("DEFAULT_SINGLE_NODE_NAME-2"));
            assertThatThrownBy(() -> getService().checkNodes(definition))
                    .isInstanceOf(WorkflowException.class)
                    .hasMessageContaining(WorkflowErrors.DEFINITION_FIRST_NODE_INVALID.name())
                    .hasMessageContaining("The first node of a workflow must be a start node");
        }

        {
            definition.nodes.clear();
            definition.nodes.add(new StartNode(DEFAULT_START_NODE_NAME));
            definition.nodes.add(new SingleNode("DEFAULT_SINGLE_NODE_NAME-1"));
            definition.nodes.add(new SingleNode("DEFAULT_SINGLE_NODE_NAME-2"));
            assertThatThrownBy(() -> getService().checkNodes(definition))
                    .isInstanceOf(WorkflowException.class)
                    .hasMessageContaining(WorkflowErrors.DEFINITION_END_NODE_INVALID.name())
                    .hasMessageContaining("The last node of a workflow must be an end node");
        }

        {
            definition.nodes.clear();
            definition.nodes.add(new StartNode(DEFAULT_START_NODE_NAME));
            definition.nodes.add(new SingleNode());
            definition.nodes.add(new EndNode(DEFAULT_END_NODE_NAME));
            assertThatThrownBy(() -> getService().checkNodes(definition))
                    .isInstanceOf(WorkflowException.class)
                    .hasMessageContaining(WorkflowErrors.NODE_NAME_INVALID.name())
                    .hasMessageContaining("The name of a node cannot be empty");
        }

        {
            definition.nodes.clear();
            definition.nodes.add(new StartNode(DEFAULT_START_NODE_NAME));
            definition.nodes.add(new StartNode(DEFAULT_START_NODE_NAME));
            definition.nodes.add(new SingleNode("DEFAULT_SINGLE_NODE_NAME-1"));
            definition.nodes.add(new SingleNode("DEFAULT_SINGLE_NODE_NAME-2"));
            definition.nodes.add(new EndNode(DEFAULT_END_NODE_NAME));
            assertThatThrownBy(() -> getService().checkNodes(definition))
                    .isInstanceOf(WorkflowException.class)
                    .hasMessageContaining(WorkflowErrors.NODE_TYPE_INVALID.name())
                    .hasMessageContaining("A workflow can only have one start node");
        }

        {
            definition.nodes.clear();
            definition.nodes.add(new StartNode(DEFAULT_START_NODE_NAME));
            definition.nodes.add(new EndNode(DEFAULT_END_NODE_NAME));
            definition.nodes.add(new EndNode(DEFAULT_END_NODE_NAME));
            assertThatThrownBy(() -> getService().checkNodes(definition))
                    .isInstanceOf(WorkflowException.class)
                    .hasMessageContaining(WorkflowErrors.NODE_TYPE_INVALID.name())
                    .hasMessageContaining("A workflow can only have one end node");
        }

        // case passed
        {
            definition.nodes.clear();
            definition.nodes.add(new StartNode(DEFAULT_START_NODE_NAME));
            definition.nodes.add(new EndNode(DEFAULT_END_NODE_NAME));
            getService().checkNodes(definition);
        }

    }

}
