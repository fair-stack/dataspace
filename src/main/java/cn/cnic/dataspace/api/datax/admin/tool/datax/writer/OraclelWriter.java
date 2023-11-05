package cn.cnic.dataspace.api.datax.admin.tool.datax.writer;

import java.util.Map;

/**
 * Oracle writer build class
 */
public class OraclelWriter extends BaseWriterPlugin implements DataxWriterInterface {

    @Override
    public String getName() {
        return "oraclewriter";
    }

    @Override
    public Map<String, Object> sample() {
        return null;
    }
}
