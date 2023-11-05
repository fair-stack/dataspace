package cn.cnic.dataspace.api.datax.admin.tool.datax.writer;

import java.util.Map;

/**
 * Building classes for db2writer
 */
public class DB2Writer extends BaseWriterPlugin implements DataxWriterInterface {

    @Override
    public String getName() {
        return "rdbmsreader";
    }

    @Override
    public Map<String, Object> sample() {
        return null;
    }
}
