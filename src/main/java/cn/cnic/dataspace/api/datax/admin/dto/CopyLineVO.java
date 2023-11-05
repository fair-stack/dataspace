package cn.cnic.dataspace.api.datax.admin.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class CopyLineVO {

    @ApiModelProperty("结构化数据ID")
    private Long dataMappingId;

    @ApiModelProperty("copy的数据")
    private List<Map<String, String>> datas;
}
