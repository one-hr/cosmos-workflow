package jp.co.onehr.workflow.dto;


import java.util.Set;

import com.google.common.collect.Sets;
import jp.co.onehr.workflow.dto.base.BaseData;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Store the information of the previous manual node for the instance.
 */
public class PreviousNodeInfo extends BaseData {

    public String nodeId = "";

    public Set<String> expandOperatorIdSet = Sets.newHashSet();

    public PreviousNodeInfo() {

    }

    public PreviousNodeInfo(String nodeId, Set<String> operatorIds) {
        this.nodeId = nodeId;
        this.expandOperatorIdSet.addAll(operatorIds);
    }

    public boolean isEmpty() {
        if (StringUtils.isBlank(nodeId)) {
            return true;
        }
        if (CollectionUtils.isEmpty(expandOperatorIdSet)) {
            return true;
        }
        return false;
    }
}
