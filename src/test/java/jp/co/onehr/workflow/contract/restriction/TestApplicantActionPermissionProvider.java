package jp.co.onehr.workflow.contract.restriction;

import jp.co.onehr.workflow.constant.Action;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;
import jp.co.onehr.workflow.dto.param.ApplicantActionContext;
import org.apache.commons.lang3.Strings;

public class TestApplicantActionPermissionProvider implements ApplicantActionPermissionProvider {

    public static final TestApplicantActionPermissionProvider singleton = new TestApplicantActionPermissionProvider();

    @Override
    public boolean canPerformApplicantAction(Definition definition, Instance instance, String operatorId, Action action,
                                             ApplicantActionContext context) {
        if (!(context instanceof TestApplicantActionContext applicantActionContext)) {
            return false;
        }
        return Action.CANCEL.equals(action)
                && Strings.CS.equals(applicantActionContext.host, "localhost")
                && Strings.CS.equals(instance.applicant, "operator-applicant")
                && Strings.CS.equals(operatorId, applicantActionContext.proxyOperatorId);
    }
}
