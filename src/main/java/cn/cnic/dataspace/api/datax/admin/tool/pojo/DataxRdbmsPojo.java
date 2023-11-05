package cn.cnic.dataspace.api.datax.admin.tool.pojo;

import cn.cnic.dataspace.api.datax.admin.entity.JobDatasource;
import lombok.Data;
import java.util.List;

/**
 * Used for passing parameters and building JSON
 */
@Data
public class DataxRdbmsPojo {

    /**
     * Table Name
     */
    private List<String> tables;

    /**
     * Column Name
     */
    private List<String> rdbmsColumns;

    /**
     * Data source information
     */
    private JobDatasource jobDatasource;

    /**
     * The querySql attribute, if specified, takes precedence over the columns parameter
     */
    private String querySql;

    /**
     * PreSql attribute
     */
    private String preSql;

    /**
     * PostSql attribute
     */
    private String postSql;

    /**
     * Splitting primary keys
     */
    private String splitPk;

    /**
     * where
     */
    private String whereParam;
}
