package cn.cnic.dataspace.api.datax.admin.tool.datax;

import cn.cnic.dataspace.api.datax.admin.tool.pojo.DataxHivePojo;
import cn.cnic.dataspace.api.datax.admin.tool.pojo.DataxMongoDBPojo;
import cn.cnic.dataspace.api.datax.admin.tool.pojo.DataxRdbmsPojo;
import java.util.Map;

/**
 * Plugin Basic Interface
 */
public interface DataxPluginInterface {

    /**
     * Get the reader plugin name
     */
    String getName();

    /**
     * Build
     */
    Map<String, Object> build(DataxRdbmsPojo dataxPluginPojo);

    /**
     * HIVE JSON Construction
     */
    Map<String, Object> buildHive(DataxHivePojo dataxHivePojo);

    /**
     * Mongodb JSON construction
     */
    Map<String, Object> buildMongoDB(DataxMongoDBPojo dataxMongoDBPojo);

    /**
     * Obtain examples
     */
    Map<String, Object> sample();
}
