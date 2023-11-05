package cn.cnic.dataspace.api.datax.admin.tool.query;

import cn.cnic.dataspace.api.datax.admin.entity.JobDatasource;
import cn.cnic.dataspace.api.datax.admin.util.JdbcUtils;
import com.google.common.collect.ImmutableList;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Query tools used in Oracle databases
 */
public class OracleQueryTool extends BaseQueryTool implements QueryToolInterface {

    public OracleQueryTool(JobDatasource jobDatasource) throws SQLException {
        super(jobDatasource);
    }

    // No matter how you search, the returned result should only be the table name and table annotation. Traverse the map to obtain the value value
    @Override
    public List<Map<String, Object>> getTableInfo(String tableName) {
        String sqlQueryTableNameComment = sqlBuilder.getSQLQueryTableNameComment();
        logger.info(sqlQueryTableNameComment);
        List<Map<String, Object>> res = null;
        try {
            res = JdbcUtils.executeQuery(connection, sqlQueryTableNameComment, ImmutableList.of(tableName));
        } catch (SQLException e) {
            logger.error("[getTableInfo Exception] --> " + "the exception message is:" + e.getMessage());
        }
        return res;
    }
}
