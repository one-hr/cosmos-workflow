package jp.co.onehr.workflow.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.thunderz99.cosmos.condition.Condition;
import jp.co.onehr.workflow.base.BaseCRUDServiceTest;
import jp.co.onehr.workflow.constant.ApplicationMode;
import jp.co.onehr.workflow.constant.ApprovalType;
import jp.co.onehr.workflow.constant.NodeType;
import jp.co.onehr.workflow.constant.WorkflowErrors;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.OperateLog;
import jp.co.onehr.workflow.dto.Workflow;
import jp.co.onehr.workflow.dto.node.*;
import jp.co.onehr.workflow.dto.param.DefinitionParam;
import jp.co.onehr.workflow.dto.param.WorkflowCreationParam;
import jp.co.onehr.workflow.dto.param.WorkflowUpdatingParam;
import jp.co.onehr.workflow.exception.WorkflowException;
import org.assertj.core.api.InstanceOfAssertFactories;
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
        try {
            var creationParam = new WorkflowCreationParam();
            creationParam.name = workflow.name;

            var definition = getService().createInitialDefinition(host, workflow, creationParam);

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
            WorkflowService.singleton.purge(host, workflow.id);
        }
    }

    @Test
    void createInitialDefinition_with_nodes_should_work() throws Exception {
        var workflow = new Workflow(getUuid(), "createInitialDefinition_should_work");
        try {
            var creationParam = new WorkflowCreationParam();
            creationParam.name = workflow.name;
            creationParam.returnToStartNode = false;
            creationParam.enableOperatorControl = true;
            creationParam.allowedOperatorIds.addAll(List.of("operator-2", "operator-3", "operator-4"));

            var singleNode1 = new SingleNode("DEFAULT_SINGLE_NODE_NAME-1");
            singleNode1.operatorId = "operator-2";
            creationParam.nodes.add(singleNode1);

            var multipleNode2 = new MultipleNode("DEFAULT_MULTIPLE_NODE_NAME-2", ApprovalType.OR, Set.of("operator-3", "operator-4"), Set.of());
            creationParam.nodes.add(multipleNode2);


            var definition = getService().createInitialDefinition(host, workflow, creationParam);

            var result = getService().readSuppressing404(host, definition.id);

            assertThat(result.workflowId).isEqualTo(workflow.id);
            assertThat(result.version).isEqualTo(0);
            assertThat(result.applicationModes).hasSize(1);
            assertThat(result.applicationModes).containsExactlyInAnyOrder(ApplicationMode.SELF);
            assertThat(result.returnToStartNode).isFalse();

            assertThat(result.nodes).hasSize(4);
            assertThat(result.nodes.get(0).nodeName).isEqualTo("Start_Node");
            assertThat(result.nodes.get(0).getType()).isEqualTo(NodeType.StartNode.name());

            assertThat(result.nodes.get(1).nodeName).isEqualTo("DEFAULT_SINGLE_NODE_NAME-1");
            assertThat(result.nodes.get(1).getType()).isEqualTo(NodeType.SingleNode.name());

            assertThat(result.nodes.get(2).nodeName).isEqualTo("DEFAULT_MULTIPLE_NODE_NAME-2");
            assertThat(result.nodes.get(2).getType()).isEqualTo(NodeType.MultipleNode.name());

            assertThat(result.nodes.get(3).nodeName).isEqualTo("End_Node");
            assertThat(result.nodes.get(3).getType()).isEqualTo(NodeType.EndNode.name());
        } finally {
            WorkflowService.singleton.purge(host, workflow.id);
        }
    }

    @Test
    void createInitialDefinition_with_node_configuration_should_work() throws Exception {
        var workflow1 = new Workflow(getUuid(), "createInitialDefinition_with_node_configuration_1");
        var workflow2 = new Workflow(getUuid(), "createInitialDefinition_with_node_configuration_2");
        try {

            // test startNodeConfiguration
            {
                var creationParam = new WorkflowCreationParam();
                creationParam.name = workflow1.name;
                creationParam.returnToStartNode = false;
                creationParam.enableOperatorControl = true;
                creationParam.allowedOperatorIds.addAll(List.of("operator-2", "operator-3", "operator-4"));

                var testOperateLog = new OperateLog();
                testOperateLog.nodeId = "testA";
                creationParam.startNodeConfiguration = testOperateLog;

                var singleNode1 = new SingleNode("DEFAULT_SINGLE_NODE_NAME-1");
                singleNode1.operatorId = "operator-2";
                creationParam.nodes.add(singleNode1);

                var definition = getService().createInitialDefinition(host, workflow1, creationParam);

                var result = getService().readSuppressing404(host, definition.id);

                assertThat(result.workflowId).isEqualTo(workflow1.id);
                assertThat(result.version).isEqualTo(0);
                assertThat(result.applicationModes).hasSize(1);
                assertThat(result.applicationModes).containsExactlyInAnyOrder(ApplicationMode.SELF);
                assertThat(result.returnToStartNode).isFalse();

                assertThat(result.nodes).hasSize(3);
                assertThat(result.nodes.get(0).nodeName).isEqualTo("Start_Node");
                assertThat(result.nodes.get(0).getType()).isEqualTo(NodeType.StartNode.name());
                assertThat(result.nodes.get(0).configuration).isNotNull();
                var testConfiguration = result.nodes.get(0).configuration;
                assertThat(testConfiguration)
                        .asInstanceOf(InstanceOfAssertFactories.map(String.class, Object.class))
                        .containsEntry("nodeId", "testA");
            }

            // test endNodeConfiguration
            {
                var creationParam = new WorkflowCreationParam();
                creationParam.name = workflow2.name;
                creationParam.returnToStartNode = false;
                creationParam.enableOperatorControl = true;
                creationParam.allowedOperatorIds.addAll(List.of("operator-2", "operator-3", "operator-4"));

                creationParam.endNodeConfiguration = Map.of("reconfirm", true);

                var singleNode1 = new SingleNode("DEFAULT_SINGLE_NODE_NAME-1");
                singleNode1.operatorId = "operator-2";
                creationParam.nodes.add(singleNode1);

                var definition = getService().createInitialDefinition(host, workflow2, creationParam);

                var result = getService().readSuppressing404(host, definition.id);

                assertThat(result.workflowId).isEqualTo(workflow2.id);
                assertThat(result.version).isEqualTo(0);
                assertThat(result.applicationModes).hasSize(1);
                assertThat(result.applicationModes).containsExactlyInAnyOrder(ApplicationMode.SELF);
                assertThat(result.returnToStartNode).isFalse();

                assertThat(result.nodes).hasSize(3);
                assertThat(result.nodes.get(0).nodeName).isEqualTo("Start_Node");
                assertThat(result.nodes.get(0).getType()).isEqualTo(NodeType.StartNode.name());
                assertThat(result.nodes.get(0).configuration).isNull();
                assertThat(result.nodes.get(2).nodeName).isEqualTo("End_Node");
                assertThat(result.nodes.get(2).getType()).isEqualTo(NodeType.EndNode.name());
                assertThat(result.nodes.get(2).configuration).isNotNull();
                var testConfiguration = result.nodes.get(2).configuration;
                assertThat(testConfiguration)
                        .asInstanceOf(InstanceOfAssertFactories.map(String.class, Object.class))
                        .containsEntry("reconfirm", true);
            }
        } finally {
            WorkflowService.singleton.purge(host, workflow1.id);
            WorkflowService.singleton.purge(host, workflow2.id);
        }
    }

    @Test
    void create_should_work() throws Exception {
        var creationParam = new WorkflowCreationParam();
        creationParam.name = "definition-test-create";
        creationParam.enableOperatorControl = false;
        var workflowId = "";

        try {
            var workflow = processDesign.createWorkflow(host, creationParam);
            workflowId = workflow.getId();

            var definition = new Definition(getUuid(), workflow.getId());
            definition.enableOperatorControl = false;

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
                var result = processDesign.getDefinition(host, definition.getId());
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
            WorkflowService.singleton.purge(host, workflowId);
        }
    }

    @Test
    void upsert_should_work() throws Exception {
        var creationParam = new WorkflowCreationParam();
        creationParam.name = "upsert_should_work";
        creationParam.enableOperatorControl = false;
        var workflowId = "";
        try {
            var workflow = processDesign.createWorkflow(host, creationParam);
            workflowId = workflow.getId();

            workflow = processDesign.getWorkflow(host, workflow.getId());
            assertThat(workflow.enableVersion).isTrue();
            assertThat(workflow.currentVersion).isEqualTo(0);

            var updatingParam = new WorkflowUpdatingParam();
            updatingParam.id = workflowId;
            updatingParam.enableVersion = false;
            processDesign.updateWorkflow(host, updatingParam);

            {
                var definitions = processDesign.findDefinitions(host, Condition.filter("workflowId", workflow.id));
                assertThat(definitions).hasSize(1);
                var definition = definitions.get(0);
                assertThat(definition.nodes).hasSize(2);

                var singleNode = new SingleNode("DEFAULT_SINGLE_NODE_NAME-1");
                singleNode.operatorId = "operator-1";
                definition.nodes.add(1, singleNode);

                var param = new DefinitionParam();
                param.workflowId = workflowId;
                param.enableOperatorControl = false;
                param.nodes.addAll(definition.nodes);
                getService().upsert(host, param);

                workflow = processDesign.getWorkflow(host, workflow.getId());
                assertThat(workflow.enableVersion).isFalse();
                assertThat(workflow.currentVersion).isEqualTo(0);

                definitions = processDesign.findDefinitions(host, Condition.filter("workflowId", workflow.id));
                assertThat(definitions).hasSize(1);

                var result = processDesign.getDefinition(host, definition.id);
                assertThat(result.nodes).hasSize(3);
                assertThat(result.version).isEqualTo(0);
            }


            updatingParam.id = workflowId;
            updatingParam.enableVersion = true;
            processDesign.updateWorkflow(host, updatingParam);

            {
                var definitions = processDesign.findDefinitions(host, Condition.filter("workflowId", workflow.id));
                assertThat(definitions).hasSize(1);

                var definition = definitions.get(0);
                assertThat(definition.nodes).hasSize(3);

                var singleNode = new SingleNode("DEFAULT_SINGLE_NODE_NAME-2");
                singleNode.operatorId = "operator-2";
                definition.nodes.add(2, singleNode);

                var param = new DefinitionParam();
                param.workflowId = workflowId;
                param.enableOperatorControl = false;
                param.nodes.addAll(definition.nodes);
                getService().upsert(host, param);

                workflow = processDesign.getWorkflow(host, workflow.getId());
                assertThat(workflow.enableVersion).isTrue();
                assertThat(workflow.currentVersion).isEqualTo(1);

                definitions = processDesign.findDefinitions(host, Condition.filter("workflowId", workflow.id));
                assertThat(definitions).hasSize(2);

                var result1 = processDesign.getCurrentDefinition(host, workflow.id, 0);
                assertThat(result1.nodes).hasSize(3);
                assertThat(result1.version).isEqualTo(0);

                var result2 = processDesign.getCurrentDefinition(host, workflow.id, 1);
                assertThat(result2.nodes).hasSize(4);
                assertThat(result2.version).isEqualTo(1);
            }
        } finally {
            WorkflowService.singleton.purge(host, workflowId);
        }
    }

    @Test
    void upsertDefinition_with_node_configuration_should_work() throws Exception {
        var creationParam = new WorkflowCreationParam();
        creationParam.name = "upsertDefinition_with_node_configuration";
        creationParam.returnToStartNode = false;
        creationParam.enableOperatorControl = true;
        creationParam.enableVersion = false;
        creationParam.allowedOperatorIds.addAll(List.of("operator-1", "operator-2"));
        creationParam.endNodeConfiguration = Map.of("reconfirm", false);

        var workflowId = "";
        try {
            var workflow = processDesign.createWorkflow(host, creationParam);
            workflowId = workflow.getId();

            workflow = processDesign.getWorkflow(host, workflow.getId());
            assertThat(workflow.enableVersion).isFalse();
            assertThat(workflow.currentVersion).isZero();

            {
                var definitions = processDesign.findDefinitions(host, Condition.filter("workflowId", workflow.id));
                assertThat(definitions).hasSize(1);
                var definition = definitions.get(0);
                assertThat(definition.nodes).hasSize(2);
                assertThat(definition.nodes.get(1).nodeName).isEqualTo("End_Node");
                assertThat(definition.nodes.get(1).configuration)
                        .asInstanceOf(InstanceOfAssertFactories.map(String.class, Object.class))
                        .containsEntry("reconfirm", false);

                var singleNode = new SingleNode("DEFAULT_SINGLE_NODE_NAME-1");
                singleNode.operatorId = "operator-1";
                definition.nodes.add(1, singleNode);
                definition.nodes.get(2).configuration = Map.of("reconfirm", true);

                var param = new DefinitionParam();
                param.workflowId = workflowId;
                param.enableOperatorControl = false;
                param.nodes.addAll(definition.nodes);
                getService().upsert(host, param);

                workflow = processDesign.getWorkflow(host, workflow.getId());
                assertThat(workflow.enableVersion).isFalse();
                assertThat(workflow.currentVersion).isZero();

                definitions = processDesign.findDefinitions(host, Condition.filter("workflowId", workflow.id));
                assertThat(definitions).hasSize(1);

                var result = definitions.get(0);
                assertThat(result.nodes).hasSize(3);
                assertThat(result.version).isZero();
                assertThat(definition.nodes.get(2).nodeName).isEqualTo("End_Node");
                assertThat(definition.nodes.get(2).configuration)
                        .asInstanceOf(InstanceOfAssertFactories.map(String.class, Object.class))
                        .containsEntry("reconfirm", true);
            }
        } finally {
            WorkflowService.singleton.purge(host, workflowId);
        }
    }

    @Test
    void getDefinition_should_work() throws Exception {
        var creationParam = new WorkflowCreationParam();
        creationParam.name = "getDefinition_should_work";
        var workflowId = "";
        try {
            var workflow = processDesign.createWorkflow(host, creationParam);
            workflowId = workflow.getId();
            var definition = processDesign.getCurrentDefinition(host, workflow.id, workflow.currentVersion);
            {
                var result = processDesign.getDefinition(host, definition.getId());
                assertThat(result.id).isEqualTo(definition.id);
                assertThat(result.version).isEqualTo(definition.version);
                assertThat(result.workflowId).isEqualTo(workflow.id);
            }

            {
                assertThatThrownBy(() -> processDesign.getDefinition(host, "error-id"))
                        .isInstanceOf(WorkflowException.class)
                        .hasMessageContaining(WorkflowErrors.DEFINITION_NOT_EXIST.name());
            }
        } finally {
            WorkflowService.singleton.purge(host, workflowId);
        }
    }

    @Test
    void getCurrentDefinition_should_work() throws Exception {
        var creationParam = new WorkflowCreationParam();
        creationParam.name = "getCurrentDefinition_should_work";
        var workflowId = "";

        try {
            var workflow = processDesign.createWorkflow(host, creationParam);
            workflow = processDesign.getWorkflow(host, workflow.getId());
            workflowId = workflow.id;
            {
                var result = processDesign.getCurrentDefinition(host, workflowId, 0);
                assertThat(result.nodes).hasSize(2);
                assertThat(result.version).isEqualTo(0);
            }

            {
                String finalWorkflowId = workflowId;
                assertThatThrownBy(() -> processDesign.getCurrentDefinition(host, finalWorkflowId, 1))
                        .isInstanceOf(WorkflowException.class)
                        .hasMessageContaining(WorkflowErrors.DEFINITION_NOT_EXIST.name())
                        .hasMessageContaining("The definition for the current version of the workflow was not found");
            }
        } finally {
            WorkflowService.singleton.purge(host, workflowId);
        }
    }

    @Test
    void multipleNode_preserve_the_order_of_IdSet_should_work() throws Exception {
        var creationParam = new WorkflowCreationParam();
        creationParam.name = "multipleNode_preserve_the_order_of_IdSet_should_work";
        var workflowId = "";

        try {
            var workflow = processDesign.createWorkflow(host, creationParam);
            workflow = processDesign.getWorkflow(host, workflow.getId());
            workflowId = workflow.id;

            var definition = processDesign.getCurrentDefinition(host, workflowId, 0);

            var multipleNode1 = new MultipleNode("DEFAULT_MULTIPLE_NODE_NAME-1");
            multipleNode1.operatorIdSet.add("operator-1");
            multipleNode1.operatorIdSet.add("operator-2");
            multipleNode1.operatorIdSet.add("operator-3");
            definition.nodes.add(1, multipleNode1);

            var multipleNode2 = new MultipleNode("DEFAULT_MULTIPLE_NODE_NAME-2");
            multipleNode2.operatorIdSet.add("operator-3");
            multipleNode2.operatorIdSet.add("operator-1");
            multipleNode2.operatorIdSet.add("operator-2");
            definition.nodes.add(2, multipleNode2);

            var multipleNode3 = new MultipleNode("DEFAULT_MULTIPLE_NODE_NAME-3");
            multipleNode3.operatorIdSet.add("operator-1");
            multipleNode3.operatorIdSet.add("operator-3");
            multipleNode3.operatorIdSet.add("operator-2");
            definition.nodes.add(3, multipleNode3);

            var definitionParam = new DefinitionParam();
            definitionParam.workflowId = workflowId;
            definitionParam.enableOperatorControl = false;
            definitionParam.nodes.addAll(definition.nodes);
            processDesign.upsertDefinition(host, definitionParam);

            definition = processDesign.getCurrentDefinition(host, workflow.id, 1);

            var result1 = (MultipleNode) definition.nodes.get(1);
            var result2 = (MultipleNode) definition.nodes.get(2);
            var result3 = (MultipleNode) definition.nodes.get(3);

            assertThat(result1.operatorIdSet).containsSequence("operator-1", "operator-2", "operator-3");
            assertThat(result2.operatorIdSet).containsSequence("operator-3", "operator-1", "operator-2");
            assertThat(result3.operatorIdSet).containsSequence("operator-1", "operator-3", "operator-2");
        } finally {
            WorkflowService.singleton.purge(host, workflowId);
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

    @Test
    void definition_validations_should_work() throws Exception {
        var workflowId = "";
        try {
            var creationParam = new WorkflowCreationParam();
            creationParam.name = "definition_validations_should_work";

            var robotNode = new RobotNode("DEFAULT_ROBOT_NODE_NAME-2");
            robotNode.plugins.add("TestPlugin");
            var configMap = new HashMap<String, String>();
            configMap.put("a", "1");
            configMap.put("b", "2");
            robotNode.configuration = configMap;
            creationParam.nodes.add(robotNode);

            // create error
            assertThatThrownBy(() -> processDesign.createWorkflow(host, creationParam))
                    .isInstanceOf(WorkflowException.class)
                    .hasMessageContaining("The first node in the definition cannot be a robot node");

            creationParam.nodes.clear();

            var workflow = processDesign.createWorkflow(host, creationParam);
            workflowId = workflow.getId();

            var result = processDesign.getCurrentDefinition(host, workflow.getId(), 0);

            assertThat(result.workflowId).isEqualTo(workflow.id);
            assertThat(result.version).isEqualTo(0);
            assertThat(result.applicationModes).hasSize(1);
            assertThat(result.applicationModes).containsExactlyInAnyOrder(ApplicationMode.SELF);

            assertThat(result.nodes).hasSize(2);
            assertThat(result.nodes.get(0).nodeName).isEqualTo("Start_Node");
            assertThat(result.nodes.get(0).getType()).isEqualTo(NodeType.StartNode.name());
            assertThat(result.nodes.get(1).nodeName).isEqualTo("End_Node");
            assertThat(result.nodes.get(1).getType()).isEqualTo(NodeType.EndNode.name());

            // update error
            var definitionParam = new DefinitionParam();
            definitionParam.workflowId = workflowId;
            definitionParam.enableOperatorControl = false;
            definitionParam.applicationModes = result.applicationModes;
            definitionParam.nodes.addAll(result.nodes);
            definitionParam.nodes.add(1, robotNode);

            assertThatThrownBy(() -> processDesign.upsertDefinition(host, definitionParam))
                    .isInstanceOf(WorkflowException.class)
                    .hasMessageContaining("The first node in the definition cannot be a robot node");

            definitionParam.nodes.remove(1);
            definitionParam.nodes.add(2, robotNode);

            assertThatThrownBy(() -> processDesign.upsertDefinition(host, definitionParam))
                    .isInstanceOf(WorkflowException.class)
                    .hasMessageContaining("The last node of a workflow must be an end node");

            definitionParam.nodes.remove(2);

            var singleNode = new SingleNode("DEFAULT_SINGLE_NODE_NAME-1");
            singleNode.operatorId = "operator-1";
            definitionParam.nodes.add(1, singleNode);
            definitionParam.nodes.add(2, robotNode);

            processDesign.upsertDefinition(host, definitionParam);
            result = processDesign.getCurrentDefinition(host, workflow.getId(), 1);

            assertThat(result.workflowId).isEqualTo(workflow.id);
            assertThat(result.version).isEqualTo(1);
            assertThat(result.applicationModes).hasSize(1);
            assertThat(result.applicationModes).containsExactlyInAnyOrder(ApplicationMode.SELF);

            assertThat(result.nodes).hasSize(4);
            assertThat(result.nodes.get(0).nodeName).isEqualTo("Start_Node");
            assertThat(result.nodes.get(0).getType()).isEqualTo(NodeType.StartNode.name());
            assertThat(result.nodes.get(3).nodeName).isEqualTo("End_Node");
            assertThat(result.nodes.get(3).getType()).isEqualTo(NodeType.EndNode.name());
        } finally {
            WorkflowService.singleton.purge(host, workflowId);
        }
    }

}
