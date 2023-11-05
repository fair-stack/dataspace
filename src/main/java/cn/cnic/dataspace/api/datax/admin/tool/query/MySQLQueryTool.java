package cn.cnic.dataspace.api.datax.admin.tool.query;

import cn.cnic.dataspace.api.datax.admin.entity.JobDatasource;
import java.sql.SQLException;

/**
 * Query tools used in MySQL databases
 */
public class MySQLQueryTool extends BaseQueryTool implements QueryToolInterface {

    public MySQLQueryTool(JobDatasource jobDatasource) throws SQLException {
        super(jobDatasource);
    }
}
