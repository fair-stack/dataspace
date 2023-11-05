package cn.cnic.dataspace.api.datax.admin.tool.datax.reader;

import java.util.Map;

/**
 * PostgreSQL Build Class
 */
public class PostgresqlReader extends BaseReaderPlugin implements DataxReaderInterface {

    @Override
    public String getName() {
        return "postgresqlreader";
    }

    @Override
    public Map<String, Object> sample() {
        return null;
    }
}
