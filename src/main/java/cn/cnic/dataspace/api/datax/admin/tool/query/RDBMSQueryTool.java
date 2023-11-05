package cn.cnic.dataspace.api.datax.admin.tool.query;

import cn.cnic.dataspace.api.datax.admin.entity.JobDatasource;
import java.sql.SQLException;

public class RDBMSQueryTool extends BaseQueryTool {

    /**
     * Construction method
     */
    RDBMSQueryTool(JobDatasource jobDatasource) throws SQLException {
        super(jobDatasource);
    }
}
