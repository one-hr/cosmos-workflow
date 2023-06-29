package jp.co.onehr.workflow.dto;

import jp.co.onehr.workflow.dto.base.SimpleData;

public class ApprovalStatus extends SimpleData {

    public ApprovalStatus() {
    }

    public ApprovalStatus(String operatorId, boolean approved) {
        this.operatorId = operatorId;
        this.approved = approved;
    }

    public String operatorId = "";

    public boolean approved = false;

}