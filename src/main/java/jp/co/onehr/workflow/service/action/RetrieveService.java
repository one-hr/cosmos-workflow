package jp.co.onehr.workflow.service.action;

import jp.co.onehr.workflow.dto.ActionResult;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.param.ActionExtendParam;
import jp.co.onehr.workflow.service.ActionStrategy;


/**
 * Processing of the "Retrieve" action
 * <p>
 * todo In the case of parallel approval, the retrieval action needs special handling.
 */
public class RetrieveService implements ActionStrategy {

    @Override
    public ActionResult execute(Definition definition, Instance instance, String operatorId, ActionExtendParam extendParam) {
        instance.nodeId = instance.preNodeId;
        return new ActionResult();
    }

}
