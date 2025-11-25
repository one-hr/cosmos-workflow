package jp.co.onehr.workflow.dto.base.index;

import java.util.List;

import jp.co.onehr.workflow.util.CheckUtil;
import org.apache.commons.collections4.CollectionUtils;


/**
 * A single index definition for the table.
 */
public class IndexDefinition {
    /**
     * Indicates whether this is a unique index.
     */
    public final boolean unique;

    /**
     * List of fields that make up the index.
     */
    public final List<IndexField> fields;


    /**
     * Constructor of IndexDefinition.
     * Specifies the fields and whether the index is unique.
     *
     * @param fields List of fields that compose the index.
     * @param unique Indicates whether the index is unique.
     *
     */
    public IndexDefinition(List<IndexField> fields, boolean unique) {
        CheckUtil.check(CollectionUtils.isNotEmpty(fields), "Index must have at least one field.");
        this.unique = unique;
        this.fields = fields;
    }

    /**
     * Constructor for a non-unique index.
     *
     * @param fields List of fields that form the index.
     */
    public IndexDefinition(List<IndexField> fields) {
        this(fields, false);
    }


    /**
     * Creates an IndexDefinition with a single text field and a unique flag.
     *
     * @param field  The field that composes the index.
     * @param unique Whether the index is unique.
     * @return A new IndexDefinition instance.
     */
    public static IndexDefinition of(String field, boolean unique) {
        return new IndexDefinition(List.of(IndexField.of(field)), unique);
    }


    /**
     * Creates an IndexDefinition with a single field and a unique flag.
     *
     * @param field  The field that composes the index.
     * @param unique Whether the index is unique.
     * @return A new non-unique IndexDefinition instance.
     */
    public static IndexDefinition of(IndexField field, boolean unique) {
        return new IndexDefinition(List.of(field), unique);
    }

    /**
     * Creates a non-unique IndexDefinition with a single field.
     *
     * @param field The field that composes the index.
     * @return A new non-unique IndexDefinition instance.
     */
    public static IndexDefinition of(IndexField field) {
        return new IndexDefinition(List.of(field));
    }

    /**
     * Creates an IndexDefinition with multiple fields and a unique flag.
     *
     * @param fields List of fields that compose the index.
     * @param unique Whether the index is unique.
     * @return A new IndexDefinition instance.
     */
    public static IndexDefinition of(List<IndexField> fields, boolean unique) {
        return new IndexDefinition(fields, unique);
    }

}
