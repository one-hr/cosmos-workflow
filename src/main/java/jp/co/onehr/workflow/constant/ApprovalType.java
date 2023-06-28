package jp.co.onehr.workflow.constant;

import jp.co.onehr.workflow.dto.ActionResult;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.node.Node;
import jp.co.onehr.workflow.dto.param.ActionExtendParam;
import jp.co.onehr.workflow.service.ApprovalStrategy;
import jp.co.onehr.workflow.service.approval.and.AndBackService;
import jp.co.onehr.workflow.service.approval.and.AndNextService;
import jp.co.onehr.workflow.service.approval.or.OrBackService;
import jp.co.onehr.workflow.service.approval.or.OrNextService;
import jp.co.onehr.workflow.service.approval.simple.SimpleBackService;
import jp.co.onehr.workflow.service.approval.simple.SimpleNextService;

/**
 * Node approval type.
 */
public enum ApprovalType {
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
    private final ApprovalStrategy nextStrategy;

    /**
     * The approval strategy used when executing the "back" action.
     */
    private final ApprovalStrategy backStrategy;

    ApprovalType(ApprovalStrategy nextStrategy, ApprovalStrategy backStrategy) {
        this.nextStrategy = nextStrategy;
        this.backStrategy = backStrategy;
    }

    public ActionResult nextExecute(Definition definition, Instance instance, Node node, String operatorId, ActionExtendParam extendParam) {
        return nextStrategy.execute(definition, instance, node, operatorId, extendParam);
    }

    public ActionResult backExecute(Definition definition, Instance instance, Node node, String operatorId, ActionExtendParam extendParam) {
        return backStrategy.execute(definition, instance, node, operatorId, extendParam);
    }
}
