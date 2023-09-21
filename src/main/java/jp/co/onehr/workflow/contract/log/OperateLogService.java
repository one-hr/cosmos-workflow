package jp.co.onehr.workflow.contract.log;


import java.util.Set;

import com.google.common.collect.Sets;
import jp.co.onehr.workflow.constant.Action;
import jp.co.onehr.workflow.dto.ActionResult;
import jp.co.onehr.workflow.dto.OperateLog;

public interface OperateLogService {

    /**
     * Actions to be recorded in the operation log
     */
    Set<Action> ACTIONS_LOG_RECORD = Sets.newHashSet(Action.values());

    void handleActionResult(OperateLog log, ActionResult actionResult);

    /**
     * Customize the collection of actions not to be logged in the operate log
     *
     * @return Actions defined as return values will not be reflected in the operation log when performed
     */
    Set<Action> generateActionsWithNotLogged();

    default void removeActionsWithNotLogged() {
        ACTIONS_LOG_RECORD.removeAll(generateActionsWithNotLogged());
    }
}
