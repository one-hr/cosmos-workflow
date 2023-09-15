package jp.co.onehr.workflow.contract.restriction;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Sets;
import jp.co.onehr.workflow.constant.Action;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;

/**
 * Admin
 * Action restrictions for the instance.
 */
public interface AdminActionRestriction {

    /**
     * Generate custom actions to be removed
     *
     * @param definition
     * @param instance
     * @param operatorId
     * @return
     */
    default Set<Action> generateCustomRemovalActionsByAdmin(Definition definition, Instance instance, String operatorId) {
        var actions = new HashSet<Action>();
        var status = instance.status;
        switch (status) {
            case PROCESSING -> actions.addAll(handleProcessingByAdmin(definition, instance, operatorId));
            case REJECTED -> actions.addAll(handleRejectedByAdmin(definition, instance, operatorId));
            case CANCELED -> actions.addAll(handleCanceledByAdmin(definition, instance, operatorId));
            case APPROVED -> actions.addAll(handleApprovedByAdmin(definition, instance, operatorId));
            case FINISHED -> actions.addAll(handleFinishedByAdmin(definition, instance, operatorId));
        }

        return Sets.newHashSet(actions);
    }

    Set<Action> handleProcessingByAdmin(Definition definition, Instance instance, String operatorId);

    Set<Action> handleRejectedByAdmin(Definition definition, Instance instance, String operatorId);

    Set<Action> handleCanceledByAdmin(Definition definition, Instance instance, String operatorId);

    Set<Action> handleApprovedByAdmin(Definition definition, Instance instance, String operatorId);

    Set<Action> handleFinishedByAdmin(Definition definition, Instance instance, String operatorId);
}
