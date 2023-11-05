package cn.cnic.dataspace.api.datax.admin.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("export_excel_task")
public class ExportExcelTask {

    @TableId
    private Long id;

    /**
     * Source information can be used as taskDesc
     */
    private String taskDesc;

    /**
     * Structured Data ID
     */
    private Long dataMappingId;

    /**
     * Export Location
     */
    private String targetPath;

    /**
     * Belonging space
     */
    private String spaceId;

    /**
     * Creation time
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createDate;

    /**
     * Creator
     */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /**
     * Update time
     */
    private Date updateDate;

    /**
     * Task completion time
     */
    private Date finishDate;

    /**
     * Export status 0 failed 1 running 2 successful
     */
    private Integer status;

    /**
     * Task Log
     */
    private String log;
}
