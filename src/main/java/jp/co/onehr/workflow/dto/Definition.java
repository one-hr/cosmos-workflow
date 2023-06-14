package jp.co.onehr.workflow.dto;

import jp.co.onehr.workflow.dto.base.BaseData;

/**
 * Workflow definition
 */
public class Definition extends BaseData {

    /**
     * Workflow name
     */
    public String name = "";

    public Definition() {

    }

    public Definition(String id, String name) {
        this.id = id;
        this.name = name;
    }
}
