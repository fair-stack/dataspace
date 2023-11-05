package cn.cnic.dataspace.api.datax.admin.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class AddEmptyColVO {

    private Long dataMappingId;

    @ApiModelProperty(value = "前后位置,front,behind")
    private String position;

    @ApiModelProperty(value = "选中的列")
    private String colName;

    @ApiModelProperty("要插入的数量")
    private Integer count;

    @ApiModelProperty(value = "要插入的类型,非必填,不填使用文本")
    private String colType;
}
