package jp.co.onehr.workflow.service;

import io.github.thunderz99.cosmos.condition.Condition;
import jp.co.onehr.workflow.base.BaseCRUDServiceTest;
import jp.co.onehr.workflow.constant.ApplicationMode;
import jp.co.onehr.workflow.constant.WorkflowErrors;
import jp.co.onehr.workflow.dto.Workflow;
import jp.co.onehr.workflow.exception.WorkflowException;
import org.junit.jupiter.api.Test;

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
        var workflow = new Workflow();
        workflow.name = "create_new_workflow_test";
        try {
            workflow = getService().create(host, workflow);

            var result = getService().readSuppressing404(host, workflow.getId());
            assertThat(result.name).isEqualTo("create_new_workflow_test");
            assertThat(result.currentVersion).isEqualTo(0);
            assertThat(result.enableVersion).isTrue();

            var definitions = DefinitionService.singleton.find(host, Condition.filter("workflowId", workflow.getId()));
            assertThat(definitions).hasSize(1);
            assertThat(definitions.get(0).id).isNotEqualTo(workflow.getId());
            assertThat(definitions.get(0).workflowId).isEqualTo(workflow.getId());
            assertThat(definitions.get(0).version).isEqualTo(0);
            assertThat(definitions.get(0).nodes).hasSize(2);
            assertThat(definitions.get(0).applicationModes).hasSize(1);
            assertThat(definitions.get(0).applicationModes).containsExactlyInAnyOrder(ApplicationMode.SELF);
        } finally {
            WorkflowService.singleton.purge(host, workflow.id);
        }
    }

    @Test
    void upsert_should_work() throws Exception {
        var workflow = new Workflow(getUuid(), "upsert_should_work");
        try {
            getService().create(host, workflow);

            workflow = getService().readSuppressing404(host, workflow.id);
            assertThat(workflow.enableVersion).isTrue();
            workflow.name = "workflow_name";
            workflow.enableVersion = false;
            getService().upsert(host, workflow);

            workflow = getService().readSuppressing404(host, workflow.id);
            assertThat(workflow.name).isNotEqualTo("upsert_should_work");
            assertThat(workflow.name).isEqualTo("workflow_name");
            assertThat(workflow.enableVersion).isFalse();
        } finally {
            WorkflowService.singleton.purge(host, workflow.id);
        }
    }

    @Test
    void getWorkflow_should_work() throws Exception {
        var workflow = new Workflow(getUuid(), "getWorkflow_should_work");
        try {
            getService().create(host, workflow);

            {
                var result = getService().getWorkflow(host, workflow.getId());
                assertThat(result.id).isEqualTo(workflow.id);
                assertThat(result.name).isEqualTo(workflow.name);
            }

            {
                assertThatThrownBy(() -> getService().getWorkflow(host, "error-id"))
                        .isInstanceOf(WorkflowException.class)
                        .hasMessageContaining(WorkflowErrors.WORKFLOW_NOT_EXIST.name());
            }
        } finally {
            WorkflowService.singleton.purge(host, workflow.id);
        }
    }

}
