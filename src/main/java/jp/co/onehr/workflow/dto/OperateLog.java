package jp.co.onehr.workflow.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonSetter;
import io.github.thunderz99.cosmos.util.JsonUtil;
import jp.co.onehr.workflow.constant.Action;
import jp.co.onehr.workflow.constant.Status;
import jp.co.onehr.workflow.contract.log.BusinessParam;
import jp.co.onehr.workflow.dto.base.SimpleData;

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
    public Action action;

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
    public BusinessParam businessParam;

    public OperateLog() {
    }

    @JsonSetter
    public void setBusinessParam(Map<String, Object> map) throws ClassNotFoundException {
        Class<?> clazz = Class.forName((String) map.get("clazz"));
        this.businessParam = (BusinessParam) JsonUtil.fromJson(JsonUtil.toJson(map), clazz);
    }
}
