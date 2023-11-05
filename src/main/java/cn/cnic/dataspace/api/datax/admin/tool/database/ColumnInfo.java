package cn.cnic.dataspace.api.datax.admin.tool.database;

import lombok.Data;

/**
 * Field information
 */
@Data
public class ColumnInfo {

    /**
     * Field Name
     */
    private String name;

    /**
     * Comment
     */
    private String comment;

    /**
     * Field Type
     */
    private String type;

    /**
     * Is it a primary key column
     */
    private Boolean ifPrimaryKey;

    /**
     * Is it nullable? 0 cannot be empty. 1 can be null
     */
    private int isnull;
}
