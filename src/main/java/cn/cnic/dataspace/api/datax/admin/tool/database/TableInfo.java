package cn.cnic.dataspace.api.datax.admin.tool.database;

import lombok.Data;
import java.util.List;

/**
 * Table Information
 */
@Data
public class TableInfo {

    /**
     * Table Name
     */
    private String name;

    /**
     * Comment
     */
    private String comment;

    /**
     * All columns
     */
    private List<ColumnInfo> columns;
}
