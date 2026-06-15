package jp.co.onehr.workflow.dto.base.index;

/**
 * PostgreSQL methods supported for custom indexes.
 *
 * <p>BTREE remains the default for scalar lookups and preserves the behavior of
 * existing index definitions. GIN is intended for containment and membership
 * queries against JSON arrays or objects.</p>
 */
public enum IndexMethod {
    BTREE,
    GIN
}
