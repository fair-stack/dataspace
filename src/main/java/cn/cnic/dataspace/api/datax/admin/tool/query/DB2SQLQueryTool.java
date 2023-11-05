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
 * Query tools used in DB2 databases
 */
public class DB2SQLQueryTool extends BaseQueryTool implements QueryToolInterface {

    public DB2SQLQueryTool(JobDatasource jobDatasource) throws SQLException {
        super(jobDatasource);
    }
}
