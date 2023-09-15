package jp.co.onehr.workflow.contract.restriction;


import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Sets;
import jp.co.onehr.workflow.constant.Action;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Instance;

/**
 * Operator
 * Action restrictions for the instance.
 */
public interface ActionRestriction {

    /**
     * Generate custom actions to be removed
     *
     * @param definition
     * @param instance
     * @param operatorId
     * @return
     */
    default Set<Action> generateCustomRemovalActionsByOperator(Definition definition, Instance instance, String operatorId) {
        var actions = new HashSet<Action>();
        var status = instance.status;
        switch (status) {
            case PROCESSING -> actions.addAll(handleProcessingByOperator(definition, instance, operatorId));
            case REJECTED -> actions.addAll(handleRejectedByOperator(definition, instance, operatorId));
            case CANCELED -> actions.addAll(handleCanceledByOperator(definition, instance, operatorId));
            case APPROVED -> actions.addAll(handleApprovedByOperator(definition, instance, operatorId));
            case FINISHED -> actions.addAll(handleFinishedByOperator(definition, instance, operatorId));
        }

        return Sets.newHashSet(actions);
    }

    Set<Action> handleProcessingByOperator(Definition definition, Instance instance, String operatorId);

    Set<Action> handleRejectedByOperator(Definition definition, Instance instance, String operatorId);

    Set<Action> handleCanceledByOperator(Definition definition, Instance instance, String operatorId);

    Set<Action> handleApprovedByOperator(Definition definition, Instance instance, String operatorId);

    Set<Action> handleFinishedByOperator(Definition definition, Instance instance, String operatorId);
}
