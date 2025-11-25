package jp.co.onehr.workflow.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import com.google.common.collect.Maps;
import io.github.thunderz99.cosmos.impl.postgres.util.TableUtil;

/**
 * Utility for operating PostgreSQL tables during testing.
 *
 * <p>
 * unit test only
 * </p>
 */
public class PGTableTestUtil {

    /*
     * Finds an index name and its index definition (utility method for testing).
     *
     * @param conn
     * @param schemaName
     * @param tableName
     * @return
     * @throws SQLException
     */
    public static Map<String, String> findIndexes(Connection conn, String schemaName, String tableName) throws SQLException {

        schemaName = TableUtil.checkAndNormalizeValidEntityName(schemaName);
        tableName = TableUtil.checkAndNormalizeValidEntityName(tableName);

        schemaName = TableUtil.removeQuotes(schemaName);
        tableName = TableUtil.removeQuotes(tableName);

        Map<String, String> ret = Maps.newHashMap();
        // Query pg_indexes to check for the index existence.
        var query = "SELECT * FROM pg_indexes WHERE schemaname = ? AND tablename = ?";
        try (var pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, schemaName);
            pstmt.setString(2, tableName);
            try (var rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ret.put(rs.getString("indexname"), rs.getString("indexdef"));
                }
            }
        }
        return ret;
    }

}
