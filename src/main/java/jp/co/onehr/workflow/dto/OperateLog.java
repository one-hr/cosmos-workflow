package jp.co.onehr.workflow.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonSetter;
import io.github.thunderz99.cosmos.util.JsonUtil;
import jp.co.onehr.workflow.constant.Status;
import jp.co.onehr.workflow.contract.context.OperatorLogContext;
import jp.co.onehr.workflow.dto.base.SimpleData;
import org.apache.commons.collections4.MapUtils;

/**
 * operate operateLog
 */
public class OperateLog extends SimpleData {

    /**
     * node id
     */
    public String nodeId = "";

    /**
     * node name
     */
    public String nodeName = "";

    /**
     * node type
     */
    public String nodeType;

    /**
     * status before action
     */
    public Status statusBefore;

    /**
     * operate id
     */
    public String operatorId = "";

    /**
     * action
     */
    public String action;

    /**
     * status after action
     */
    public Status statusAfter;

    /**
     * comment. not required
     */
    public String comment = "";

    /**
     * business param. not required
     */
    public OperatorLogContext logContext;

    /**
     * The operator's action time for the instance
     */
    public String operatorAt = "";

    public OperateLog() {
    }

    @JsonSetter
    public void setLogContext(Map<String, Object> map) throws ClassNotFoundException {
        if (MapUtils.isNotEmpty(map) && map.containsKey("clazz")) {
            Class<?> clazz = Class.forName((String) map.get("clazz"));
            this.logContext = (OperatorLogContext) JsonUtil.fromJson(JsonUtil.toJson(map), clazz);
        }
    }
}
