package jp.co.onehr.workflow.dto.base.index;

/**
 * Supported data types for JSONB index fields.
 */
public enum IndexFieldType {

    TEXT("text"),
    NUMERIC("numeric"),
    BIGINT("bigint"),
    JSONB("jsonb");

    /**
     * postgres type string used directly in SQL statements.
     */
    public final String sqlString;

    IndexFieldType(String sqlString) {
        this.sqlString = sqlString;
    }
}
