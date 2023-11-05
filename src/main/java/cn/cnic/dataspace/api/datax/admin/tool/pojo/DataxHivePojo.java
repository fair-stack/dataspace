package cn.cnic.dataspace.api.datax.admin.tool.pojo;

import cn.cnic.dataspace.api.datax.admin.entity.JobDatasource;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Used for passing parameters and building JSON
 */
@Data
public class DataxHivePojo {

    /**
     * Hive column name
     */
    private List<Map<String, Object>> columns;

    /**
     * Data source information
     */
    private JobDatasource jdbcDatasource;

    private String readerPath;

    private String readerDefaultFS;

    private String readerFileType;

    private String readerFieldDelimiter;

    private String writerDefaultFS;

    private String writerFileType;

    private String writerPath;

    private String writerFileName;

    private String writeMode;

    private String writeFieldDelimiter;

    private Boolean skipHeader;
}
