package jp.co.onehr.workflow.dto.param;


import java.util.List;
import java.util.Set;

import com.azure.cosmos.implementation.guava25.collect.Sets;
import com.google.common.collect.Lists;
import jp.co.onehr.workflow.constant.ApplicationMode;
import jp.co.onehr.workflow.dto.node.Node;

public class DefinitionParam {

    public String workflowId = "";

    public List<Node> nodes = Lists.newLinkedList();

    public Set<ApplicationMode> applicationModes = Sets.newHashSet();
}
