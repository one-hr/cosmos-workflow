package jp.co.onehr.workflow.contract.validation;


import jp.co.onehr.workflow.constant.NodeType;
import jp.co.onehr.workflow.constant.WorkflowErrors;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.exception.WorkflowException;
import jp.co.onehr.workflow.service.NodeService;

public class TestValidation implements Validations {

    public static final TestValidation singleton = new TestValidation();

    @Override
    public void definitionValidation(Definition definition) throws Exception {
        var firstNode = NodeService.getFirstNode(definition);
        if (NodeType.RobotNode.isEqual(firstNode.getType())) {
            throw new WorkflowException(WorkflowErrors.NODE_TYPE_INVALID,
                    "The first node in the definition cannot be a robot node", definition.id);
        }
    }
}
