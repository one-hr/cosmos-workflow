package jp.co.onehr.workflow.dto.base.index;


import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Interface for customizing postgres indexes for a DTO.
 *
 * <p>
 * By implementing this interface, you can define custom indexes
 * for the DTO's table.<br>
 * Example: Creating an index on data->>'workflowId' for Instance.
 * </p>
 *
 * <p>
 * Effective only for postgres.
 * Has no effect for other database types.
 * </p>
 */
public interface IndexCustomizable {


    /**
     * Returns a list of custom index definitions required by this DTO
     * (effective only when using postgres).
     *
     * <p>
     * If nothing is defined, no custom indexes are created.
     * If definitions are provided, they are created during schema initialization in BaseNoSqlService.getColl(host).
     * If an index with the same name already exists, no action is taken.
     * Fields specified as uniqueKey automatically generate a unique B-tree index,so no custom index needs to be defined here.
     * Example: Instance.workflowId requires no custom index.
     * </p>
     *
     * <p>
     * Instance implements IndexCustomizable and can be referenced as an example.
     * Note: When inheriting a parent class, custom indexes defined in the parent have no effect.
     * Only custom indexes defined by the child (overridden) are applied.
     * </p>
     *
     * @return A list of custom index definitions (multiple indexes may be specified).
     */
    @JsonIgnore
    public List<IndexDefinition> getCustomIndexDefinitions();
}
