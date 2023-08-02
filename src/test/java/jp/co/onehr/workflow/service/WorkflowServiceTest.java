package jp.co.onehr.workflow.service;

import io.github.thunderz99.cosmos.condition.Condition;
import jp.co.onehr.workflow.base.BaseCRUDServiceTest;
import jp.co.onehr.workflow.constant.ApplicationMode;
import jp.co.onehr.workflow.constant.WorkflowErrors;
import jp.co.onehr.workflow.dto.Workflow;
import jp.co.onehr.workflow.dto.param.WorkflowCreationParam;
import jp.co.onehr.workflow.exception.WorkflowException;
import org.junit.jupiter.api.Test;

import static jp.co.onehr.workflow.service.DefinitionService.DEFAULT_END_NODE_NAME;
import static jp.co.onehr.workflow.service.DefinitionService.DEFAULT_START_NODE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class WorkflowServiceTest extends BaseCRUDServiceTest<Workflow, WorkflowService> {

    @Override
    protected Class<Workflow> getDataClass() {
        return Workflow.class;
    }

    @Override
    protected WorkflowService getService() {
        return WorkflowService.singleton;
    }

    @Test
    void create_should_work() throws Exception {
        var creationParam = new WorkflowCreationParam();
        creationParam.name = "create_new_workflow_test";
        var workflowId = "";
        try {
            var workflow = processDesign.createWorkflow(host, creationParam);
            workflowId = workflow.getId();
            var result = processDesign.getWorkflow(host, workflow.getId());
            assertThat(result.name).isEqualTo("create_new_workflow_test");
            assertThat(result.currentVersion).isEqualTo(0);
            assertThat(result.enableVersion).isTrue();

            var definitions = processDesign.findDefinitions(host, Condition.filter("workflowId", workflow.getId()));
            assertThat(definitions).hasSize(1);
            assertThat(definitions.get(0).id).isNotEqualTo(workflow.getId());
            assertThat(definitions.get(0).workflowId).isEqualTo(workflow.getId());
            assertThat(definitions.get(0).version).isEqualTo(0);
            assertThat(definitions.get(0).nodes).hasSize(2);
            assertThat(definitions.get(0).applicationModes).hasSize(1);
            assertThat(definitions.get(0).applicationModes).containsExactlyInAnyOrder(ApplicationMode.SELF);
            assertThat(definitions.get(0).nodes.get(0).nodeName).isEqualTo(DEFAULT_START_NODE_NAME);
            assertThat(definitions.get(0).nodes.get(1).nodeName).isEqualTo(DEFAULT_END_NODE_NAME);
        } finally {
            WorkflowService.singleton.purge(host, workflowId);
        }
    }

    @Test
    void create_custom_node_names_should_work() throws Exception {
        var creationParam = new WorkflowCreationParam();
        creationParam.name = "create_new_workflow_test";
        creationParam.startNodeName = "custom_start_name";
        creationParam.endNodeName = "custom_end_name";
        creationParam.id = "abcdefg";
        var workflowId = "";
        try {
            var workflow = processDesign.createWorkflow(host, creationParam);
            workflowId = workflow.getId();
            var result = processDesign.getWorkflow(host, workflow.getId());
            assertThat(result.name).isEqualTo("create_new_workflow_test");
            assertThat(result.currentVersion).isEqualTo(0);
            assertThat(result.enableVersion).isTrue();
            assertThat(result.id).isEqualTo("abcdefg");

            var definitions = processDesign.findDefinitions(host, Condition.filter("workflowId", workflow.getId()));
            assertThat(definitions).hasSize(1);
            assertThat(definitions.get(0).id).isNotEqualTo(workflow.getId());
            assertThat(definitions.get(0).workflowId).isEqualTo(workflow.getId());
            assertThat(definitions.get(0).version).isEqualTo(0);
            assertThat(definitions.get(0).nodes).hasSize(2);
            assertThat(definitions.get(0).applicationModes).hasSize(1);
            assertThat(definitions.get(0).applicationModes).containsExactlyInAnyOrder(ApplicationMode.SELF);
            assertThat(definitions.get(0).nodes.get(0).nodeName).isEqualTo("custom_start_name");
            assertThat(definitions.get(0).nodes.get(1).nodeName).isEqualTo("custom_end_name");
        } finally {
            WorkflowService.singleton.purge(host, workflowId);
        }
    }

    @Test
    void update_should_work() throws Exception {
        var creationParam = new WorkflowCreationParam();
        creationParam.name = "upsert_should_work";
        var workflowId = "";
        try {
            var workflow = processDesign.createWorkflow(host, creationParam);
            workflowId = workflow.getId();

            workflow = processDesign.getWorkflow(host, workflow.id);
            assertThat(workflow.enableVersion).isTrue();
            workflow.name = "workflow_name";
            workflow.enableVersion = false;
            getService().update(host, workflow);

            workflow = processDesign.getWorkflow(host, workflow.id);
            assertThat(workflow.name).isNotEqualTo("upsert_should_work");
            assertThat(workflow.name).isEqualTo("workflow_name");
            assertThat(workflow.enableVersion).isFalse();
        } finally {
            WorkflowService.singleton.purge(host, workflowId);
        }
    }

    @Test
    void getWorkflow_should_work() throws Exception {
        var creationParam = new WorkflowCreationParam();
        creationParam.name = "upsert_should_work";
        var workflowId = "";
        try {
            var workflow = processDesign.createWorkflow(host, creationParam);
            workflowId = workflow.getId();

            {
                var result = processDesign.getWorkflow(host, workflow.getId());
                assertThat(result.id).isEqualTo(workflow.id);
                assertThat(result.name).isEqualTo(workflow.name);
            }

            {
                assertThatThrownBy(() -> processDesign.getWorkflow(host, "error-id"))
                        .isInstanceOf(WorkflowException.class)
                        .hasMessageContaining(WorkflowErrors.WORKFLOW_NOT_EXIST.name());
            }
        } finally {
            WorkflowService.singleton.purge(host, workflowId);
        }
    }

}
