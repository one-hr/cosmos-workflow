package jp.co.onehr.workflow.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jp.co.onehr.workflow.base.BaseCRUDServiceTest;
import jp.co.onehr.workflow.constant.*;
import jp.co.onehr.workflow.contract.notification.TestNotification;
import jp.co.onehr.workflow.contract.operator.TestBusinessParam;
import jp.co.onehr.workflow.contract.plugin.TestPluginParam;
import jp.co.onehr.workflow.contract.plugin.TestPluginResult;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.Workflow;
import jp.co.onehr.workflow.dto.node.MultipleNode;
import jp.co.onehr.workflow.dto.node.RobotNode;
import jp.co.onehr.workflow.dto.node.SingleNode;
import jp.co.onehr.workflow.dto.param.ActionExtendParam;
import jp.co.onehr.workflow.dto.param.ApplicationParam;
import jp.co.onehr.workflow.exception.WorkflowException;
import org.junit.jupiter.api.Test;

import static jp.co.onehr.workflow.contract.operator.TestOperatorService.SKIP_OPERATOR;
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
            workflow = processEngine.createWorkflow(host, workflow);

            var definition = processEngine.getCurrentDefinition(host, workflow.id, 0);
            {
                var param = new ApplicationParam();
                param.workflowId = workflow.id;
                param.applicant = "operator-1";
                var instance = processEngine.startInstance(host, param);
                assertThat(instance.workflowId).isEqualTo(workflow.id);
                assertThat(instance.definitionId).isEqualTo(definition.id);
                assertThat(instance.operatorIdSet).isEmpty();
                assertThat(instance.operatorOrgIdSet).isEmpty();
                assertThat(instance.applicant).isEqualTo("operator-1");
                assertThat(instance.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(instance.status).isEqualTo(Status.APPROVED);

                var node = NodeService.getNodeByNodeId(definition, instance.nodeId);
                assertThat(node.getType()).isEqualTo(NodeType.EndNode.name());
                assertThat(node.nodeName).isEqualTo(DEFAULT_END_NODE_NAME);
            }

            var singleNode = new SingleNode("DEFAULT_SINGLE_NODE_NAME-1");
            singleNode.operatorId = "operator-node";
            definition.nodes.add(1, singleNode);
            processEngine.upsertDefinition(host, definition);

            definition = processEngine.getCurrentDefinition(host, workflow.id, 1);

            {
                var businessParam = new TestBusinessParam();
                businessParam.operator = Map.of("operator-1-name", "operator-1-name-value");

                var param = new ApplicationParam();
                param.workflowId = workflow.id;
                param.applicant = "operator-1";
                param.businessParam = businessParam;
                param.comment = "operator-1-comment";
                var instance = processEngine.startInstance(host, param);
                assertThat(instance.workflowId).isEqualTo(workflow.id);
                assertThat(instance.definitionId).isEqualTo(definition.id);
                assertThat(instance.operatorIdSet).containsExactlyInAnyOrder("operator-node");
                assertThat(instance.operatorOrgIdSet).isEmpty();
                assertThat(instance.applicant).isEqualTo("operator-1");
                assertThat(instance.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(instance.status).isEqualTo(Status.PROCESSING);

                var operateLogList = instance.operateLogList;
                assertThat(operateLogList).hasSize(1);

                var firstNodeOperateLog = operateLogList.get(0);
                var firstNode = NodeService.getFirstNode(definition);
                assertThat(firstNodeOperateLog.nodeId).isEqualTo(firstNode.nodeId);
                assertThat(firstNodeOperateLog.nodeName).isEqualTo(firstNode.nodeName);
                assertThat(firstNodeOperateLog.nodeType).isEqualTo(firstNode.getType());
                assertThat(firstNodeOperateLog.statusBefore).isEqualTo(Status.NEW);
                assertThat(firstNodeOperateLog.action).isEqualTo(Action.APPLY);
                assertThat(firstNodeOperateLog.statusAfter).isEqualTo(Status.PROCESSING);
                assertThat(firstNodeOperateLog.comment).isEqualTo(param.comment);
                assertThat(firstNodeOperateLog.businessParam).isNotNull();
                assertThat(((TestBusinessParam) firstNodeOperateLog.businessParam).operator).isNotEmpty();
                assertThat(((TestBusinessParam) firstNodeOperateLog.businessParam).operator).containsEntry("operator-1-name", "operator-1-name-value");

                var node = NodeService.getNodeByNodeId(definition, instance.nodeId);
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
            workflow = processEngine.createWorkflow(host, workflow);

            var definition = processEngine.getCurrentDefinition(host, workflow.id, 0);

            var singleNode1 = new SingleNode("DEFAULT_SINGLE_NODE_NAME-1");
            singleNode1.operatorId = "operator-node-1";
            definition.nodes.add(1, singleNode1);
            var singleNode2 = new SingleNode("DEFAULT_SINGLE_NODE_NAME-2");
            singleNode2.operatorId = "operator-node-2";
            definition.nodes.add(2, singleNode2);
            var singleNode3 = new SingleNode("DEFAULT_SINGLE_NODE_NAME-3");
            singleNode3.operatorId = "operator-node-3";
            definition.nodes.add(3, singleNode3);
            processEngine.upsertDefinition(host, definition);

            definition = processEngine.getCurrentDefinition(host, workflow.id, 1);

            var param = new ApplicationParam();
            param.workflowId = workflow.id;
            param.applicant = "operator-1";
            param.comment = "apply comment";
            var instance = processEngine.startInstance(host, param);

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
                var businessParam = new TestBusinessParam();
                businessParam.operator = Map.of("operator-node-1-name", "operator-node-1-name-value");

                ActionExtendParam extendParam = new ActionExtendParam();
                extendParam.comment = "operator-node-1-comment";
                extendParam.businessParam = businessParam;

                var actionResult = processEngine.resolve(host, instance.getId(), Action.NEXT, "operator-node-1", extendParam);
                instance = actionResult.instance;

                var result1 = processEngine.getInstance(host, instance.getId());
                assertThat(result1.definitionId).isEqualTo(definition.id);
                assertThat(result1.operatorIdSet).containsExactlyInAnyOrder("operator-node-2");
                assertThat(result1.operatorOrgIdSet).isEmpty();
                assertThat(result1.expandOperatorIdSet).containsExactlyInAnyOrder("operator-node-2");
                assertThat(result1.applicant).isEqualTo("operator-1");
                assertThat(result1.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result1.status).isEqualTo(Status.PROCESSING);
                assertThat(result1.nodeId).isEqualTo(singleNode2.nodeId);

                // operate operateLog
                var operateLogList = result1.operateLogList;
                assertThat(operateLogList).hasSize(2);

                var firstNode = NodeService.getFirstNode(definition);
                var operateLog1 = operateLogList.get(0);
                assertThat(operateLog1).isNotNull();
                assertThat(operateLog1.nodeId).isEqualTo(firstNode.nodeId);
                assertThat(operateLog1.nodeName).isEqualTo(firstNode.nodeName);
                assertThat(operateLog1.statusBefore).isEqualTo(Status.NEW);
                assertThat(operateLog1.operatorId).isEqualTo("operator-1");
                assertThat(operateLog1.action).isEqualTo(Action.APPLY);
                assertThat(operateLog1.statusAfter).isEqualTo(Status.PROCESSING);
                assertThat(operateLog1.comment).isEqualTo("apply comment");
                assertThat(operateLog1.businessParam).isNull();

                var operateLog2 = operateLogList.get(1);
                assertThat(operateLog2).isNotNull();
                assertThat(operateLog2.nodeId).isEqualTo(definition.nodes.get(2).nodeId);
                assertThat(operateLog2.nodeName).isEqualTo(definition.nodes.get(2).nodeName);
                assertThat(operateLog2.statusBefore).isEqualTo(Status.PROCESSING);
                assertThat(operateLog2.operatorId).isEqualTo("operator-node-1");
                assertThat(operateLog2.action).isEqualTo(Action.NEXT);
                assertThat(operateLog2.statusAfter).isEqualTo(Status.PROCESSING);
                assertThat(operateLog2.comment).isEqualTo(extendParam.comment);
                assertThat(operateLog2.businessParam).isNotNull();
                assertThat(((TestBusinessParam) operateLog2.businessParam).operator).isNotEmpty();
                assertThat(((TestBusinessParam) operateLog2.businessParam).operator).containsEntry("operator-node-1-name", "operator-node-1-name-value");

                actionResult = processEngine.resolve(host, instance.getId(), Action.NEXT, "operator-node-2");
                instance = actionResult.instance;

                var result2 = processEngine.getInstance(host, instance.getId());
                assertThat(result2.definitionId).isEqualTo(definition.id);
                assertThat(result2.operatorIdSet).containsExactlyInAnyOrder("operator-node-3");
                assertThat(result2.operatorOrgIdSet).isEmpty();
                assertThat(result2.expandOperatorIdSet).containsExactlyInAnyOrder("operator-node-3");
                assertThat(result2.applicant).isEqualTo("operator-1");
                assertThat(result2.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result2.status).isEqualTo(Status.PROCESSING);
                assertThat(result2.nodeId).isEqualTo(singleNode3.nodeId);

                // operate operateLog
                assertThat(result2.operateLogList).hasSize(3);
                var operateLog3 = result2.operateLogList.get(2);
                assertThat(operateLog3).isNotNull();
                assertThat(operateLog3.nodeId).isEqualTo(definition.nodes.get(3).nodeId);
                assertThat(operateLog3.nodeName).isEqualTo(definition.nodes.get(3).nodeName);
                assertThat(operateLog3.statusBefore).isEqualTo(Status.PROCESSING);
                assertThat(operateLog3.operatorId).isEqualTo("operator-node-2");
                assertThat(operateLog3.action).isEqualTo(Action.NEXT);
                assertThat(operateLog3.statusAfter).isEqualTo(Status.PROCESSING);
                assertThat(operateLog3.comment).isEmpty();
            }

            // test back PREVIOUS
            {
                var actionResult = processEngine.resolve(host, instance.getId(), Action.BACK, "operator-node-3");
                instance = actionResult.instance;

                var result1 = processEngine.getInstance(host, instance.getId());
                assertThat(result1.definitionId).isEqualTo(definition.id);
                assertThat(result1.operatorIdSet).containsExactlyInAnyOrder("operator-node-2");
                assertThat(result1.operatorOrgIdSet).isEmpty();
                assertThat(result1.expandOperatorIdSet).containsExactlyInAnyOrder("operator-node-2");
                assertThat(result1.applicant).isEqualTo("operator-1");
                assertThat(result1.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result1.status).isEqualTo(Status.PROCESSING);
                assertThat(result1.nodeId).isEqualTo(singleNode2.nodeId);

                // operate operateLog
                assertThat(result1.operateLogList).hasSize(4);
                var operateLog4 = result1.operateLogList.get(3);
                assertThat(operateLog4).isNotNull();
                assertThat(operateLog4.nodeId).isEqualTo(definition.nodes.get(2).nodeId);
                assertThat(operateLog4.nodeName).isEqualTo(definition.nodes.get(2).nodeName);
                assertThat(operateLog4.statusBefore).isEqualTo(Status.PROCESSING);
                assertThat(operateLog4.operatorId).isEqualTo("operator-node-3");
                assertThat(operateLog4.action).isEqualTo(Action.BACK);
                assertThat(operateLog4.statusAfter).isEqualTo(Status.PROCESSING);
                assertThat(operateLog4.comment).isEmpty();

                actionResult = processEngine.resolve(host, instance.getId(), Action.NEXT, "operator-node-2");
                instance = actionResult.instance;

                var result2 = processEngine.getInstance(host, instance.getId());
                assertThat(result2.definitionId).isEqualTo(definition.id);
                assertThat(result2.operatorIdSet).containsExactlyInAnyOrder("operator-node-3");
                assertThat(result2.operatorOrgIdSet).isEmpty();
                assertThat(result2.expandOperatorIdSet).containsExactlyInAnyOrder("operator-node-3");
                assertThat(result2.applicant).isEqualTo("operator-1");
                assertThat(result2.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result2.status).isEqualTo(Status.PROCESSING);
                assertThat(result2.nodeId).isEqualTo(singleNode3.nodeId);

                // operate operateLog
                assertThat(result2.operateLogList).hasSize(5);
                var operateLog5 = result2.operateLogList.get(4);
                assertThat(operateLog5).isNotNull();
                assertThat(operateLog5.nodeId).isEqualTo(definition.nodes.get(3).nodeId);
                assertThat(operateLog5.nodeName).isEqualTo(definition.nodes.get(3).nodeName);
                assertThat(operateLog5.statusBefore).isEqualTo(Status.PROCESSING);
                assertThat(operateLog5.operatorId).isEqualTo("operator-node-2");
                assertThat(operateLog5.action).isEqualTo(Action.NEXT);
                assertThat(operateLog5.statusAfter).isEqualTo(Status.PROCESSING);
                assertThat(operateLog5.comment).isEmpty();
            }

            // test back FIRST
            {
                var actionParam = new ActionExtendParam();
                actionParam.backMode = BackMode.FIRST;
                actionParam.comment = "back to first node";
                var actionResult = processEngine.resolve(host, instance.getId(), Action.BACK, "operator-node-3", actionParam);
                instance = actionResult.instance;

                var result1 = processEngine.getInstance(host, instance.getId());
                assertThat(result1.definitionId).isEqualTo(definition.id);
                assertThat(result1.operatorIdSet).containsExactlyInAnyOrder("operator-node-1");
                assertThat(result1.operatorOrgIdSet).isEmpty();
                assertThat(result1.expandOperatorIdSet).containsExactlyInAnyOrder("operator-node-1");
                assertThat(result1.applicant).isEqualTo("operator-1");
                assertThat(result1.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result1.status).isEqualTo(Status.PROCESSING);
                assertThat(result1.nodeId).isEqualTo(singleNode1.nodeId);

                // operate operateLog
                assertThat(result1.operateLogList).hasSize(6);
                var operateLog6 = result1.operateLogList.get(5);
                assertThat(operateLog6).isNotNull();
                assertThat(operateLog6.nodeId).isEqualTo(definition.nodes.get(1).nodeId);
                assertThat(operateLog6.nodeName).isEqualTo(definition.nodes.get(1).nodeName);
                assertThat(operateLog6.statusBefore).isEqualTo(Status.PROCESSING);
                assertThat(operateLog6.operatorId).isEqualTo("operator-node-3");
                assertThat(operateLog6.action).isEqualTo(Action.BACK);
                assertThat(operateLog6.statusAfter).isEqualTo(Status.PROCESSING);
                assertThat(operateLog6.comment).isEqualTo("back to first node");
            }
        } finally {
            WorkflowService.singleton.purge(host, workflow.id);
        }
    }

    @Test
    void resolve_multipleNode_or_should_work() throws Exception {
        var workflow = new Workflow(getUuid(), "resolve_multipleNode_or_should_work");
        try {
            workflow = processEngine.createWorkflow(host, workflow);

            var definition = processEngine.getCurrentDefinition(host, workflow.id, 0);

            var multipleNode1 = new MultipleNode("DEFAULT_MULTIPLE_NODE_NAME-1", Set.of("operator-1", "operator-2"), Set.of());
            definition.nodes.add(1, multipleNode1);
            var multipleNode2 = new MultipleNode("DEFAULT_MULTIPLE_NODE_NAME-2", Set.of("operator-3", "operator-4"), Set.of());
            definition.nodes.add(2, multipleNode2);
            var multipleNode3 = new MultipleNode("DEFAULT_MULTIPLE_NODE_NAME-3", Set.of("operator-1", "operator-4"), Set.of());
            definition.nodes.add(3, multipleNode3);
            processEngine.upsertDefinition(host, definition);

            definition = processEngine.getCurrentDefinition(host, workflow.id, 1);

            var param = new ApplicationParam();
            param.workflowId = workflow.id;
            param.applicant = "operator-0";
            var instance = processEngine.startInstance(host, param);

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
                var actionResult = processEngine.resolve(host, instance.getId(), Action.NEXT, "operator-1");
                instance = actionResult.instance;

                var result1 = processEngine.getInstance(host, instance.getId());
                assertThat(result1.definitionId).isEqualTo(definition.id);
                assertThat(result1.operatorIdSet).containsExactlyInAnyOrder("operator-3", "operator-4");
                assertThat(result1.operatorOrgIdSet).isEmpty();
                assertThat(result1.expandOperatorIdSet).containsExactlyInAnyOrder("operator-3", "operator-4");
                assertThat(result1.applicant).isEqualTo("operator-0");
                assertThat(result1.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result1.status).isEqualTo(Status.PROCESSING);
                assertThat(result1.nodeId).isEqualTo(multipleNode2.nodeId);


                actionResult = processEngine.resolve(host, instance.getId(), Action.NEXT, "operator-3");
                instance = actionResult.instance;

                var result2 = processEngine.getInstance(host, instance.getId());
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
                var actionResult = processEngine.resolve(host, instance.getId(), Action.BACK, "operator-4");
                instance = actionResult.instance;

                var result1 = processEngine.getInstance(host, instance.getId());
                assertThat(result1.definitionId).isEqualTo(definition.id);
                assertThat(result1.operatorIdSet).containsExactlyInAnyOrder("operator-3", "operator-4");
                assertThat(result1.operatorOrgIdSet).isEmpty();
                assertThat(result1.expandOperatorIdSet).containsExactlyInAnyOrder("operator-3", "operator-4");
                assertThat(result1.applicant).isEqualTo("operator-0");
                assertThat(result1.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result1.status).isEqualTo(Status.PROCESSING);
                assertThat(result1.nodeId).isEqualTo(multipleNode2.nodeId);

                actionResult = processEngine.resolve(host, instance.getId(), Action.NEXT, "operator-3");
                instance = actionResult.instance;

                var result2 = processEngine.getInstance(host, instance.getId());
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
                var actionResult = processEngine.resolve(host, instance.getId(), Action.BACK, "operator-4", actionParam);
                instance = actionResult.instance;

                var result1 = processEngine.getInstance(host, instance.getId());
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
            workflow = processEngine.createWorkflow(host, workflow);

            var definition = processEngine.getCurrentDefinition(host, workflow.id, 0);

            var multipleNode1 = new MultipleNode("DEFAULT_MULTIPLE_NODE_NAME-1", ApprovalType.AND, Set.of("operator-1", "operator-2"), Set.of());
            definition.nodes.add(1, multipleNode1);
            var multipleNode2 = new MultipleNode("DEFAULT_MULTIPLE_NODE_NAME-2", ApprovalType.AND, Set.of("operator-3", "operator-4"), Set.of());
            definition.nodes.add(2, multipleNode2);
            var multipleNode3 = new MultipleNode("DEFAULT_MULTIPLE_NODE_NAME-3", ApprovalType.AND, Set.of("operator-1", "operator-4"), Set.of());
            definition.nodes.add(3, multipleNode3);
            processEngine.upsertDefinition(host, definition);

            definition = processEngine.getCurrentDefinition(host, workflow.id, 1);

            var param = new ApplicationParam();
            param.workflowId = workflow.id;
            param.applicant = "operator-0";
            var instance = processEngine.startInstance(host, param);

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
                var actionResult = processEngine.resolve(host, instance.getId(), Action.NEXT, "operator-1");
                instance = actionResult.instance;

                var result1 = processEngine.getInstance(host, instance.getId());
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
                actionResult = processEngine.resolve(host, instance.getId(), Action.NEXT, "operator-2");
                instance = actionResult.instance;

                result1 = processEngine.getInstance(host, instance.getId());
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
                actionResult = processEngine.resolve(host, instance.getId(), Action.NEXT, "operator-3");
                instance = actionResult.instance;

                var result2 = processEngine.getInstance(host, instance.getId());
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
                actionResult = processEngine.resolve(host, instance.getId(), Action.SAVE, "operator-3");
                instance = actionResult.instance;

                result1 = processEngine.getInstance(host, instance.getId());
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

                actionResult = processEngine.resolve(host, instance.getId(), Action.NEXT, "operator-4");
                instance = actionResult.instance;

                var result3 = processEngine.getInstance(host, instance.getId());
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
                var actionResult = processEngine.resolve(host, instance.getId(), Action.BACK, "operator-4");
                instance = actionResult.instance;

                var result1 = processEngine.getInstance(host, instance.getId());
                assertThat(result1.definitionId).isEqualTo(definition.id);
                assertThat(result1.operatorIdSet).containsExactlyInAnyOrder("operator-3", "operator-4");
                assertThat(result1.operatorOrgIdSet).isEmpty();
                assertThat(result1.expandOperatorIdSet).containsExactlyInAnyOrder("operator-3", "operator-4");
                assertThat(result1.applicant).isEqualTo("operator-0");
                assertThat(result1.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result1.status).isEqualTo(Status.PROCESSING);
                assertThat(result1.nodeId).isEqualTo(multipleNode2.nodeId);

                actionResult = processEngine.resolve(host, instance.getId(), Action.NEXT, "operator-3");
                instance = actionResult.instance;

                var result2 = processEngine.getInstance(host, instance.getId());
                assertThat(result2.definitionId).isEqualTo(definition.id);
                assertThat(result2.operatorIdSet).containsExactlyInAnyOrder("operator-3", "operator-4");
                assertThat(result2.operatorOrgIdSet).isEmpty();
                assertThat(result2.expandOperatorIdSet).containsExactlyInAnyOrder("operator-3", "operator-4");
                assertThat(result2.applicant).isEqualTo("operator-0");
                assertThat(result2.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result2.status).isEqualTo(Status.PROCESSING);
                assertThat(result2.nodeId).isEqualTo(multipleNode2.nodeId);

                actionResult = processEngine.resolve(host, instance.getId(), Action.NEXT, "operator-4");
                instance = actionResult.instance;

                result2 = processEngine.getInstance(host, instance.getId());
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
                var actionResult = processEngine.resolve(host, instance.getId(), Action.BACK, "operator-4", actionParam);
                instance = actionResult.instance;

                var result1 = processEngine.getInstance(host, instance.getId());
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
            workflow = processEngine.createWorkflow(host, workflow);
            var definition = processEngine.getCurrentDefinition(host, workflow.id, 0);

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
            processEngine.upsertDefinition(host, definition);

            definition = processEngine.getCurrentDefinition(host, workflow.id, 1);

            var param = new ApplicationParam();
            param.workflowId = workflow.id;
            param.applicant = "operator-1";
            var instance = processEngine.startInstance(host, param);

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

                var actionResult = processEngine.resolve(host, instance.getId(), Action.NEXT, "operator-node-1", extendParam);
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

                var result1 = processEngine.getInstance(host, instance.getId());
                assertThat(result1.definitionId).isEqualTo(definition.id);
                assertThat(result1.operatorIdSet).containsExactlyInAnyOrder("operator-node-3");
                assertThat(result1.operatorOrgIdSet).isEmpty();
                assertThat(result1.expandOperatorIdSet).containsExactlyInAnyOrder("operator-node-3");
                assertThat(result1.applicant).isEqualTo("operator-1");
                assertThat(result1.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result1.status).isEqualTo(Status.PROCESSING);
                assertThat(result1.nodeId).isEqualTo(singleNode3.nodeId);

                assertThat(instance.operateLogList).hasSize(3);
                var operateLog2 = instance.operateLogList.get(1);
                assertThat(operateLog2).isNotNull();
                assertThat(operateLog2.nodeId).isEqualTo(definition.nodes.get(2).nodeId);
                assertThat(operateLog2.nodeName).isEqualTo(definition.nodes.get(2).nodeName);
                assertThat(operateLog2.nodeType).isEqualTo(definition.nodes.get(2).getType());
                assertThat(operateLog2.statusBefore).isEqualTo(Status.PROCESSING);
                assertThat(operateLog2.operatorId).isEqualTo("operator-node-1");
                assertThat(operateLog2.action).isEqualTo(Action.NEXT);
                assertThat(operateLog2.statusAfter).isEqualTo(Status.PROCESSING);
                assertThat(operateLog2.comment).isEqualTo(extendParam.comment);
                assertThat(operateLog2.businessParam).isNull();
            }

            // back cannot use plugins
            {
                var extendParam = new ActionExtendParam();
                var testPluginParam = new TestPluginParam();
                testPluginParam.num = 20;
                testPluginParam.str = "test-back";
                extendParam.pluginParam = testPluginParam;

                var actionResult = processEngine.resolve(host, instance.getId(), Action.BACK, "operator-node-3", extendParam);
                var pluginResult = actionResult.pluginResult;
                assertThat(pluginResult).isEmpty();

                var result1 = processEngine.getInstance(host, instance.getId());
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
    void resolve_auto_skip_should_work() throws Exception {
        var workflow = new Workflow(getUuid(), "resolve_auto_skip_should_work");
        try {
            workflow = processEngine.createWorkflow(host, workflow);
            var definition = processEngine.getCurrentDefinition(host, workflow.id, 0);

            var singleNode1 = new SingleNode("DEFAULT_SINGLE_NODE_NAME-1");
            singleNode1.operatorId = "operator-1";
            definition.nodes.add(1, singleNode1);

            var robotNode = new RobotNode("DEFAULT_ROBOT_NODE_NAME-2");
            robotNode.plugins.add("TestPlugin");
            var configMap = new HashMap<String, String>();
            configMap.put("a", "1");
            configMap.put("b", "2");
            robotNode.configuration = configMap;
            definition.nodes.add(2, robotNode);

            var singleNode3 = new SingleNode("DEFAULT_SINGLE_NODE_NAME-3");
            singleNode3.operatorId = SKIP_OPERATOR;
            definition.nodes.add(3, singleNode3);

            var singleNode4 = new SingleNode("DEFAULT_SINGLE_NODE_NAME-3");
            singleNode4.operatorId = "operator-4";
            definition.nodes.add(4, singleNode4);
            processEngine.upsertDefinition(host, definition);

            var endNode = definition.nodes.get(definition.nodes.size() - 1);

            definition = processEngine.getCurrentDefinition(host, workflow.id, 1);

            var param = new ApplicationParam();
            param.workflowId = workflow.id;
            param.applicant = "operator-1";
            var instance = processEngine.startInstance(host, param);

            assertThat(instance.workflowId).isEqualTo(workflow.id);
            assertThat(instance.definitionId).isEqualTo(definition.id);
            assertThat(instance.operatorIdSet).containsExactlyInAnyOrder("operator-1");
            assertThat(instance.operatorOrgIdSet).isEmpty();
            assertThat(instance.expandOperatorIdSet).containsExactlyInAnyOrder("operator-1");
            assertThat(instance.applicant).isEqualTo("operator-1");
            assertThat(instance.applicationMode).isEqualTo(ApplicationMode.SELF);
            assertThat(instance.status).isEqualTo(Status.PROCESSING);
            assertThat(instance.nodeId).isEqualTo(singleNode1.nodeId);

            // next skip
            {

                processEngine.resolve(host, instance.getId(), Action.NEXT, "operator-1");

                var result1 = processEngine.getInstance(host, instance.getId());
                assertThat(result1.definitionId).isEqualTo(definition.id);
                assertThat(result1.operatorIdSet).containsExactlyInAnyOrder("operator-4");
                assertThat(result1.operatorOrgIdSet).isEmpty();
                assertThat(result1.expandOperatorIdSet).containsExactlyInAnyOrder("operator-4");
                assertThat(result1.applicant).isEqualTo("operator-1");
                assertThat(result1.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result1.status).isEqualTo(Status.PROCESSING);
                assertThat(result1.nodeId).isEqualTo(singleNode4.nodeId);
            }

            // back skip
            {
                processEngine.resolve(host, instance.getId(), Action.BACK, "operator-4");

                var result1 = processEngine.getInstance(host, instance.getId());
                assertThat(result1.definitionId).isEqualTo(definition.id);
                assertThat(result1.operatorIdSet).containsExactlyInAnyOrder("operator-1");
                assertThat(result1.operatorOrgIdSet).isEmpty();
                assertThat(result1.expandOperatorIdSet).containsExactlyInAnyOrder("operator-1");
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
    void resolve_reject_should_work() throws Exception {
        var workflow = new Workflow(getUuid(), "resolve_reject_should_work");
        try {
            workflow = processEngine.createWorkflow(host, workflow);
            var definition = processEngine.getCurrentDefinition(host, workflow.id, 0);

            var singleNode1 = new SingleNode("DEFAULT_SINGLE_NODE_NAME-1");
            singleNode1.operatorId = "operator-2";
            definition.nodes.add(1, singleNode1);

            var multipleNode2 = new MultipleNode("DEFAULT_MULTIPLE_NODE_NAME-2", ApprovalType.OR, Set.of("operator-3", "operator-4"), Set.of());
            definition.nodes.add(2, multipleNode2);

            processEngine.upsertDefinition(host, definition);

            var startNode = definition.nodes.get(0);

            var param = new ApplicationParam();
            param.workflowId = workflow.id;
            param.applicant = "operator-1";
            var instance = processEngine.startInstance(host, param);

            var actionResult = processEngine.resolve(host, instance.getId(), Action.REJECT, "operator-2", null);

            var rejectedInstance = actionResult.instance;
            assertThat(rejectedInstance).isNotNull();
            assertThat(rejectedInstance.status).isEqualTo(Status.REJECTED);
            assertThat(rejectedInstance.nodeId).isEqualTo(startNode.nodeId);
            assertThat(rejectedInstance.operatorOrgIdSet).isEmpty();
            assertThat(rejectedInstance.operatorIdSet).isEmpty();
            assertThat(rejectedInstance.expandOperatorIdSet).isEmpty();
            assertThat(instance.nodeId).isEqualTo(singleNode1.nodeId);

        } finally {
            WorkflowService.singleton.purge(host, workflow.id);
        }
    }

    @Test
    void resolve_withdraw_should_work() throws Exception {
        var workflow = new Workflow(getUuid(), "resolve_withdraw_should_work");
        try {
            workflow = processEngine.createWorkflow(host, workflow);
            var definition = processEngine.getCurrentDefinition(host, workflow.id, 0);

            var singleNode1 = new SingleNode("DEFAULT_SINGLE_NODE_NAME-1");
            singleNode1.operatorId = "operator-2";
            definition.nodes.add(1, singleNode1);

            var multipleNode2 = new MultipleNode("DEFAULT_MULTIPLE_NODE_NAME-2", ApprovalType.OR, Set.of("operator-3", "operator-4"), Set.of());
            definition.nodes.add(2, multipleNode2);

            processEngine.upsertDefinition(host, definition);

            var param = new ApplicationParam();
            param.workflowId = workflow.id;
            param.applicant = "operator-1";
            var instance = processEngine.startInstance(host, param);

            processEngine.resolve(host, instance.getId(), Action.CANCEL, "operator-2", null);

            processEngine.resolve(host, instance.getId(), Action.WITHDRAW, "operator-1", null);

            var withdrawalInstance = processEngine.getInstance(host, instance.getId());
            assertThat(withdrawalInstance).isNull();

        } finally {
            WorkflowService.singleton.purge(host, workflow.id);
        }
    }

    @Test
    void resolve_retrieve_should_work() throws Exception {
        var workflow = new Workflow(getUuid(), "resolve_retrieve_should_work");

        try {
            workflow = processEngine.createWorkflow(host, workflow);
            var definition = processEngine.getCurrentDefinition(host, workflow.id, 0);

            var singleNode1 = new SingleNode("DEFAULT_SINGLE_NODE_NAME-1");
            singleNode1.operatorId = "operator-1";
            definition.nodes.add(1, singleNode1);

            var multipleNode2 = new MultipleNode("DEFAULT_MULTIPLE_NODE_NAME-2", ApprovalType.OR, Set.of("operator-2", "operator-3"), Set.of());
            definition.nodes.add(2, multipleNode2);

            var singleNode3 = new SingleNode("DEFAULT_SINGLE_NODE_NAME-3");
            singleNode3.operatorId = "operator-4";
            definition.nodes.add(3, singleNode3);

            processEngine.upsertDefinition(host, definition);

            definition = processEngine.getCurrentDefinition(host, workflow.id, 1);

            var param = new ApplicationParam();
            param.workflowId = workflow.id;
            param.applicant = "operator-test";

            var instance = processEngine.startInstance(host, param);

            assertThat(instance.workflowId).isEqualTo(workflow.id);
            assertThat(instance.definitionId).isEqualTo(definition.id);
            assertThat(instance.operatorIdSet).containsExactlyInAnyOrder("operator-1");
            assertThat(instance.operatorOrgIdSet).isEmpty();
            assertThat(instance.expandOperatorIdSet).containsExactlyInAnyOrder("operator-1");
            assertThat(instance.applicant).isEqualTo("operator-test");
            assertThat(instance.applicationMode).isEqualTo(ApplicationMode.SELF);
            assertThat(instance.status).isEqualTo(Status.PROCESSING);
            assertThat(instance.nodeId).isEqualTo(singleNode1.nodeId);
            assertThat(instance.preNodeId).isEqualTo("");
            assertThat(instance.preExpandOperatorIdSet).isEmpty();

            // second node
            {
                processEngine.resolve(host, instance.getId(), Action.NEXT, "operator-1");

                var result1 = processEngine.getInstanceWithOps(host, instance.getId(), "operator-2");
                assertThat(result1.allowingActions).doesNotContain(Action.RETRIEVE);
                assertThat(result1.nodeId).isEqualTo(multipleNode2.nodeId);

                var result2 = processEngine.getInstanceWithOps(host, instance.getId(), "operator-1");
                assertThat(result2.allowingActions).containsExactlyInAnyOrder(Action.RETRIEVE);
                assertThat(result2.nodeId).isEqualTo(multipleNode2.nodeId);

                // retrieve
                processEngine.resolve(host, instance.getId(), Action.RETRIEVE, "operator-1");

                var result3 = processEngine.getInstanceWithOps(host, instance.getId(), "operator-1");
                assertThat(result3.expandOperatorIdSet).containsExactlyInAnyOrder("operator-1");
                assertThat(result3.nodeId).isEqualTo(singleNode1.nodeId);
                assertThat(result3.preNodeId).isEqualTo("");
                assertThat(result3.preExpandOperatorIdSet).isEmpty();

                processEngine.resolve(host, instance.getId(), Action.NEXT, "operator-1");
            }

            // third node
            {
                processEngine.resolve(host, instance.getId(), Action.NEXT, "operator-2");

                var result1 = processEngine.getInstanceWithOps(host, instance.getId(), "operator-4");
                assertThat(result1.allowingActions).doesNotContain(Action.RETRIEVE);
                assertThat(result1.nodeId).isEqualTo(singleNode3.nodeId);
                assertThat(result1.preNodeId).isEqualTo(multipleNode2.nodeId);
                assertThat(result1.preExpandOperatorIdSet).containsExactlyInAnyOrder("operator-2", "operator-3");

                var result2 = processEngine.getInstanceWithOps(host, instance.getId(), "operator-2");
                assertThat(result2.nodeId).isEqualTo(singleNode3.nodeId);
                assertThat(result2.preNodeId).isEqualTo(multipleNode2.nodeId);
                assertThat(result2.preExpandOperatorIdSet).containsExactlyInAnyOrder("operator-2", "operator-3");
                assertThat(result2.allowingActions).containsExactlyInAnyOrder(Action.RETRIEVE);

                // retrieve
                processEngine.resolve(host, instance.getId(), Action.RETRIEVE, "operator-2");

                var result3 = processEngine.getInstanceWithOps(host, instance.getId(), "operator-2");
                assertThat(result3.expandOperatorIdSet).containsExactlyInAnyOrder("operator-2", "operator-3");
                assertThat(result3.nodeId).isEqualTo(multipleNode2.nodeId);
                assertThat(result3.preNodeId).isEqualTo(singleNode1.nodeId);
                assertThat(result3.preExpandOperatorIdSet).containsExactlyInAnyOrder("operator-1");
            }
        } finally {
            WorkflowService.singleton.purge(host, workflow.id);
        }
    }

    @Test
    void resolve_apply_should_work() throws Exception {
        var workflow = new Workflow(getUuid(), "resolve_apply_should_work");

        try {
            workflow = processEngine.createWorkflow(host, workflow);
            var definition = processEngine.getCurrentDefinition(host, workflow.id, 0);

            var singleNode1 = new SingleNode("DEFAULT_SINGLE_NODE_NAME-1");
            singleNode1.operatorId = "operator-1";
            definition.nodes.add(1, singleNode1);

            var multipleNode2 = new MultipleNode("DEFAULT_MULTIPLE_NODE_NAME-2", ApprovalType.OR, Set.of("operator-2", "operator-3"), Set.of());
            definition.nodes.add(2, multipleNode2);

            var singleNode3 = new SingleNode("DEFAULT_SINGLE_NODE_NAME-3");
            singleNode3.operatorId = "operator-4";
            definition.nodes.add(3, singleNode3);

            processEngine.upsertDefinition(host, definition);

            definition = processEngine.getCurrentDefinition(host, workflow.id, 1);

            var startNode = definition.nodes.get(0);

            var param = new ApplicationParam();
            param.workflowId = workflow.id;
            param.applicant = "operator-test";

            var instance = processEngine.startInstance(host, param);

            // reject -> apply
            {
                var result = processEngine.getInstanceWithOps(host, instance.getId(), "operator-1");
                assertThat(result.nodeId).isEqualTo(singleNode1.nodeId);
                assertThat(result.expandOperatorIdSet).containsExactlyInAnyOrder("operator-1");
                assertThat(result.operateLogList).hasSize(1);

                assertThat(result.operateLogList.get(0).nodeId).isEqualTo(singleNode1.nodeId);
                assertThat(result.operateLogList.get(0).nodeName).isEqualTo(singleNode1.nodeName);
                assertThat(result.operateLogList.get(0).nodeType).isEqualTo(NodeType.SingleNode.name());
                assertThat(result.operateLogList.get(0).statusBefore).isEqualTo(Status.NEW);
                assertThat(result.operateLogList.get(0).statusAfter).isEqualTo(Status.PROCESSING);
                assertThat(result.operateLogList.get(0).action).isEqualTo(Action.APPLY);

                processEngine.resolve(host, instance.getId(), Action.REJECT, "operator-1");

                var result1 = processEngine.getInstanceWithOps(host, instance.getId(), "operator-test");
                assertThat(result1.nodeId).isEqualTo(startNode.nodeId);
                assertThat(result1.expandOperatorIdSet).isEmpty();
                assertThat(result1.operateLogList).hasSize(2);
                assertThat(result1.allowingActions).containsExactlyInAnyOrder(Action.WITHDRAW, Action.APPLY, Action.CANCEL);

                assertThat(result1.operateLogList.get(0).nodeId).isEqualTo(singleNode1.nodeId);
                assertThat(result1.operateLogList.get(0).nodeName).isEqualTo(singleNode1.nodeName);
                assertThat(result1.operateLogList.get(0).nodeType).isEqualTo(NodeType.SingleNode.name());
                assertThat(result1.operateLogList.get(0).statusBefore).isEqualTo(Status.NEW);
                assertThat(result1.operateLogList.get(0).statusAfter).isEqualTo(Status.PROCESSING);
                assertThat(result1.operateLogList.get(0).action).isEqualTo(Action.APPLY);

                assertThat(result1.operateLogList.get(1).nodeId).isEqualTo(startNode.nodeId);
                assertThat(result1.operateLogList.get(1).nodeName).isEqualTo(startNode.nodeName);
                assertThat(result1.operateLogList.get(1).nodeType).isEqualTo(NodeType.StartNode.name());
                assertThat(result1.operateLogList.get(1).statusBefore).isEqualTo(Status.PROCESSING);
                assertThat(result1.operateLogList.get(1).statusAfter).isEqualTo(Status.REJECTED);
                assertThat(result1.operateLogList.get(1).action).isEqualTo(Action.REJECT);

                processEngine.resolve(host, instance.getId(), Action.APPLY, "operator-test");

                var result2 = processEngine.getInstanceWithOps(host, instance.getId(), "operator-1");
                assertThat(result2.nodeId).isEqualTo(singleNode1.nodeId);
                assertThat(result2.expandOperatorIdSet).containsExactlyInAnyOrder("operator-1");
                assertThat(result2.operateLogList).hasSize(3);
                assertThat(result2.allowingActions).containsExactlyInAnyOrder(Action.WITHDRAW, Action.NEXT, Action.CANCEL, Action.REJECT);

                assertThat(result2.operateLogList.get(0).nodeId).isEqualTo(singleNode1.nodeId);
                assertThat(result2.operateLogList.get(0).nodeName).isEqualTo(singleNode1.nodeName);
                assertThat(result2.operateLogList.get(0).nodeType).isEqualTo(NodeType.SingleNode.name());
                assertThat(result2.operateLogList.get(0).statusBefore).isEqualTo(Status.NEW);
                assertThat(result2.operateLogList.get(0).statusAfter).isEqualTo(Status.PROCESSING);
                assertThat(result2.operateLogList.get(0).action).isEqualTo(Action.APPLY);

                assertThat(result2.operateLogList.get(1).nodeId).isEqualTo(startNode.nodeId);
                assertThat(result2.operateLogList.get(1).nodeName).isEqualTo(startNode.nodeName);
                assertThat(result2.operateLogList.get(1).nodeType).isEqualTo(NodeType.StartNode.name());
                assertThat(result2.operateLogList.get(1).statusBefore).isEqualTo(Status.PROCESSING);
                assertThat(result2.operateLogList.get(1).statusAfter).isEqualTo(Status.REJECTED);
                assertThat(result2.operateLogList.get(1).action).isEqualTo(Action.REJECT);

                assertThat(result2.operateLogList.get(2).nodeId).isEqualTo(singleNode1.nodeId);
                assertThat(result2.operateLogList.get(2).nodeName).isEqualTo(singleNode1.nodeName);
                assertThat(result2.operateLogList.get(2).nodeType).isEqualTo(NodeType.SingleNode.name());
                assertThat(result2.operateLogList.get(2).statusBefore).isEqualTo(Status.REJECTED);
                assertThat(result2.operateLogList.get(2).statusAfter).isEqualTo(Status.PROCESSING);
                assertThat(result2.operateLogList.get(2).action).isEqualTo(Action.APPLY);
            }

            // cancel -> apply
            {
                processEngine.resolve(host, instance.getId(), Action.CANCEL, "operator-1");

                var result1 = processEngine.getInstanceWithOps(host, instance.getId(), "operator-test");
                assertThat(result1.nodeId).isEqualTo(startNode.nodeId);
                assertThat(result1.expandOperatorIdSet).isEmpty();
                assertThat(result1.operateLogList).hasSize(4);
                assertThat(result1.allowingActions).containsExactlyInAnyOrder(Action.WITHDRAW, Action.APPLY);

                assertThat(result1.operateLogList.get(3).nodeId).isEqualTo(startNode.nodeId);
                assertThat(result1.operateLogList.get(3).nodeName).isEqualTo(startNode.nodeName);
                assertThat(result1.operateLogList.get(3).nodeType).isEqualTo(NodeType.StartNode.name());
                assertThat(result1.operateLogList.get(3).statusBefore).isEqualTo(Status.PROCESSING);
                assertThat(result1.operateLogList.get(3).statusAfter).isEqualTo(Status.CANCELED);
                assertThat(result1.operateLogList.get(3).action).isEqualTo(Action.CANCEL);

                processEngine.resolve(host, instance.getId(), Action.APPLY, "operator-test");

                var result2 = processEngine.getInstanceWithOps(host, instance.getId(), "operator-1");
                assertThat(result2.nodeId).isEqualTo(singleNode1.nodeId);
                assertThat(result2.expandOperatorIdSet).containsExactlyInAnyOrder("operator-1");
                assertThat(result2.operateLogList).hasSize(5);
                assertThat(result2.allowingActions).containsExactlyInAnyOrder(Action.WITHDRAW, Action.NEXT, Action.CANCEL, Action.REJECT);

                assertThat(result2.operateLogList.get(3).nodeId).isEqualTo(startNode.nodeId);
                assertThat(result2.operateLogList.get(3).nodeName).isEqualTo(startNode.nodeName);
                assertThat(result2.operateLogList.get(3).nodeType).isEqualTo(NodeType.StartNode.name());
                assertThat(result2.operateLogList.get(3).statusBefore).isEqualTo(Status.PROCESSING);
                assertThat(result2.operateLogList.get(3).statusAfter).isEqualTo(Status.CANCELED);
                assertThat(result2.operateLogList.get(3).action).isEqualTo(Action.CANCEL);

                assertThat(result2.operateLogList.get(4).nodeId).isEqualTo(singleNode1.nodeId);
                assertThat(result2.operateLogList.get(4).nodeName).isEqualTo(singleNode1.nodeName);
                assertThat(result2.operateLogList.get(4).nodeType).isEqualTo(NodeType.SingleNode.name());
                assertThat(result2.operateLogList.get(4).statusBefore).isEqualTo(Status.CANCELED);
                assertThat(result2.operateLogList.get(4).statusAfter).isEqualTo(Status.PROCESSING);
                assertThat(result2.operateLogList.get(4).action).isEqualTo(Action.APPLY);

                processEngine.resolve(host, instance.getId(), Action.NEXT, "operator-1");

                var result3 = processEngine.getInstanceWithOps(host, instance.getId(), "operator-2");
                assertThat(result3.nodeId).isEqualTo(multipleNode2.nodeId);
                assertThat(result3.expandOperatorIdSet).containsExactlyInAnyOrder("operator-2", "operator-3");
                assertThat(result3.operateLogList).hasSize(6);
                assertThat(result3.allowingActions).containsExactlyInAnyOrder(Action.WITHDRAW, Action.SAVE, Action.BACK, Action.NEXT, Action.CANCEL, Action.REJECT);
                assertThat(result3.operateLogList).hasSize(6);

                assertThat(result3.operateLogList.get(3).nodeId).isEqualTo(startNode.nodeId);
                assertThat(result3.operateLogList.get(3).nodeName).isEqualTo(startNode.nodeName);
                assertThat(result3.operateLogList.get(3).nodeType).isEqualTo(NodeType.StartNode.name());
                assertThat(result3.operateLogList.get(3).statusBefore).isEqualTo(Status.PROCESSING);
                assertThat(result3.operateLogList.get(3).statusAfter).isEqualTo(Status.CANCELED);
                assertThat(result3.operateLogList.get(3).action).isEqualTo(Action.CANCEL);

                assertThat(result3.operateLogList.get(4).nodeId).isEqualTo(singleNode1.nodeId);
                assertThat(result3.operateLogList.get(4).nodeName).isEqualTo(singleNode1.nodeName);
                assertThat(result3.operateLogList.get(4).nodeType).isEqualTo(NodeType.SingleNode.name());
                assertThat(result3.operateLogList.get(4).statusBefore).isEqualTo(Status.CANCELED);
                assertThat(result3.operateLogList.get(4).statusAfter).isEqualTo(Status.PROCESSING);
                assertThat(result3.operateLogList.get(4).action).isEqualTo(Action.APPLY);

                assertThat(result3.operateLogList.get(5).nodeId).isEqualTo(multipleNode2.nodeId);
                assertThat(result3.operateLogList.get(5).nodeName).isEqualTo(multipleNode2.nodeName);
                assertThat(result3.operateLogList.get(5).nodeType).isEqualTo(NodeType.MultipleNode.name());
                assertThat(result3.operateLogList.get(5).statusBefore).isEqualTo(Status.PROCESSING);
                assertThat(result3.operateLogList.get(5).statusAfter).isEqualTo(Status.PROCESSING);
                assertThat(result3.operateLogList.get(5).action).isEqualTo(Action.NEXT);
            }
        } finally {
            WorkflowService.singleton.purge(host, workflow.id);
        }
    }

    @Test
    void resolve_allowingActions_should_work() throws Exception {
        var workflow = new Workflow(getUuid(), "resolve_allowingActions_should_work");
        try {
            workflow = processEngine.createWorkflow(host, workflow);
            var definition = processEngine.getCurrentDefinition(host, workflow.id, 0);

            var singleNode1 = new SingleNode("DEFAULT_SINGLE_NODE_NAME-1");
            singleNode1.operatorId = "operator-1";
            definition.nodes.add(1, singleNode1);

            var multipleNode2 = new MultipleNode("DEFAULT_MULTIPLE_NODE_NAME-2", ApprovalType.OR, Set.of("operator-3", "operator-4"), Set.of());
            definition.nodes.add(2, multipleNode2);

            processEngine.upsertDefinition(host, definition);

            definition = processEngine.getCurrentDefinition(host, workflow.id, 1);

            var startNode = definition.nodes.get(0);
            var endNode = definition.nodes.get(definition.nodes.size() - 1);

            var param = new ApplicationParam();
            param.workflowId = workflow.id;
            param.applicant = "operator-1";
            var instance = processEngine.startInstance(host, param);

            assertThat(instance.workflowId).isEqualTo(workflow.id);
            assertThat(instance.definitionId).isEqualTo(definition.id);
            assertThat(instance.operatorIdSet).containsExactlyInAnyOrder("operator-1");
            assertThat(instance.operatorOrgIdSet).isEmpty();
            assertThat(instance.expandOperatorIdSet).containsExactlyInAnyOrder("operator-1");
            assertThat(instance.applicant).isEqualTo("operator-1");
            assertThat(instance.applicationMode).isEqualTo(ApplicationMode.SELF);
            assertThat(instance.status).isEqualTo(Status.PROCESSING);
            assertThat(instance.nodeId).isEqualTo(singleNode1.nodeId);

            // processing status
            {
                // first node
                var result = processEngine.getInstanceWithOps(host, instance.getId(), "operator-1");
                assertThat(result.allowingActions).containsExactlyInAnyOrder(Action.NEXT, Action.CANCEL, Action.REJECT, Action.WITHDRAW);

                assertThatThrownBy(() -> processEngine.resolve(host, instance.getId(), Action.BACK, "operator-1"))
                        .isInstanceOf(WorkflowException.class)
                        .hasMessageContaining(WorkflowErrors.NODE_ACTION_INVALID.name());

                assertThatThrownBy(() -> processEngine.resolve(host, instance.getId(), Action.NEXT, "operator-2"))
                        .isInstanceOf(WorkflowException.class)
                        .hasMessageContaining(WorkflowErrors.NODE_ACTION_INVALID.name());

                assertThatThrownBy(() -> processEngine.resolve(host, instance.getId(), Action.SAVE, "operator-1"))
                        .isInstanceOf(WorkflowException.class)
                        .hasMessageContaining(WorkflowErrors.NODE_ACTION_INVALID.name());

                processEngine.resolve(host, instance.getId(), Action.NEXT, "operator-1");

                // second node not operator
                var result2 = processEngine.getInstanceWithOps(host, instance.getId(), "operator-5");
                assertThat(result2.definitionId).isEqualTo(definition.id);
                assertThat(result2.nodeId).isEqualTo(multipleNode2.nodeId);
                assertThat(result2.operatorIdSet).containsExactlyInAnyOrder("operator-3", "operator-4");
                assertThat(result2.operatorOrgIdSet).isEmpty();
                assertThat(result2.expandOperatorIdSet).containsExactlyInAnyOrder("operator-3", "operator-4");
                assertThat(result2.applicant).isEqualTo("operator-1");
                assertThat(result2.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result2.status).isEqualTo(Status.PROCESSING);
                assertThat(result2.allowingActions).isEmpty();

                // second node is pre operator
                result2 = processEngine.getInstanceWithOps(host, instance.getId(), "operator-1");
                assertThat(result2.nodeId).isEqualTo(multipleNode2.nodeId);
                assertThat(result2.operatorIdSet).containsExactlyInAnyOrder("operator-3", "operator-4");
                assertThat(result2.expandOperatorIdSet).containsExactlyInAnyOrder("operator-3", "operator-4");
                assertThat(result2.applicant).isEqualTo("operator-1");
                assertThat(result2.status).isEqualTo(Status.PROCESSING);
                assertThat(result2.allowingActions).containsExactlyInAnyOrder(Action.RETRIEVE);

                // second node operator
                var result3 = processEngine.getInstanceWithOps(host, instance.getId(), "operator-3");
                assertThat(result3.definitionId).isEqualTo(definition.id);
                assertThat(result3.nodeId).isEqualTo(multipleNode2.nodeId);
                assertThat(result3.operatorIdSet).containsExactlyInAnyOrder("operator-3", "operator-4");
                assertThat(result3.operatorOrgIdSet).isEmpty();
                assertThat(result3.expandOperatorIdSet).containsExactlyInAnyOrder("operator-3", "operator-4");
                assertThat(result3.applicant).isEqualTo("operator-1");
                assertThat(result3.applicationMode).isEqualTo(ApplicationMode.SELF);
                assertThat(result3.status).isEqualTo(Status.PROCESSING);
                assertThat(result3.allowingActions).containsExactlyInAnyOrder(Action.NEXT, Action.BACK, Action.SAVE, Action.REJECT, Action.CANCEL, Action.WITHDRAW);

                processEngine.resolve(host, instance.getId(), Action.NEXT, "operator-3");
            }

            // approved status
            {
                // end node applicant
                var result = processEngine.getInstanceWithOps(host, instance.getId(), "operator-1");
                assertThat(result.nodeId).isEqualTo(endNode.nodeId);
                assertThat(result.status).isEqualTo(Status.APPROVED);
                assertThat(result.allowingActions).containsExactlyInAnyOrder(Action.CANCEL, Action.WITHDRAW);

                var operateLogList = result.operateLogList;
                var lastOperateLog = operateLogList.get(operateLogList.size() - 1);
                assertThat(lastOperateLog.statusAfter).isEqualTo(Status.APPROVED);

                // end node not applicant
                var result2 = processEngine.getInstanceWithOps(host, instance.getId(), "operator-2");
                assertThat(result2.nodeId).isEqualTo(endNode.nodeId);
                assertThat(result2.status).isEqualTo(Status.APPROVED);
                assertThat(result2.allowingActions).isEmpty();

                // end node pre operator
                result2 = processEngine.getInstanceWithOps(host, instance.getId(), "operator-3");
                assertThat(result2.nodeId).isEqualTo(endNode.nodeId);
                assertThat(result2.status).isEqualTo(Status.APPROVED);
                assertThat(result2.allowingActions).containsExactlyInAnyOrder(Action.RETRIEVE);
            }

            // instance 2
            var param2 = new ApplicationParam();
            param2.workflowId = workflow.id;
            param2.applicant = "operator-second-1";
            var instance2 = processEngine.startInstance(host, param2);

            assertThat(instance2.workflowId).isEqualTo(workflow.id);
            assertThat(instance2.definitionId).isEqualTo(definition.id);
            assertThat(instance2.operatorIdSet).containsExactlyInAnyOrder("operator-1");
            assertThat(instance2.operatorOrgIdSet).isEmpty();
            assertThat(instance2.expandOperatorIdSet).containsExactlyInAnyOrder("operator-1");
            assertThat(instance2.applicant).isEqualTo("operator-second-1");
            assertThat(instance2.applicationMode).isEqualTo(ApplicationMode.SELF);
            assertThat(instance2.status).isEqualTo(Status.PROCESSING);
            assertThat(instance2.nodeId).isEqualTo(singleNode1.nodeId);

            // rejected status
            {
                processEngine.resolve(host, instance2.getId(), Action.REJECT, "operator-1");

                // operator not application
                var result = processEngine.getInstanceWithOps(host, instance2.getId(), "operator-1");
                assertThat(result.status).isEqualTo(Status.REJECTED);
                assertThat(result.nodeId).isEqualTo(startNode.nodeId);
                assertThat(result.allowingActions).isEmpty();

                // application
                var result2 = processEngine.getInstanceWithOps(host, instance2.getId(), "operator-second-1");
                assertThat(result2.status).isEqualTo(Status.REJECTED);
                assertThat(result2.nodeId).isEqualTo(startNode.nodeId);
                assertThat(result2.allowingActions).containsExactlyInAnyOrder(Action.WITHDRAW, Action.CANCEL, Action.APPLY);
            }

            // canceled status
            {
                processEngine.resolve(host, instance2.getId(), Action.CANCEL, "operator-second-1");

                // operator not application
                var result = processEngine.getInstanceWithOps(host, instance2.getId(), "operator-1");
                assertThat(result.status).isEqualTo(Status.CANCELED);
                assertThat(result.nodeId).isEqualTo(startNode.nodeId);
                assertThat(result.allowingActions).isEmpty();

                // application
                var result2 = processEngine.getInstanceWithOps(host, instance2.getId(), "operator-second-1");
                assertThat(result2.status).isEqualTo(Status.CANCELED);
                assertThat(result2.nodeId).isEqualTo(startNode.nodeId);
                assertThat(result2.allowingActions).containsExactlyInAnyOrder(Action.WITHDRAW, Action.APPLY);
            }
        } finally {
            WorkflowService.singleton.purge(host, workflow.id);
        }
    }

    @Test
    void resolve_send_notification_should_work() throws Exception {
        var workflow = new Workflow(getUuid(), "resolve_send_notification_should_work");
        try {
            workflow = processEngine.createWorkflow(host, workflow);
            var definition = processEngine.getCurrentDefinition(host, workflow.id, 0);

            var singleNode1 = new SingleNode("DEFAULT_SINGLE_NODE_NAME-1");
            singleNode1.operatorId = "operator-1";
            singleNode1.enableNotification = false;
            definition.nodes.add(1, singleNode1);

            var singleNode2 = new SingleNode("DEFAULT_SINGLE_NODE_NAME-2");
            singleNode2.operatorId = "operator-2";
            singleNode2.notificationModes.put(Action.BACK, NotificationMode.NEVER);
            singleNode2.notificationModes.put(Action.NEXT, NotificationMode.ALWAYS);
            definition.nodes.add(2, singleNode2);

            var singleNode3 = new SingleNode("DEFAULT_SINGLE_NODE_NAME-3");
            singleNode3.operatorId = "operator-3";
            singleNode3.notificationModes.put(Action.BACK, NotificationMode.USER_DEFAULT_NOT_SEND);
            definition.nodes.add(3, singleNode3);

            processEngine.upsertDefinition(host, definition);

            var startNode = definition.nodes.get(0);

            var param = new ApplicationParam();
            param.workflowId = workflow.id;
            param.applicant = "operator-1";
            var instance = processEngine.startInstance(host, param);
            assertThat(instance.workflowId).isEqualTo(workflow.id);
            assertThat(instance.definitionId).isEqualTo(definition.id);
            assertThat(instance.operatorIdSet).containsExactlyInAnyOrder("operator-1");
            assertThat(instance.operatorOrgIdSet).isEmpty();
            assertThat(instance.expandOperatorIdSet).containsExactlyInAnyOrder("operator-1");
            assertThat(instance.applicant).isEqualTo("operator-1");
            assertThat(instance.applicationMode).isEqualTo(ApplicationMode.SELF);
            assertThat(instance.status).isEqualTo(Status.PROCESSING);
            assertThat(instance.nodeId).isEqualTo(singleNode1.nodeId);

            // node1 enable notification false
            {
                var actionParam = new ActionExtendParam();
                var testNotification = new TestNotification();
                testNotification.content = "next test";

                actionParam.notification = testNotification;

                var result = processEngine.resolve(host, instance.getId(), Action.NEXT, "operator-1", actionParam);

                instance = result.instance;

                assertThat(instance.operatorIdSet).containsExactlyInAnyOrder("operator-2");
                assertThat(instance.operatorOrgIdSet).isEmpty();
                assertThat(instance.expandOperatorIdSet).containsExactlyInAnyOrder("operator-2");
                assertThat(instance.nodeId).isEqualTo(singleNode2.nodeId);

                assertThat(testNotification.result).isEqualTo("");
            }

            // node2 next
            {
                var actionParam = new ActionExtendParam();
                var testNotification = new TestNotification();
                testNotification.content = "second test";

                actionParam.notification = testNotification;

                var result = processEngine.resolve(host, instance.getId(), Action.NEXT, "operator-2", actionParam);

                instance = result.instance;

                assertThat(instance.operatorIdSet).containsExactlyInAnyOrder("operator-3");
                assertThat(instance.operatorOrgIdSet).isEmpty();
                assertThat(instance.expandOperatorIdSet).containsExactlyInAnyOrder("operator-3");
                assertThat(instance.nodeId).isEqualTo(singleNode3.nodeId);

                assertThat(testNotification.result).isEqualTo("NEXT:second test");
            }

            // node3 back user default
            {
                var actionParam = new ActionExtendParam();
                var testNotification = new TestNotification();
                testNotification.content = "third test";

                actionParam.notification = testNotification;

                var result = processEngine.resolve(host, instance.getId(), Action.BACK, "operator-3", actionParam);

                instance = result.instance;

                assertThat(instance.operatorIdSet).containsExactlyInAnyOrder("operator-2");
                assertThat(instance.operatorOrgIdSet).isEmpty();
                assertThat(instance.expandOperatorIdSet).containsExactlyInAnyOrder("operator-2");
                assertThat(instance.nodeId).isEqualTo(singleNode2.nodeId);

                assertThat(testNotification.result).isEqualTo("");

                var result2 = processEngine.resolve(host, instance.getId(), Action.NEXT, "operator-2", actionParam);
                instance = result2.instance;
            }

            // node3 back user send
            {
                var actionParam = new ActionExtendParam();
                var testNotification = new TestNotification();
                testNotification.content = "third test";

                actionParam.notification = testNotification;
                actionParam.selectedSend = true;

                var result = processEngine.resolve(host, instance.getId(), Action.BACK, "operator-3", actionParam);

                instance = result.instance;

                assertThat(instance.operatorIdSet).containsExactlyInAnyOrder("operator-2");
                assertThat(instance.operatorOrgIdSet).isEmpty();
                assertThat(instance.expandOperatorIdSet).containsExactlyInAnyOrder("operator-2");
                assertThat(instance.nodeId).isEqualTo(singleNode2.nodeId);

                assertThat(testNotification.result).isEqualTo("BACK:third test");

            }

            // node2 back never
            {
                var actionParam = new ActionExtendParam();
                var testNotification = new TestNotification();
                testNotification.content = "fourth test";

                actionParam.notification = testNotification;
                actionParam.selectedSend = true;

                var result = processEngine.resolve(host, instance.getId(), Action.BACK, "operator-2", actionParam);

                instance = result.instance;

                assertThat(instance.operatorIdSet).containsExactlyInAnyOrder("operator-1");
                assertThat(instance.operatorOrgIdSet).isEmpty();
                assertThat(instance.expandOperatorIdSet).containsExactlyInAnyOrder("operator-1");
                assertThat(instance.nodeId).isEqualTo(singleNode1.nodeId);

                assertThat(testNotification.result).isEqualTo("");

                var result2 = processEngine.resolve(host, instance.getId(), Action.NEXT, "operator-1", actionParam);
                instance = result2.instance;
            }

            // node2 reject
            {
                var actionParam = new ActionExtendParam();
                var testNotification = new TestNotification();
                testNotification.content = "fifth test";

                actionParam.notification = testNotification;

                var result = processEngine.resolve(host, instance.getId(), Action.REJECT, "operator-2", actionParam);

                instance = result.instance;

                assertThat(instance.operatorIdSet).isEmpty();
                assertThat(instance.operatorOrgIdSet).isEmpty();
                assertThat(instance.expandOperatorIdSet).isEmpty();
                assertThat(instance.nodeId).isEqualTo(startNode.nodeId);
                assertThat(instance.status).isEqualTo(Status.REJECTED);

                assertThat(testNotification.result).isEqualTo("reject content");
            }
        } finally {
            WorkflowService.singleton.purge(host, workflow.id);
        }
    }

}
