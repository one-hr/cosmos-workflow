package jp.co.onehr.workflow.dto.node;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import jp.co.onehr.workflow.contract.context.InstanceContext;
import jp.co.onehr.workflow.dto.Instance;

/**
 * The first node of the workflow, used to mark the beginning of the process.
 */
public class StartNode extends Node {

    public StartNode() {

    }

    public StartNode(String nodeName) {
        this.nodeName = nodeName;
    }

    @Override
    public void resetCurrentOperators(Instance instance, InstanceContext instanceContext) {
        clearOperators(instance);
    }

    @Override
    public Set<String> generateExpandOperatorIds(InstanceContext instanceContext) {
        return Sets.newHashSet();
    }

    @Override
    public void checkNodeSetting() {

    }

    @Override
    public void checkOperators(List<String> allowedOperatorIds) {

    }
}
