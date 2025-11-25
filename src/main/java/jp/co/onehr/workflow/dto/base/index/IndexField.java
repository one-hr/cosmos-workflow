package jp.co.onehr.workflow.dto.base.index;

/**
 * Index for a nested field inside a JSONB column.
 */
public class IndexField {

    /**
     * Field name inside the JSON.
     * Example: contents.item1.value, address.city
     */
    public final String fieldName;
    /**
     * Type of the field.
     */
    public final IndexFieldType type;

    /**
     * Constructor.
     *
     * @param fieldName The field name inside the JSON (e.g., address.city).
     * @param type      The type of the field.
     */
    public IndexField(String fieldName, IndexFieldType type) {
        this.fieldName = fieldName;
        this.type = type;
    }

    /**
     * Constructor that initializes the field using the default type (TEXT).
     *
     * @param fieldName The field name inside the JSON (e.g., address.city).
     */
    public IndexField(String fieldName) {
        this(fieldName, IndexFieldType.TEXT);
    }

    /**
     * Creates a new IndexField instance with the specified field name and type.
     *
     * @param fieldName The field name inside the JSON (e.g., address.city).
     * @param type      The type of the field.
     * @return A new IndexField instance.
     */
    public static IndexField of(String fieldName, IndexFieldType type) {
        return new IndexField(fieldName, type);
    }

    /**
     * Creates a new IndexField instance using the default type (TEXT).
     *
     * @param fieldName The field name inside the JSON (e.g., address.city).
     * @return A new IndexField instance.
     */
    public static IndexField of(String fieldName) {
        return new IndexField(fieldName);
    }
}
