package cn.cnic.dataspace.api.datax.admin.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("import_excel_task")
public class ImportExcelTask {

    private Long id;

    /**
     */
    private Long dataMappingId;

    /**
     * Source information can be used as taskDesc
     */
    private String taskDesc;

    /**
     * Structured Data Name
     */
    @TableField(exist = false)
    private String dataMappingName;

    /**
     * File Location
     */
    private String fileLocation;

    /**
     * spaceId
     */
    private String spaceId;

    /**
     * Import status 0 failed 1 running 2 successful
     */
    private Integer status;

    /**
     * Log
     */
    private String log;

    @TableField(fill = FieldFill.INSERT)
    private Date createDate;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /**
     * Completion time
     */
    private Date finishDate;
}
