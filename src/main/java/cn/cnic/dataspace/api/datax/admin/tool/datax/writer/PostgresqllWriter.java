package cn.cnic.dataspace.api.datax.admin.tool.datax.writer;

import java.util.Map;

/**
 * PostgreSQL writer build class
 */
public class PostgresqllWriter extends BaseWriterPlugin implements DataxWriterInterface {

    @Override
    public String getName() {
        return "postgresqlwriter";
    }

    @Override
    public Map<String, Object> sample() {
        return null;
    }
}
