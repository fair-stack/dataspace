package cn.cnic.dataspace.api.datax.admin.tool.datax.writer;

import java.util.Map;

/**
 * SQL Server Writer Build Class
 */
public class SqlServerlWriter extends BaseWriterPlugin implements DataxWriterInterface {

    @Override
    public String getName() {
        return "sqlserverwriter";
    }

    @Override
    public Map<String, Object> sample() {
        return null;
    }
}
