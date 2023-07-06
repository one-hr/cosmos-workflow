package jp.co.onehr.workflow.service;

import java.util.HashMap;
import java.util.Set;

import jp.co.onehr.workflow.base.BaseCRUDServiceTest;
import jp.co.onehr.workflow.constant.*;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.TestPluginParam;
import jp.co.onehr.workflow.dto.TestPluginResult;
import jp.co.onehr.workflow.dto.Workflow;
import jp.co.onehr.workflow.dto.node.MultipleNode;
import jp.co.onehr.workflow.dto.node.RobotNode;
import jp.co.onehr.workflow.dto.node.SingleNode;
import jp.co.onehr.workflow.dto.param.ActionExtendParam;
import jp.co.onehr.workflow.dto.param.ApplicationParam;
import jp.co.onehr.workflow.exception.WorkflowException;
import org.junit.jupiter.api.Test;

import static jp.co.onehr.workflow.service.DefinitionService.DEFAULT_END_NODE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class InstanceServiceTest extends BaseCRUDServiceTest<Instance, InstanceService> {

    @Override
    protected Class<Instance> getDataClass() {
        return Instance.class;
    }

    @Override
    protected InstanceService getService() {
        return InstanceService.singleton;
    }

    @Test
    void start_should_work() throws Exception {
        var workflow = new Workflow(getUuid(), "start_should_work");
        try {
            workflow = workflowEngine.createWorkflow(host, workflow);

            var definition = workflowEngine.getCurrentDefinition(host, workflow.id, 0);
            {
                var param = new ApplicationParam();
                param.workflowId = workflow.id;
                param.applicant = "operator-1";
                var instance = workflowEngine.startInstance(host, param);
                assertThat(instance.workflowId).isEqualTo(workflow.id);
                assertThat(instance.definitionId).isEqualTo(definition.id);
                assertThat(instance.operatorIdSet).isEmpty();
                assertThat(instance.operatorOrgIdSet).isEmpty();
                assertThat(instance.applicant).isEqualTo("operator-1");
                assertThat(instance.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(instance.status).isEqualTo(Status.Finished);

                var node = NodeService.getCurrentNode(definition, instance.nodeId);
                assertThat(node.getType()).isEqualTo(NodeType.EndNode.name());
                assertThat(node.nodeName).isEqualTo(DEFAULT_END_NODE_NAME);
            }

            var singleNode = new SingleNode("DEFAULT_SINGLE_NODE_NAME-1");
            singleNode.operatorId = "operator-node";
            definition.nodes.add(1, singleNode);
            workflowEngine.upsertDefinition(host, definition);

            definition = workflowEngine.getCurrentDefinition(host, workflow.id, 1);

            {
                var param = new ApplicationParam();
                param.workflowId = workflow.id;
                param.applicant = "operator-1";
                var instance = workflowEngine.startInstance(host, param);
                assertThat(instance.workflowId).isEqualTo(workflow.id);
                assertThat(instance.definitionId).isEqualTo(definition.id);
                assertThat(instance.operatorIdSet).containsExactlyInAnyOrder("operator-node");
                assertThat(instance.operatorOrgIdSet).isEmpty();
                assertThat(instance.applicant).isEqualTo("operator-1");
                assertThat(instance.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(instance.status).isEqualTo(Status.PROCESSING);

                var node = NodeService.getCurrentNode(definition, instance.nodeId);
                assertThat(node.getType()).isEqualTo(NodeType.SingleNode.name());
                assertThat(node.nodeName).isEqualTo("DEFAULT_SINGLE_NODE_NAME-1");
            }
        } finally {
            WorkflowService.singleton.purge(host, workflow.id);
        }
    }

    @Test
    void resolve_singleNode_should_work() throws Exception {
        var workflow = new Workflow(getUuid(), "resolve_singleNode_should_work");
        try {
            workflow = workflowEngine.createWorkflow(host, workflow);

            var definition = workflowEngine.getCurrentDefinition(host, workflow.id, 0);

            var singleNode1 = new SingleNode("DEFAULT_SINGLE_NODE_NAME-1");
            singleNode1.operatorId = "operator-node-1";
            definition.nodes.add(1, singleNode1);
            var singleNode2 = new SingleNode("DEFAULT_SINGLE_NODE_NAME-2");
            singleNode2.operatorId = "operator-node-2";
            definition.nodes.add(2, singleNode2);
            var singleNode3 = new SingleNode("DEFAULT_SINGLE_NODE_NAME-3");
            singleNode3.operatorId = "operator-node-3";
            definition.nodes.add(3, singleNode3);
            workflowEngine.upsertDefinition(host, definition);

            definition = workflowEngine.getCurrentDefinition(host, workflow.id, 1);

            var param = new ApplicationParam();
            param.workflowId = workflow.id;
            param.applicant = "operator-1";
            var instance = workflowEngine.startInstance(host, param);

            assertThat(instance.workflowId).isEqualTo(workflow.id);
            assertThat(instance.definitionId).isEqualTo(definition.id);
            assertThat(instance.operatorIdSet).containsExactlyInAnyOrder("operator-node-1");
            assertThat(instance.operatorOrgIdSet).isEmpty();
            assertThat(instance.expandOperatorIdSet).containsExactlyInAnyOrder("operator-node-1");
            assertThat(instance.applicant).isEqualTo("operator-1");
            assertThat(instance.applicationMode).isEqualTo(ApplicationMode.SELF);
            assertThat(instance.status).isEqualTo(Status.PROCESSING);
            assertThat(instance.nodeId).isEqualTo(singleNode1.nodeId);

            // simple next
            {
                var actionResult = workflowEngine.resolve(host, instance, Action.NEXT, "operator-node-1");
                instance = actionResult.instance;

                var result1 = workflowEngine.getInstance(host, instance.getId());
                assertThat(result1.definitionId).isEqualTo(definition.id);
                assertThat(result1.operatorIdSet).containsExactlyInAnyOrder("operator-node-2");
                assertThat(result1.operatorOrgIdSet).isEmpty();
                assertThat(result1.expandOperatorIdSet).containsExactlyInAnyOrder("operator-node-2");
                assertThat(result1.applicant).isEqualTo("operator-1");
                assertThat(result1.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result1.status).isEqualTo(Status.PROCESSING);
                assertThat(result1.nodeId).isEqualTo(singleNode2.nodeId);


                actionResult = workflowEngine.resolve(host, instance, Action.NEXT, "operator-node-2");
                instance = actionResult.instance;

                var result2 = workflowEngine.getInstance(host, instance.getId());
                assertThat(result2.definitionId).isEqualTo(definition.id);
                assertThat(result2.operatorIdSet).containsExactlyInAnyOrder("operator-node-3");
                assertThat(result2.operatorOrgIdSet).isEmpty();
                assertThat(result2.expandOperatorIdSet).containsExactlyInAnyOrder("operator-node-3");
                assertThat(result2.applicant).isEqualTo("operator-1");
                assertThat(result2.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result2.status).isEqualTo(Status.PROCESSING);
                assertThat(result2.nodeId).isEqualTo(singleNode3.nodeId);
            }

            // test back PREVIOUS
            {
                var actionResult = workflowEngine.resolve(host, instance, Action.BACK, "operator-node-3");
                instance = actionResult.instance;

                var result1 = workflowEngine.getInstance(host, instance.getId());
                assertThat(result1.definitionId).isEqualTo(definition.id);
                assertThat(result1.operatorIdSet).containsExactlyInAnyOrder("operator-node-2");
                assertThat(result1.operatorOrgIdSet).isEmpty();
                assertThat(result1.expandOperatorIdSet).containsExactlyInAnyOrder("operator-node-2");
                assertThat(result1.applicant).isEqualTo("operator-1");
                assertThat(result1.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result1.status).isEqualTo(Status.PROCESSING);
                assertThat(result1.nodeId).isEqualTo(singleNode2.nodeId);

                actionResult = workflowEngine.resolve(host, instance, Action.NEXT, "operator-node-2");
                instance = actionResult.instance;

                var result2 = workflowEngine.getInstance(host, instance.getId());
                assertThat(result2.definitionId).isEqualTo(definition.id);
                assertThat(result2.operatorIdSet).containsExactlyInAnyOrder("operator-node-3");
                assertThat(result2.operatorOrgIdSet).isEmpty();
                assertThat(result2.expandOperatorIdSet).containsExactlyInAnyOrder("operator-node-3");
                assertThat(result2.applicant).isEqualTo("operator-1");
                assertThat(result2.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result2.status).isEqualTo(Status.PROCESSING);
                assertThat(result2.nodeId).isEqualTo(singleNode3.nodeId);
            }

            // test back FIRST
            {
                var actionParam = new ActionExtendParam();
                actionParam.backMode = BackMode.FIRST;
                var actionResult = workflowEngine.resolve(host, instance, Action.BACK, "operator-node-3", actionParam);
                instance = actionResult.instance;

                var result1 = workflowEngine.getInstance(host, instance.getId());
                assertThat(result1.definitionId).isEqualTo(definition.id);
                assertThat(result1.operatorIdSet).containsExactlyInAnyOrder("operator-node-1");
                assertThat(result1.operatorOrgIdSet).isEmpty();
                assertThat(result1.expandOperatorIdSet).containsExactlyInAnyOrder("operator-node-1");
                assertThat(result1.applicant).isEqualTo("operator-1");
                assertThat(result1.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result1.status).isEqualTo(Status.PROCESSING);
                assertThat(result1.nodeId).isEqualTo(singleNode1.nodeId);
            }
        } finally {
            WorkflowService.singleton.purge(host, workflow.id);
        }
    }

    @Test
    void resolve_multipleNode_or_should_work() throws Exception {
        var workflow = new Workflow(getUuid(), "resolve_multipleNode_or_should_work");
        try {
            workflow = workflowEngine.createWorkflow(host, workflow);

            var definition = workflowEngine.getCurrentDefinition(host, workflow.id, 0);

            var multipleNode1 = new MultipleNode("DEFAULT_MULTIPLE_NODE_NAME-1", Set.of("operator-1", "operator-2"), Set.of());
            definition.nodes.add(1, multipleNode1);
            var multipleNode2 = new MultipleNode("DEFAULT_MULTIPLE_NODE_NAME-2", Set.of("operator-3", "operator-4"), Set.of());
            definition.nodes.add(2, multipleNode2);
            var multipleNode3 = new MultipleNode("DEFAULT_MULTIPLE_NODE_NAME-3", Set.of("operator-1", "operator-4"), Set.of());
            definition.nodes.add(3, multipleNode3);
            workflowEngine.upsertDefinition(host, definition);

            definition = workflowEngine.getCurrentDefinition(host, workflow.id, 1);

            var param = new ApplicationParam();
            param.workflowId = workflow.id;
            param.applicant = "operator-0";
            var instance = workflowEngine.startInstance(host, param);

            assertThat(instance.workflowId).isEqualTo(workflow.id);
            assertThat(instance.definitionId).isEqualTo(definition.id);
            assertThat(instance.operatorIdSet).containsExactlyInAnyOrder("operator-1", "operator-2");
            assertThat(instance.operatorOrgIdSet).isEmpty();
            assertThat(instance.expandOperatorIdSet).containsExactlyInAnyOrder("operator-1", "operator-2");
            assertThat(instance.applicant).isEqualTo("operator-0");
            assertThat(instance.applicationMode).isEqualTo(ApplicationMode.SELF);
            assertThat(instance.status).isEqualTo(Status.PROCESSING);
            assertThat(instance.nodeId).isEqualTo(multipleNode1.nodeId);

            // or next
            {
                var actionResult = workflowEngine.resolve(host, instance, Action.NEXT, "operator-1");
                instance = actionResult.instance;

                var result1 = workflowEngine.getInstance(host, instance.getId());
                assertThat(result1.definitionId).isEqualTo(definition.id);
                assertThat(result1.operatorIdSet).containsExactlyInAnyOrder("operator-3", "operator-4");
                assertThat(result1.operatorOrgIdSet).isEmpty();
                assertThat(result1.expandOperatorIdSet).containsExactlyInAnyOrder("operator-3", "operator-4");
                assertThat(result1.applicant).isEqualTo("operator-0");
                assertThat(result1.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result1.status).isEqualTo(Status.PROCESSING);
                assertThat(result1.nodeId).isEqualTo(multipleNode2.nodeId);


                actionResult = workflowEngine.resolve(host, instance, Action.NEXT, "operator-3");
                instance = actionResult.instance;

                var result2 = workflowEngine.getInstance(host, instance.getId());
                assertThat(result2.definitionId).isEqualTo(definition.id);
                assertThat(result2.operatorIdSet).containsExactlyInAnyOrder("operator-1", "operator-4");
                assertThat(result2.operatorOrgIdSet).isEmpty();
                assertThat(result2.expandOperatorIdSet).containsExactlyInAnyOrder("operator-1", "operator-4");
                assertThat(result2.applicant).isEqualTo("operator-0");
                assertThat(result2.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result2.status).isEqualTo(Status.PROCESSING);
                assertThat(result2.nodeId).isEqualTo(multipleNode3.nodeId);
            }

            // test or back PREVIOUS
            {
                var actionResult = workflowEngine.resolve(host, instance, Action.BACK, "operator-4");
                instance = actionResult.instance;

                var result1 = workflowEngine.getInstance(host, instance.getId());
                assertThat(result1.definitionId).isEqualTo(definition.id);
                assertThat(result1.operatorIdSet).containsExactlyInAnyOrder("operator-3", "operator-4");
                assertThat(result1.operatorOrgIdSet).isEmpty();
                assertThat(result1.expandOperatorIdSet).containsExactlyInAnyOrder("operator-3", "operator-4");
                assertThat(result1.applicant).isEqualTo("operator-0");
                assertThat(result1.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result1.status).isEqualTo(Status.PROCESSING);
                assertThat(result1.nodeId).isEqualTo(multipleNode2.nodeId);

                actionResult = workflowEngine.resolve(host, instance, Action.NEXT, "operator-3");
                instance = actionResult.instance;

                var result2 = workflowEngine.getInstance(host, instance.getId());
                assertThat(result2.definitionId).isEqualTo(definition.id);
                assertThat(result2.operatorIdSet).containsExactlyInAnyOrder("operator-1", "operator-4");
                assertThat(result2.operatorOrgIdSet).isEmpty();
                assertThat(result2.expandOperatorIdSet).containsExactlyInAnyOrder("operator-1", "operator-4");
                assertThat(result2.applicant).isEqualTo("operator-0");
                assertThat(result2.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result2.status).isEqualTo(Status.PROCESSING);
                assertThat(result2.nodeId).isEqualTo(multipleNode3.nodeId);
            }

            // test or back FIRST
            {
                var actionParam = new ActionExtendParam();
                actionParam.backMode = BackMode.FIRST;
                var actionResult = workflowEngine.resolve(host, instance, Action.BACK, "operator-4", actionParam);
                instance = actionResult.instance;

                var result1 = workflowEngine.getInstance(host, instance.getId());
                assertThat(result1.definitionId).isEqualTo(definition.id);
                assertThat(result1.operatorIdSet).containsExactlyInAnyOrder("operator-1", "operator-2");
                assertThat(result1.operatorOrgIdSet).isEmpty();
                assertThat(result1.expandOperatorIdSet).containsExactlyInAnyOrder("operator-1", "operator-2");
                assertThat(result1.applicant).isEqualTo("operator-0");
                assertThat(result1.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result1.status).isEqualTo(Status.PROCESSING);
                assertThat(result1.nodeId).isEqualTo(multipleNode1.nodeId);
            }
        } finally {
            WorkflowService.singleton.purge(host, workflow.id);
        }
    }

    @Test
    void resolve_multipleNode_and_should_work() throws Exception {
        var workflow = new Workflow(getUuid(), "resolve_multipleNode_or_should_work");
        try {
            workflow = workflowEngine.createWorkflow(host, workflow);

            var definition = workflowEngine.getCurrentDefinition(host, workflow.id, 0);

            var multipleNode1 = new MultipleNode("DEFAULT_MULTIPLE_NODE_NAME-1", ApprovalType.AND, Set.of("operator-1", "operator-2"), Set.of());
            definition.nodes.add(1, multipleNode1);
            var multipleNode2 = new MultipleNode("DEFAULT_MULTIPLE_NODE_NAME-2", ApprovalType.AND, Set.of("operator-3", "operator-4"), Set.of());
            definition.nodes.add(2, multipleNode2);
            var multipleNode3 = new MultipleNode("DEFAULT_MULTIPLE_NODE_NAME-3", ApprovalType.AND, Set.of("operator-1", "operator-4"), Set.of());
            definition.nodes.add(3, multipleNode3);
            workflowEngine.upsertDefinition(host, definition);

            definition = workflowEngine.getCurrentDefinition(host, workflow.id, 1);

            var param = new ApplicationParam();
            param.workflowId = workflow.id;
            param.applicant = "operator-0";
            var instance = workflowEngine.startInstance(host, param);

            assertThat(instance.workflowId).isEqualTo(workflow.id);
            assertThat(instance.definitionId).isEqualTo(definition.id);
            assertThat(instance.operatorIdSet).containsExactlyInAnyOrder("operator-1", "operator-2");
            assertThat(instance.operatorOrgIdSet).isEmpty();
            assertThat(instance.expandOperatorIdSet).containsExactlyInAnyOrder("operator-1", "operator-2");
            assertThat(instance.applicant).isEqualTo("operator-0");
            assertThat(instance.applicationMode).isEqualTo(ApplicationMode.SELF);
            assertThat(instance.status).isEqualTo(Status.PROCESSING);
            assertThat(instance.nodeId).isEqualTo(multipleNode1.nodeId);

            // and next
            {
                var actionResult = workflowEngine.resolve(host, instance, Action.NEXT, "operator-1");
                instance = actionResult.instance;

                var result1 = workflowEngine.getInstance(host, instance.getId());
                assertThat(result1.definitionId).isEqualTo(definition.id);
                assertThat(result1.operatorIdSet).containsExactlyInAnyOrder("operator-1", "operator-2");
                assertThat(result1.operatorOrgIdSet).isEmpty();
                assertThat(result1.expandOperatorIdSet).containsExactlyInAnyOrder("operator-1", "operator-2");
                assertThat(result1.applicant).isEqualTo("operator-0");
                assertThat(result1.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result1.status).isEqualTo(Status.PROCESSING);

                assertThat(result1.nodeId).isEqualTo(multipleNode1.nodeId);

                assertThat(result1.parallelApproval).hasSize(2);
                assertThat(result1.parallelApproval.get("operator-1").operatorId).isEqualTo("operator-1");
                assertThat(result1.parallelApproval.get("operator-1").approved).isTrue();
                assertThat(result1.parallelApproval.get("operator-2").operatorId).isEqualTo("operator-2");
                assertThat(result1.parallelApproval.get("operator-2").approved).isFalse();

                // the second operator confirms the action.
                actionResult = workflowEngine.resolve(host, instance, Action.NEXT, "operator-2");
                instance = actionResult.instance;

                result1 = workflowEngine.getInstance(host, instance.getId());
                assertThat(result1.definitionId).isEqualTo(definition.id);
                assertThat(result1.operatorIdSet).containsExactlyInAnyOrder("operator-3", "operator-4");
                assertThat(result1.operatorOrgIdSet).isEmpty();
                assertThat(result1.expandOperatorIdSet).containsExactlyInAnyOrder("operator-3", "operator-4");
                assertThat(result1.applicant).isEqualTo("operator-0");
                assertThat(result1.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result1.status).isEqualTo(Status.PROCESSING);

                assertThat(result1.nodeId).isEqualTo(multipleNode2.nodeId);

                assertThat(result1.parallelApproval).hasSize(2);
                assertThat(result1.parallelApproval.get("operator-3").operatorId).isEqualTo("operator-3");
                assertThat(result1.parallelApproval.get("operator-3").approved).isFalse();
                assertThat(result1.parallelApproval.get("operator-4").operatorId).isEqualTo("operator-4");
                assertThat(result1.parallelApproval.get("operator-4").approved).isFalse();

                // moving to the third node
                actionResult = workflowEngine.resolve(host, instance, Action.NEXT, "operator-3");
                instance = actionResult.instance;

                var result2 = workflowEngine.getInstance(host, instance.getId());
                assertThat(result2.definitionId).isEqualTo(definition.id);
                assertThat(result2.operatorIdSet).containsExactlyInAnyOrder("operator-3", "operator-4");
                assertThat(result2.operatorOrgIdSet).isEmpty();
                assertThat(result2.expandOperatorIdSet).containsExactlyInAnyOrder("operator-3", "operator-4");
                assertThat(result2.applicant).isEqualTo("operator-0");
                assertThat(result2.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result2.status).isEqualTo(Status.PROCESSING);

                assertThat(result2.nodeId).isEqualTo(multipleNode2.nodeId);

                assertThat(result2.parallelApproval).hasSize(2);
                assertThat(result2.parallelApproval.get("operator-3").operatorId).isEqualTo("operator-3");
                assertThat(result2.parallelApproval.get("operator-3").approved).isTrue();
                assertThat(result2.parallelApproval.get("operator-4").operatorId).isEqualTo("operator-4");
                assertThat(result2.parallelApproval.get("operator-4").approved).isFalse();

                // The save action does not clear the approval status
                actionResult = workflowEngine.resolve(host, instance, Action.SAVE, "operator-3");
                instance = actionResult.instance;

                result1 = workflowEngine.getInstance(host, instance.getId());
                assertThat(result2.definitionId).isEqualTo(definition.id);
                assertThat(result2.operatorIdSet).containsExactlyInAnyOrder("operator-3", "operator-4");
                assertThat(result2.operatorOrgIdSet).isEmpty();
                assertThat(result2.expandOperatorIdSet).containsExactlyInAnyOrder("operator-3", "operator-4");
                assertThat(result2.applicant).isEqualTo("operator-0");
                assertThat(result2.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result2.status).isEqualTo(Status.PROCESSING);

                assertThat(result2.nodeId).isEqualTo(multipleNode2.nodeId);

                assertThat(result2.parallelApproval).hasSize(2);
                assertThat(result2.parallelApproval.get("operator-3").operatorId).isEqualTo("operator-3");
                assertThat(result2.parallelApproval.get("operator-3").approved).isTrue();
                assertThat(result2.parallelApproval.get("operator-4").operatorId).isEqualTo("operator-4");
                assertThat(result2.parallelApproval.get("operator-4").approved).isFalse();

                actionResult = workflowEngine.resolve(host, instance, Action.NEXT, "operator-4");
                instance = actionResult.instance;

                var result3 = workflowEngine.getInstance(host, instance.getId());
                assertThat(result3.definitionId).isEqualTo(definition.id);
                assertThat(result3.operatorIdSet).containsExactlyInAnyOrder("operator-1", "operator-4");
                assertThat(result3.operatorOrgIdSet).isEmpty();
                assertThat(result3.expandOperatorIdSet).containsExactlyInAnyOrder("operator-1", "operator-4");
                assertThat(result3.applicant).isEqualTo("operator-0");
                assertThat(result3.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result3.status).isEqualTo(Status.PROCESSING);
                assertThat(result3.nodeId).isEqualTo(multipleNode3.nodeId);

                assertThat(result3.nodeId).isEqualTo(multipleNode3.nodeId);

                assertThat(result3.parallelApproval).hasSize(2);
                assertThat(result3.parallelApproval.get("operator-1").operatorId).isEqualTo("operator-1");
                assertThat(result3.parallelApproval.get("operator-1").approved).isFalse();
                assertThat(result3.parallelApproval.get("operator-4").operatorId).isEqualTo("operator-4");
                assertThat(result3.parallelApproval.get("operator-4").approved).isFalse();
            }

            // test and back PREVIOUS
            {
                var actionResult = workflowEngine.resolve(host, instance, Action.BACK, "operator-4");
                instance = actionResult.instance;

                var result1 = workflowEngine.getInstance(host, instance.getId());
                assertThat(result1.definitionId).isEqualTo(definition.id);
                assertThat(result1.operatorIdSet).containsExactlyInAnyOrder("operator-3", "operator-4");
                assertThat(result1.operatorOrgIdSet).isEmpty();
                assertThat(result1.expandOperatorIdSet).containsExactlyInAnyOrder("operator-3", "operator-4");
                assertThat(result1.applicant).isEqualTo("operator-0");
                assertThat(result1.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result1.status).isEqualTo(Status.PROCESSING);
                assertThat(result1.nodeId).isEqualTo(multipleNode2.nodeId);

                actionResult = workflowEngine.resolve(host, instance, Action.NEXT, "operator-3");
                instance = actionResult.instance;

                var result2 = workflowEngine.getInstance(host, instance.getId());
                assertThat(result2.definitionId).isEqualTo(definition.id);
                assertThat(result2.operatorIdSet).containsExactlyInAnyOrder("operator-3", "operator-4");
                assertThat(result2.operatorOrgIdSet).isEmpty();
                assertThat(result2.expandOperatorIdSet).containsExactlyInAnyOrder("operator-3", "operator-4");
                assertThat(result2.applicant).isEqualTo("operator-0");
                assertThat(result2.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result2.status).isEqualTo(Status.PROCESSING);
                assertThat(result2.nodeId).isEqualTo(multipleNode2.nodeId);

                actionResult = workflowEngine.resolve(host, instance, Action.NEXT, "operator-4");
                instance = actionResult.instance;

                result2 = workflowEngine.getInstance(host, instance.getId());
                assertThat(result2.definitionId).isEqualTo(definition.id);
                assertThat(result2.operatorIdSet).containsExactlyInAnyOrder("operator-1", "operator-4");
                assertThat(result2.operatorOrgIdSet).isEmpty();
                assertThat(result2.expandOperatorIdSet).containsExactlyInAnyOrder("operator-1", "operator-4");
                assertThat(result2.applicant).isEqualTo("operator-0");
                assertThat(result2.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result2.status).isEqualTo(Status.PROCESSING);
                assertThat(result2.nodeId).isEqualTo(multipleNode3.nodeId);
            }

            // test and back FIRST
            {
                var actionParam = new ActionExtendParam();
                actionParam.backMode = BackMode.FIRST;
                var actionResult = workflowEngine.resolve(host, instance, Action.BACK, "operator-4", actionParam);
                instance = actionResult.instance;

                var result1 = workflowEngine.getInstance(host, instance.getId());
                assertThat(result1.definitionId).isEqualTo(definition.id);
                assertThat(result1.operatorIdSet).containsExactlyInAnyOrder("operator-1", "operator-2");
                assertThat(result1.operatorOrgIdSet).isEmpty();
                assertThat(result1.expandOperatorIdSet).containsExactlyInAnyOrder("operator-1", "operator-2");
                assertThat(result1.applicant).isEqualTo("operator-0");
                assertThat(result1.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result1.status).isEqualTo(Status.PROCESSING);
                assertThat(result1.nodeId).isEqualTo(multipleNode1.nodeId);
            }
        } finally {
            WorkflowService.singleton.purge(host, workflow.id);
        }
    }

    @Test
    void resolve_robotNode_should_work() throws Exception {
        var workflow = new Workflow(getUuid(), "resolve_multipleNode_or_should_work");
        try {
            workflow = workflowEngine.createWorkflow(host, workflow);
            var definition = workflowEngine.getCurrentDefinition(host, workflow.id, 0);

            var singleNode1 = new SingleNode("DEFAULT_SINGLE_NODE_NAME-1");
            singleNode1.operatorId = "operator-node-1";
            definition.nodes.add(1, singleNode1);

            var robotNode = new RobotNode("DEFAULT_ROBOT_NODE_NAME-2");
            robotNode.plugins.add("TestPlugin");
            var configMap = new HashMap<String, String>();
            configMap.put("a", "1");
            configMap.put("b", "2");
            robotNode.configuration = configMap;
            definition.nodes.add(2, robotNode);

            var singleNode3 = new SingleNode("DEFAULT_SINGLE_NODE_NAME-3");
            singleNode3.operatorId = "operator-node-3";
            definition.nodes.add(3, singleNode3);
            workflowEngine.upsertDefinition(host, definition);

            definition = workflowEngine.getCurrentDefinition(host, workflow.id, 1);

            var param = new ApplicationParam();
            param.workflowId = workflow.id;
            param.applicant = "operator-1";
            var instance = workflowEngine.startInstance(host, param);

            assertThat(instance.workflowId).isEqualTo(workflow.id);
            assertThat(instance.definitionId).isEqualTo(definition.id);
            assertThat(instance.operatorIdSet).containsExactlyInAnyOrder("operator-node-1");
            assertThat(instance.operatorOrgIdSet).isEmpty();
            assertThat(instance.expandOperatorIdSet).containsExactlyInAnyOrder("operator-node-1");
            assertThat(instance.applicant).isEqualTo("operator-1");
            assertThat(instance.applicationMode).isEqualTo(ApplicationMode.SELF);
            assertThat(instance.status).isEqualTo(Status.PROCESSING);
            assertThat(instance.nodeId).isEqualTo(singleNode1.nodeId);

            // next can use plugins
            {
                var extendParam = new ActionExtendParam();
                var testPluginParam = new TestPluginParam();
                testPluginParam.num = 10;
                testPluginParam.str = "test";
                extendParam.pluginParam = testPluginParam;

                var actionResult = workflowEngine.resolve(host, instance, Action.NEXT, "operator-node-1", extendParam);
                var pluginResult = actionResult.pluginResult;
                assertThat(pluginResult).isNotEmpty();
                var testPluginResult = (TestPluginResult) pluginResult.get("TestPlugin");
                assertThat(testPluginResult.nodeType).isEqualTo("RobotNode");
                assertThat(testPluginResult.resultStr).isEqualTo("test");
                assertThat(testPluginResult.resultNum).isEqualTo(10);
                assertThat(testPluginResult.resultMap).hasSize(2);
                assertThat(testPluginResult.resultMap).containsEntry("a", "1");
                assertThat(testPluginResult.resultMap).containsEntry("b", "2");

                instance = actionResult.instance;

                var result1 = workflowEngine.getInstance(host, instance.getId());
                assertThat(result1.definitionId).isEqualTo(definition.id);
                assertThat(result1.operatorIdSet).containsExactlyInAnyOrder("operator-node-3");
                assertThat(result1.operatorOrgIdSet).isEmpty();
                assertThat(result1.expandOperatorIdSet).containsExactlyInAnyOrder("operator-node-3");
                assertThat(result1.applicant).isEqualTo("operator-1");
                assertThat(result1.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result1.status).isEqualTo(Status.PROCESSING);
                assertThat(result1.nodeId).isEqualTo(singleNode3.nodeId);
            }

            // back cannot use plugins
            {
                var extendParam = new ActionExtendParam();
                var testPluginParam = new TestPluginParam();
                testPluginParam.num = 20;
                testPluginParam.str = "test-back";
                extendParam.pluginParam = testPluginParam;

                var actionResult = workflowEngine.resolve(host, instance, Action.BACK, "operator-node-3", extendParam);
                var pluginResult = actionResult.pluginResult;
                assertThat(pluginResult).isEmpty();

                var result1 = workflowEngine.getInstance(host, instance.getId());
                assertThat(result1.definitionId).isEqualTo(definition.id);
                assertThat(result1.operatorIdSet).containsExactlyInAnyOrder("operator-node-1");
                assertThat(result1.operatorOrgIdSet).isEmpty();
                assertThat(result1.expandOperatorIdSet).containsExactlyInAnyOrder("operator-node-1");
                assertThat(result1.applicant).isEqualTo("operator-1");
                assertThat(result1.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result1.status).isEqualTo(Status.PROCESSING);
                assertThat(result1.nodeId).isEqualTo(singleNode1.nodeId);
            }
        } finally {
            WorkflowService.singleton.purge(host, workflow.id);
        }
    }

    @Test
    void resolve_allowingActions_should_work() throws Exception {
        var workflow = new Workflow(getUuid(), "resolve_allowingActions_should_work");
        try {
            workflow = workflowEngine.createWorkflow(host, workflow);
            var definition = workflowEngine.getCurrentDefinition(host, workflow.id, 0);

            var singleNode1 = new SingleNode("DEFAULT_SINGLE_NODE_NAME-1");
            singleNode1.operatorId = "operator-1";
            definition.nodes.add(1, singleNode1);

            var multipleNode2 = new MultipleNode("DEFAULT_MULTIPLE_NODE_NAME-2", ApprovalType.OR, Set.of("operator-3", "operator-4"), Set.of());
            definition.nodes.add(2, multipleNode2);

            workflowEngine.upsertDefinition(host, definition);

            definition = workflowEngine.getCurrentDefinition(host, workflow.id, 1);

            var param = new ApplicationParam();
            param.workflowId = workflow.id;
            param.applicant = "operator-1";
            var instance = workflowEngine.startInstance(host, param);

            assertThat(instance.workflowId).isEqualTo(workflow.id);
            assertThat(instance.definitionId).isEqualTo(definition.id);
            assertThat(instance.operatorIdSet).containsExactlyInAnyOrder("operator-1");
            assertThat(instance.operatorOrgIdSet).isEmpty();
            assertThat(instance.expandOperatorIdSet).containsExactlyInAnyOrder("operator-1");
            assertThat(instance.applicant).isEqualTo("operator-1");
            assertThat(instance.applicationMode).isEqualTo(ApplicationMode.SELF);
            assertThat(instance.status).isEqualTo(Status.PROCESSING);
            assertThat(instance.nodeId).isEqualTo(singleNode1.nodeId);

            {
                var result = workflowEngine.getInstanceWithOps(host, instance.getId(), "operator-1");
                assertThat(result.allowingActions).containsExactlyInAnyOrder(Action.NEXT);

                assertThatThrownBy(() -> workflowEngine.resolve(host, instance, Action.BACK, "operator-1"))
                        .isInstanceOf(WorkflowException.class)
                        .hasMessageContaining(WorkflowErrors.NODE_ACTION_INVALID.name());

                assertThatThrownBy(() -> workflowEngine.resolve(host, instance, Action.NEXT, "operator-2"))
                        .isInstanceOf(WorkflowException.class)
                        .hasMessageContaining(WorkflowErrors.NODE_ACTION_INVALID.name());

                assertThatThrownBy(() -> workflowEngine.resolve(host, instance, Action.SAVE, "operator-1"))
                        .isInstanceOf(WorkflowException.class)
                        .hasMessageContaining(WorkflowErrors.NODE_ACTION_INVALID.name());

                workflowEngine.resolve(host, instance, Action.NEXT, "operator-1");

                var result2 = workflowEngine.getInstanceWithOps(host, instance.getId(), "operator-1");
                assertThat(result2.definitionId).isEqualTo(definition.id);
                assertThat(result2.nodeId).isEqualTo(multipleNode2.nodeId);
                assertThat(result2.operatorIdSet).containsExactlyInAnyOrder("operator-3", "operator-4");
                assertThat(result2.operatorOrgIdSet).isEmpty();
                assertThat(result2.expandOperatorIdSet).containsExactlyInAnyOrder("operator-3", "operator-4");
                assertThat(result2.applicant).isEqualTo("operator-1");
                assertThat(result2.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result2.status).isEqualTo(Status.PROCESSING);
                assertThat(result2.allowingActions).isEmpty();

                var result3 = workflowEngine.getInstanceWithOps(host, instance.getId(), "operator-3");
                assertThat(result3.definitionId).isEqualTo(definition.id);
                assertThat(result3.nodeId).isEqualTo(multipleNode2.nodeId);
                assertThat(result3.operatorIdSet).containsExactlyInAnyOrder("operator-3", "operator-4");
                assertThat(result3.operatorOrgIdSet).isEmpty();
                assertThat(result3.expandOperatorIdSet).containsExactlyInAnyOrder("operator-3", "operator-4");
                assertThat(result3.applicant).isEqualTo("operator-1");
                assertThat(result3.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result3.status).isEqualTo(Status.PROCESSING);
                assertThat(result3.allowingActions).containsExactlyInAnyOrder(Action.NEXT, Action.BACK, Action.SAVE);

                workflowEngine.resolve(host, instance, Action.NEXT, "operator-3");
            }

            {
                var result = workflowEngine.getInstanceWithOps(host, instance.getId(), "operator-1");
                assertThat(result.allowingActions).containsExactlyInAnyOrder(Action.SAVE, Action.BACK);

                var result2 = workflowEngine.getInstanceWithOps(host, instance.getId(), "operator-3");
                assertThat(result.allowingActions).containsExactlyInAnyOrder(Action.SAVE, Action.BACK);

                workflowEngine.resolve(host, instance, Action.BACK, "operator-1");

                var result3 = workflowEngine.getInstanceWithOps(host, instance.getId(), "operator-3");
                assertThat(result3.definitionId).isEqualTo(definition.id);
                assertThat(result3.nodeId).isEqualTo(multipleNode2.nodeId);
                assertThat(result3.operatorIdSet).containsExactlyInAnyOrder("operator-3", "operator-4");
                assertThat(result3.operatorOrgIdSet).isEmpty();
                assertThat(result3.expandOperatorIdSet).containsExactlyInAnyOrder("operator-3", "operator-4");
                assertThat(result3.applicant).isEqualTo("operator-1");
                assertThat(result3.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result3.status).isEqualTo(Status.PROCESSING);
                assertThat(result3.allowingActions).containsExactlyInAnyOrder(Action.NEXT, Action.BACK, Action.SAVE);
            }
        } finally {
            WorkflowService.singleton.purge(host, workflow.id);
        }
    }

}
