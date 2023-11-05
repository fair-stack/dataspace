package cn.cnic.dataspace.api.datax.admin.dto;

import cn.cnic.dataspace.api.datax.admin.enums.DataMappingType;
import cn.cnic.dataspace.api.datax.admin.enums.SourceType;
import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class DataMappingDto {

    private Long id;

    private String name;

    private String spaceId;

    private String desc;

    private DataMappingType type;

    private Integer isLock;

    private Integer isPublic;

    private String source;

    private SourceType sourceType;

    @ApiModelProperty(value = "创建时间", hidden = true)
    private Date createDate;

    @ApiModelProperty(value = "创建人", hidden = true)
    private String createBy;

    @ApiModelProperty(value = "更新时间", hidden = true)
    private Date updateDate;

    @ApiModelProperty(value = "更新人", hidden = true)
    private String updateBy;

    @TableField(exist = false)
    private String createByUserName;

    @TableField(exist = false)
    private String updateByUserName;

    private Long dataCount;

    /**
     * 1 On state
     */
    private Integer triggerStatus;

    private List<DataMappingDto> children;
}
