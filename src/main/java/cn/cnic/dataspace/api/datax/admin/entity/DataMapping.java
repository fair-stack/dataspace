package cn.cnic.dataspace.api.datax.admin.entity;

import cn.cnic.dataspace.api.datax.admin.enums.DataMappingType;
import cn.cnic.dataspace.api.datax.admin.enums.SourceType;
import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
@TableName("data_mapping")
public class DataMapping {

    @TableId
    private Long id;

    /**
     * Asset Name
     */
    @ApiModelProperty(value = "名称")
    private String name;

    /**
     * Name of the database to which it belongs
     */
    @ApiModelProperty(value = "所属库名")
    private String databaseName;

    /**
     * Space ID
     */
    @ApiModelProperty(value = "所属空间ID")
    private String spaceId;

    /**
     * Description
     */
    @ApiModelProperty(value = "描述")
    @TableField(value = "`desc`")
    private String desc;

    /**
     * This field does not allow set get
     */
    private Integer isLock;

    /**
     * Type
     */
    @ApiModelProperty(value = "类型")
    private DataMappingType type;

    /**
     * Status: 0 deleted 1 enabled 2 disabled
     */
    @ApiModelProperty(value = "状态：0删除 1启用 2禁用")
    @TableLogic
    private Integer status;

    /**
     * Table name after deletion
     */
    private String deleteName;

    /**
     * Directory format pid
     */
    private Long pid;

    private SourceType sourceType;

    /**
     * Source Record Details
     */
    private String source;

    /**
     * Locked 1 Locked 0 Unlocked
     */
    // private Integer isLock;
    /**
     * Is it public mode 1, public mode 0, not public mode 1
     */
    private Integer isPublic;

    @TableField(fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建时间", hidden = true)
    private Date createDate;

    @TableField(fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建人", hidden = true)
    private String createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @ApiModelProperty(value = "更新时间", hidden = true)
    private Date updateDate;

    // @TableField(fill = FieldFill.INSERT_UPDATE)
    @ApiModelProperty(value = "更新人", hidden = true)
    private String updateBy;

    @TableField(exist = false)
    private String createByUserName;

    @TableField(exist = false)
    private String updateByUserName;

    @TableField(exist = false)
    private List<DataMapping> children;
}
