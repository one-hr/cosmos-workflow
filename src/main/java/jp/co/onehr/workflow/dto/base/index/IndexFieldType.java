package jp.co.onehr.workflow.dto.base.index;

/**
 * Supported data types for JSONB index fields.
 *
 * <p>
 * PostgreSQL custom indexes use these strings when casting JSONB values,
 * for example {@code (data->>'active')::boolean}.
 * </p>
 */
public enum IndexFieldType {

    TEXT("text"),
    NUMERIC("numeric"),
    BIGINT("bigint"),
    BOOLEAN("boolean"),
    JSONB("jsonb");

    /**
     * postgres type string used directly in SQL statements.
     */
    public final String sqlString;

    IndexFieldType(String sqlString) {
        this.sqlString = sqlString;
    }
}
