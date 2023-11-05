package cn.cnic.dataspace.api.datax.admin.tool.datax.reader;

import java.util.Map;

/**
 * SQL server reader construction class
 */
public class SqlServerReader extends BaseReaderPlugin implements DataxReaderInterface {

    @Override
    public String getName() {
        return "sqlserverreader";
    }

    @Override
    public Map<String, Object> sample() {
        return null;
    }
}
