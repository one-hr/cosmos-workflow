package jp.co.onehr.workflow.service;

import jp.co.onehr.workflow.dto.ActionResult;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.node.Node;
import jp.co.onehr.workflow.dto.param.ActionExtendParam;

/**
 * The handling strategies corresponding to different approve modes of nodes
 */
public interface ApprovalStrategy {

    ActionResult execute(Definition definition, Instance instance, Node node, String operatorId, ActionExtendParam extendParam);

}
