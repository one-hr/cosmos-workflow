package jp.co.onehr.workflow.contract.log;


import jp.co.onehr.workflow.dto.ActionResult;
import jp.co.onehr.workflow.dto.OperateLog;

public interface OperateLogService {

    void handleActionResult(OperateLog log, ActionResult actionResult);

}
