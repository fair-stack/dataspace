package cn.cnic.dataspace.api.datax.admin.tool.pojo;

import cn.cnic.dataspace.api.datax.admin.dto.UpsertInfo;
import cn.cnic.dataspace.api.datax.admin.entity.JobDatasource;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Used for passing parameters and building JSON
 */
@Data
public class DataxMongoDBPojo {

    /**
     * Hive column name
     */
    private List<Map<String, Object>> columns;

    /**
     * Data source information
     */
    private JobDatasource jdbcDatasource;

    private String address;

    private String dbName;

    private String readerTable;

    private String writerTable;

    private String query;

    private UpsertInfo upsertInfo;
}
