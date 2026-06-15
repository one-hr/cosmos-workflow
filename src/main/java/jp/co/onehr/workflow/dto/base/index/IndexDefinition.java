package jp.co.onehr.workflow.dto.base.index;

import java.util.List;
import java.util.Objects;

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
     * PostgreSQL index method. Existing definitions use {@link IndexMethod#BTREE}
     * unless another method is specified explicitly.
     */
    public final IndexMethod method;

    /**
     * Constructor of IndexDefinition.
     * Specifies the fields and whether the index is unique.
     *
     * @param fields List of fields that compose the index.
     * @param unique Indicates whether the index is unique.
     *
     */
    public IndexDefinition(List<IndexField> fields, boolean unique) {
        this(fields, unique, IndexMethod.BTREE);
    }

    /**
     * Creates an index definition with an explicit PostgreSQL index method.
     *
     * @param fields List of fields that compose the index.
     * @param unique Indicates whether the index is unique.
     * @param method PostgreSQL index method.
     */
    public IndexDefinition(List<IndexField> fields, boolean unique, IndexMethod method) {
        CheckUtil.check(CollectionUtils.isNotEmpty(fields), "Index must have at least one field.");
        CheckUtil.check(Objects.nonNull(method), "Index method is required.");
        validateGinConstraints(fields, unique, method);
        this.unique = unique;
        this.fields = fields;
        this.method = method;
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
     * Creates a non-unique index definition with an explicit PostgreSQL index method.
     *
     * @param fields List of fields that form the index.
     * @param method PostgreSQL index method.
     */
    public IndexDefinition(List<IndexField> fields, IndexMethod method) {
        this(fields, false, method);
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

    /**
     * Creates an index definition with multiple fields, a uniqueness constraint,
     * and an explicit PostgreSQL index method.
     *
     * @param fields List of fields that compose the index.
     * @param unique Whether the index is unique.
     * @param method PostgreSQL index method.
     * @return A new IndexDefinition instance.
     */
    public static IndexDefinition of(List<IndexField> fields, boolean unique, IndexMethod method) {
        return new IndexDefinition(fields, unique, method);
    }

    /**
     * Creates a non-unique, single-field PostgreSQL GIN index definition.
     *
     * <p>GIN custom indexes are intended for JSON arrays and objects stored below
     * the table's JSONB data column. Only {@link IndexFieldType#TEXT} and
     * {@link IndexFieldType#JSONB} are accepted because java-cosmos uses the JSONB
     * expression directly rather than casting it to a scalar value.</p>
     *
     * @param field Field or nested JSON path to index.
     * @return A validated GIN index definition.
     */
    public static IndexDefinition ofGin(IndexField field) {
        return new IndexDefinition(List.of(field), false, IndexMethod.GIN);
    }

    private static void validateGinConstraints(List<IndexField> fields, boolean unique, IndexMethod method) {
        if (method != IndexMethod.GIN) {
            return;
        }

        CheckUtil.check(!unique, "GIN index does not support unique=true");
        CheckUtil.check(fields.size() == 1, "GIN index currently only supports a single field");

        var field = fields.get(0);
        CheckUtil.check(Objects.nonNull(field), "GIN index field is required.");
        CheckUtil.check(Objects.nonNull(field.type), "GIN index field type is required.");
        CheckUtil.check(field.type == IndexFieldType.TEXT || field.type == IndexFieldType.JSONB,
                "GIN index only supports TEXT or JSONB field type");
    }

}
