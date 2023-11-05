package cn.cnic.dataspace.api.datax.admin.tool.datax.reader;

import java.util.Map;

/**
 * MySQL Reader Construction Class
 */
public class MysqlReader extends BaseReaderPlugin implements DataxReaderInterface {

    @Override
    public String getName() {
        return "mysqlreader";
    }

    @Override
    public Map<String, Object> sample() {
        return null;
    }
}
