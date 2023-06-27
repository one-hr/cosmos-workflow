package jp.co.onehr.workflow.service.approve.or;

import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.node.Node;
import jp.co.onehr.workflow.dto.param.ActionExtendParam;
import jp.co.onehr.workflow.service.ApproveStrategy;
import jp.co.onehr.workflow.service.action.NextService;

public class OrNextService extends NextService implements ApproveStrategy {

    @Override
    public void execute(Definition definition, Instance instance, Node node, ActionExtendParam extendParam) {
        handleSimpleNext(definition, instance);
    }

}
