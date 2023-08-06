package jp.co.onehr.workflow.dto.param;


import java.util.LinkedHashMap;

import com.google.common.collect.Maps;
import jp.co.onehr.workflow.dto.base.SimpleData;

/**
 * The parameters required for creating a workflow.
 */
public class WorkflowCreationParam extends SimpleData {

    public String id = "";

    /**
     * The name of the workflow.
     */
    public String name = "";

    /**
     * Whether to enable workflow version control.
     */
    public boolean enableVersion = true;

    /**
     * Custom name for the start node.
     */
    public String startNodeName = "";

    public LinkedHashMap<String, String> startLocalNames = Maps.newLinkedHashMap();

    /**
     * Custom name for the end node.
     */
    public String endNodeName = "";

    public LinkedHashMap<String, String> endLocalNames = Maps.newLinkedHashMap();

    public WorkflowCreationParam() {

    }

    public WorkflowCreationParam(String name) {
        this.name = name;
    }

    public WorkflowCreationParam(String name, String startNodeName, String endNodeName) {
        this.name = name;
        this.startNodeName = startNodeName;
        this.endNodeName = endNodeName;
    }

}