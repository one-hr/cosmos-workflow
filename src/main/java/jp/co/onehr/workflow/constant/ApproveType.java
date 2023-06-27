package jp.co.onehr.workflow.constant;

import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.node.Node;
import jp.co.onehr.workflow.dto.param.ActionExtendParam;
import jp.co.onehr.workflow.service.ApproveStrategy;
import jp.co.onehr.workflow.service.approve.and.AndBackService;
import jp.co.onehr.workflow.service.approve.and.AndNextService;
import jp.co.onehr.workflow.service.approve.or.OrBackService;
import jp.co.onehr.workflow.service.approve.or.OrNextService;
import jp.co.onehr.workflow.service.approve.simple.SimpleBackService;
import jp.co.onehr.workflow.service.approve.simple.SimpleNextService;

/**
 * Node approval type.
 */
public enum ApproveType {
    /**
     * All Approval
     */
    AND(new AndNextService(), new AndBackService()),
    /**
     * Any Approval
     */
    OR(new OrNextService(), new OrBackService()),

    /**
     * The default approval mode, when not specifically set, is used by all nodes.
     */
    SIMPLE(new SimpleNextService(), new SimpleBackService());

    /**
     * The approval strategy used when executing the "next" action.
     */
    private final ApproveStrategy nextStrategy;

    /**
     * The approval strategy used when executing the "back" action.
     */
    private final ApproveStrategy backStrategy;

    ApproveType(ApproveStrategy nextStrategy, ApproveStrategy backStrategy) {
        this.nextStrategy = nextStrategy;
        this.backStrategy = backStrategy;
    }

    public void nextExecute(Definition definition, Instance instance, Node node, ActionExtendParam extendParam) {
        nextStrategy.execute(definition, instance, node, extendParam);
    }

    public void backExecute(Definition definition, Instance instance, Node node, ActionExtendParam extendParam) {
        backStrategy.execute(definition, instance, node, extendParam);
    }
}
