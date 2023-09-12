package jp.co.onehr.workflow.dto.param;

import java.util.Set;

import com.google.common.collect.Sets;
import jp.co.onehr.workflow.constant.Status;

public class BulkRebindingParam {

    /**
     * version number of the definition for instance re-binding
     */
    public Integer definitionVersion;

    /**
     * comment
     */
    public String comment = "";

    /**
     * Specify the Instances Requiring Rebinding Based on Their States
     */
    public Set<Status> statuses = Sets.newHashSet();

    /**
     * Instances Under the Target Definition Also Reset to the first Node
     */
    public boolean includeTargetDefinition = false;
}
