package cn.cnic.dataspace.api.datax.admin.tool.database;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Original jdbc field object
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DasColumn {

    private String columnName;

    private String columnTypeName;

    private String columnClassName;

    private String columnComment;

    private int isNull;

    private boolean isprimaryKey;
}
