package cn.cnic.dataspace.api.datax.admin.tool.datax.reader;

import java.util.Map;

/**
 * Oracle Reader Build Class
 */
public class OracleReader extends BaseReaderPlugin implements DataxReaderInterface {

    @Override
    public String getName() {
        return "oraclereader";
    }

    @Override
    public Map<String, Object> sample() {
        return null;
    }
}
