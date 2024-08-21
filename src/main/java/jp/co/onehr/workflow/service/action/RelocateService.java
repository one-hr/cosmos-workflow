package jp.co.onehr.workflow.service.action;


import jp.co.onehr.workflow.dto.ActionResult;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.param.ActionExtendParam;
import jp.co.onehr.workflow.service.ActionStrategy;

/**
 * Move the instance to any specified node
 */
public class RelocateService implements ActionStrategy {

    @Override
    public ActionResult execute(Definition definition, Instance instance, String operatorId, ActionExtendParam extendParam) {
        return new ActionResult();
    }
    
}
