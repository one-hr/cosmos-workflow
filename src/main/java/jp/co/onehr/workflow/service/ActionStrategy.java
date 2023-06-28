package jp.co.onehr.workflow.service;

import jp.co.onehr.workflow.dto.ActionResult;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.param.ActionExtendParam;

/**
 * The handling strategies corresponding to the movement actions of nodes.
 */
public interface ActionStrategy {

    ActionResult execute(Definition definition, Instance instance, String operatorId, ActionExtendParam extendParam);

}
