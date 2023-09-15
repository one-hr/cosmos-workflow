package jp.co.onehr.workflow.contract.log;

import jp.co.onehr.workflow.contract.plugin.TestPluginResult;
import jp.co.onehr.workflow.dto.ActionResult;
import jp.co.onehr.workflow.dto.OperateLog;
import org.apache.commons.collections4.MapUtils;


public class TestOperateLogService implements OperateLogService {

    public static final TestOperateLogService singleton = new TestOperateLogService();

    @Override
    public void handleActionResult(OperateLog log, ActionResult actionResult) {
        if (MapUtils.isNotEmpty(actionResult.pluginResult) &&
                actionResult.pluginResult.get("TestPlugin") instanceof TestPluginResult pluginResult) {
            log.comment = "Plugin Execution Completed";
            log.action = "AUTOTEST";
        }
    }

}
