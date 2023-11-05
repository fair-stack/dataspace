package cn.cnic.dataspace.api.datax.admin.tool.query;

import cn.cnic.dataspace.api.datax.admin.entity.JobDatasource;
import cn.cnic.dataspace.api.datax.admin.util.JdbcUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * sql server
 *
 * @author
 * @version 1.0
 * @since 2019/8/2
 */
public class SqlServerQueryTool extends BaseQueryTool implements QueryToolInterface {

    public SqlServerQueryTool(JobDatasource jobDatasource) throws SQLException {
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

    // Obtain the primary key of the specified table, which may be multiple, so use list
    @Override
    public List<String> getPrimaryKeys(String tableName) {
        List<String> res = Lists.newArrayList();
        String sqlQueryPrimaryKey = sqlBuilder.getSQLQueryPrimaryKey();
        try {
            List<Map<String, Object>> pkColumns = JdbcUtils.executeQuery(connection, sqlQueryPrimaryKey, ImmutableList.of(tableName));
            // Just return the primary key name
            pkColumns.forEach(e -> res.add((String) new ArrayList<>(e.values()).get(0)));
        } catch (SQLException e) {
            logger.error("[getPrimaryKeys Exception] --> " + "the exception message is:" + e.getMessage());
        }
        return res;
    }
}
