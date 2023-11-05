package cn.cnic.dataspace.api.datax.admin.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.Map;

@Data
public class AddLineVo {

    @ApiModelProperty("结构化数据ID")
    private Long dataMappingId;

    @ApiModelProperty("单行数据")
    private Map<String, String> data;
}
