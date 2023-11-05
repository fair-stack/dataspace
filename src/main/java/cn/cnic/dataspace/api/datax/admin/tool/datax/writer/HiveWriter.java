package cn.cnic.dataspace.api.datax.admin.tool.datax.writer;

import cn.cnic.dataspace.api.datax.admin.tool.pojo.DataxHivePojo;
import com.google.common.collect.Maps;
import java.util.Map;

/**
 * Hive writer build class
 */
public class HiveWriter extends BaseWriterPlugin implements DataxWriterInterface {

    @Override
    public String getName() {
        return "hdfswriter";
    }

    @Override
    public Map<String, Object> sample() {
        return null;
    }

    @Override
    public Map<String, Object> buildHive(DataxHivePojo plugin) {
        Map<String, Object> writerObj = Maps.newLinkedHashMap();
        writerObj.put("name", getName());
        Map<String, Object> parameterObj = Maps.newLinkedHashMap();
        parameterObj.put("defaultFS", plugin.getWriterDefaultFS());
        parameterObj.put("fileType", plugin.getWriterFileType());
        parameterObj.put("path", plugin.getWriterPath());
        parameterObj.put("fileName", plugin.getWriterFileName());
        parameterObj.put("writeMode", plugin.getWriteMode());
        parameterObj.put("fieldDelimiter", plugin.getWriteFieldDelimiter());
        parameterObj.put("column", plugin.getColumns());
        writerObj.put("parameter", parameterObj);
        return writerObj;
    }
}
