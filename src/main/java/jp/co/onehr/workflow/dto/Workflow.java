package jp.co.onehr.workflow.dto;

import jp.co.onehr.workflow.dto.base.BaseData;

/**
 * The basic definition of a workflow.
 * <p>
 * Manage the entire workflow, maintain versions of the workflow.
 * Keep a record of the currently used workflow definition.
 */
public class Workflow extends BaseData {

    public String name = "";

    /**
     * The current version number of the running workflow definition.
     * <p>
     * Only the current version of the definition is allowed for new instance applications.
     * Historical definitions cannot be used for new instances.
     */
    public Integer currentVersion = 0;

    /**
     * Control whether the workflow enables version management
     * <p>
     * The default value is true, and for each change in the workflow, a new version definition is generated.
     */
    public boolean enableVersion = true;

    public Workflow() {

    }

    public Workflow(String id, String name) {
        this.setId(id);
        this.name = name;
    }
}
