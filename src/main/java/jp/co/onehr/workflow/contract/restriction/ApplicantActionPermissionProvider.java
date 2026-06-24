package jp.co.onehr.workflow.contract.restriction;

import jp.co.onehr.workflow.constant.Action;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.param.ApplicantActionContext;

public interface ApplicantActionPermissionProvider {

    /**
     * Checks whether business rules allow the operator to perform an applicant-side action.
     *
     * @param definition workflow definition for the instance
     * @param instance workflow instance being checked
     * @param operatorId operator requesting the action
     * @param action applicant-side action to check
     * @param context extra values used by the business permission check
     * @return true when the operator is allowed to perform the action
     */
    default boolean canPerformApplicantAction(Definition definition, Instance instance, String operatorId, Action action, ApplicantActionContext context) {
        return false;
    }
}
