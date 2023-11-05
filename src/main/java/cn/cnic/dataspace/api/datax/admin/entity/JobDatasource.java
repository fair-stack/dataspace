package cn.cnic.dataspace.api.datax.admin.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import cn.cnic.dataspace.api.datax.admin.core.handler.AESEncryptHandler;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

/**
 * Jdbc data source configuration entity class (job_jdbc_datasource)
 */
@Data
@ApiModel
@TableName("job_jdbc_datasource")
public class JobDatasource extends Model<JobDatasource> {

    /**
     * Self increasing primary key
     */
    @TableId
    @ApiModelProperty(value = "自增主键")
    private Long id;

    /**
     * Data source name
     */
    @ApiModelProperty(value = "数据源名称")
    private String datasourceName;

    /**
     * Data source
     */
    @ApiModelProperty(value = "数据源")
    @NotNull
    private String datasource;

    /**
     * Data source grouping
     */
    @ApiModelProperty(value = "所属空间ID")
    private String spaceId;

    /**
     * User name
     */
    @ApiModelProperty(value = "用户名")
    @TableField(typeHandler = AESEncryptHandler.class)
    private String jdbcUsername;

    /**
     * Password
     */
    @TableField(typeHandler = AESEncryptHandler.class)
    @ApiModelProperty(value = "密码")
    private String jdbcPassword;

    /**
     * jdbc url
     */
    @ApiModelProperty(value = "jdbc url")
    private String jdbcUrl;

    /**
     * Jdbc driver class
     */
    @ApiModelProperty(value = "jdbc驱动类")
    private String jdbcDriverClass;

    /**
     * Status: 0 deleted 1 enabled 2 disabled
     */
    @TableLogic
    @ApiModelProperty(value = "状态：0删除 1启用 2禁用")
    private Integer status;

    /**
     * Creator
     */
    @TableField(fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建人", hidden = true)
    private String createBy;

    /**
     * Creation time
     */
    @TableField(fill = FieldFill.INSERT)
    @JSONField(format = "yyyy/MM/dd")
    @ApiModelProperty(value = "创建时间", hidden = true)
    private Date createDate;

    /**
     * Updated by
     */
    // @TableField(fill = FieldFill.INSERT_UPDATE)
    @ApiModelProperty(value = "更新人", hidden = true)
    private String updateBy;

    /**
     * Update time
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JSONField(format = "yyyy/MM/dd")
    @ApiModelProperty(value = "更新时间", hidden = true)
    private Date updateDate;

    /**
     * Remarks
     */
    @ApiModelProperty(value = "备注", hidden = true)
    private String comments;

    /**
     * Zookeeper address
     */
    @ApiModelProperty(value = "zookeeper地址", hidden = true)
    private String zkAdress;

    /**
     * Database name
     */
    @ApiModelProperty(value = "数据库名")
    private String databaseName;

    /**
     * Number of citations
     */
    private Integer citationNum;

    /**
     * Determine and create a table to transfer to the imported table name
     */
    @TableField(exist = false)
    private String tableName;

    /**
     * Get primary key value
     */
    @Override
    protected Serializable pkVal() {
        return this.id;
    }

    public String getCacheKey() {
        return spaceId + "-" + datasourceName;
    }
}
