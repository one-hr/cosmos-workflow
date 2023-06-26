package jp.co.onehr.workflow.service;

import java.util.HashSet;
import java.util.UUID;

import io.github.thunderz99.cosmos.condition.Condition;
import jp.co.onehr.workflow.constant.ApplicationMode;
import jp.co.onehr.workflow.constant.NodeType;
import jp.co.onehr.workflow.constant.WorkflowErrors;
import jp.co.onehr.workflow.dto.Definition;
import jp.co.onehr.workflow.dto.Workflow;
import jp.co.onehr.workflow.dto.node.EndNode;
import jp.co.onehr.workflow.dto.node.StartNode;
import jp.co.onehr.workflow.exception.WorkflowException;
import jp.co.onehr.workflow.service.base.BaseCRUDService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

public class DefinitionService extends BaseCRUDService<Definition> {

    public static final String DEFAULT_START_NODE_NAME = "Start_Node";
    public static final String DEFAULT_END_NODE_NAME = "End_Node";

    public static final DefinitionService singleton = new DefinitionService();

    DefinitionService() {
        super(Definition.class);
    }

    /**
     * Create an initial workflow definition.
     *
     * @param host
     * @param workflow
     * @return
     */
    public Definition createInitialDefinition(String host, Workflow workflow) throws Exception {
        var definition = generateInitialDefinition(workflow);

        checkNodes(definition);
        return super.create(host, definition);
    }

    @Override
    public Definition create(String host, Definition definition) throws Exception {
        var workflow = WorkflowService.singleton.getWorkflow(host, definition.workflowId);

        definition.id = generateId(definition);
        definition.workflowId = workflow.getId();

        checkNodes(definition);
        return super.create(host, definition);
    }

    @Override
    public Definition upsert(String host, Definition definition) throws Exception {
        var workflow = WorkflowService.singleton.getWorkflow(host, definition.workflowId);

        // increment the version number of the definition by 1
        if (workflow.enableVersion) {
            definition.id = UUID.randomUUID().toString();
            definition.version++;
        }

        definition.id = generateId(definition);
        definition.workflowId = workflow.getId();

        checkNodes(definition);
        var result = super.upsert(host, definition);

        // updating the definition would require updating the current version number of the workflow.
        if (workflow.enableVersion) {
            workflow.currentVersion = result.version;
            WorkflowService.singleton.upsert(host, workflow);
        }

        return result;
    }

    /**
     * Make sure Definition is existed
     *
     * @param host
     * @param definitionId
     * @return
     * @throws Exception
     */
    public Definition getDefinition(String host, String definitionId) throws Exception {
        var definition = super.readSuppressing404(host, definitionId);
        if (ObjectUtils.isEmpty(definition)) {
            throw new WorkflowException(WorkflowErrors.DEFINITION_NOT_EXIST, "The definition does not exist in the database", definitionId);
        }
        return definition;
    }

    /**
     * Retrieve the corresponding workflow definition based on the workflow ID and version number
     *
     * @param host
     * @param workflowId
     * @param version
     * @return
     * @throws Exception
     */
    public Definition getCurrentDefinition(String host, String workflowId, int version) throws Exception {
        var definitions = this.find(host, Condition.filter("workflowId", workflowId, "version", version));
        if (CollectionUtils.isEmpty(definitions)) {
            throw new WorkflowException(WorkflowErrors.DEFINITION_NOT_EXIST, "The definition for the current version of the workflow was not found", workflowId);
        }
        return definitions.get(0);
    }

    /**
     * Generate an initial default definition for the workflow
     * <p>
     * Add a start node and an end node, and set the application mode to self-application
     *
     * @param workflow
     * @return
     * @throws Exception
     */
    private Definition generateInitialDefinition(Workflow workflow) {
        var definition = new Definition();
        definition.id = generateId(definition);
        definition.workflowId = workflow.getId();
        definition.version = 0;
        definition.nodes.add(generateStartNode());
        definition.nodes.add(generateEndNode());
        definition.applicationModes.add(ApplicationMode.SELF);
        return definition;
    }

    /**
     * Generate a start node with a default name
     *
     * @return
     */
    private StartNode generateStartNode() {
        return new StartNode(DEFAULT_START_NODE_NAME);
    }

    /**
     * Generate an end node with a default name
     *
     * @return
     */
    private EndNode generateEndNode() {
        return new EndNode(DEFAULT_END_NODE_NAME);
    }

    /**
     * Check if the nodes in the workflow definition are correct.
     *
     * @param definition
     * @throws Exception
     */
    public void checkNodes(Definition definition) throws Exception {
        var nodes = definition.nodes;

        if (nodes.size() < 2) {
            throw new WorkflowException(WorkflowErrors.DEFINITION_NODE_SIZE_INVALID, "Workflow must have at least two nodes", definition.id);
        }

        var firstNode = nodes.get(0);
        if (!NodeType.StartNode.isEqual(firstNode.getType())) {
            throw new WorkflowException(WorkflowErrors.DEFINITION_FIRST_NODE_INVALID, "The first node of a workflow must be a start node", definition.id);
        }

        var laseNode = nodes.get(nodes.size() - 1);
        if (!NodeType.EndNode.isEqual(laseNode.getType())) {
            throw new WorkflowException(WorkflowErrors.DEFINITION_END_NODE_INVALID, "The last node of a workflow must be an end node", definition.id);
        }

        var typeSet = new HashSet<String>();

        for (var node : nodes) {
            if (StringUtils.isEmpty(node.nodeName)) {
                throw new WorkflowException(WorkflowErrors.NODE_NAME_INVALID, "The name of a node cannot be empty", definition.id);
            }
            if (typeSet.contains(node.getType())) {
                if (NodeType.StartNode.isEqual(node.getType())) {
                    throw new WorkflowException(WorkflowErrors.NODE_TYPE_INVALID, "A workflow can only have one start node", definition.id);
                }
                if (NodeType.EndNode.isEqual(node.getType())) {
                    throw new WorkflowException(WorkflowErrors.NODE_TYPE_INVALID, "A workflow can only have one end node", definition.id);
                }
            }
            typeSet.add(node.getType());
        }

    }

}
