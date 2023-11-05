package cn.cnic.dataspace.api.datax.admin.tool.datax.writer;

import java.util.Map;

/**
 * MySQL writer build class
 */
public class MysqlWriter extends BaseWriterPlugin implements DataxWriterInterface {

    @Override
    public String getName() {
        return "mysqlwriter";
    }

    @Override
    public Map<String, Object> sample() {
        return null;
    }
}
