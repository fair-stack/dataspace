package cn.cnic.dataspace.api.datax.admin.tool.datax.reader;

import java.util.Map;

/**
 * Building classes for db2reader
 */
public class DB2Reader extends BaseReaderPlugin implements DataxReaderInterface {

    @Override
    public String getName() {
        return "rdbmsreader";
    }

    @Override
    public Map<String, Object> sample() {
        return null;
    }
}
