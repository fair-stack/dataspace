package cn.cnic.dataspace.api.datax.admin.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import java.util.List;

@Data
public class CopyAddColVO {

    private Long dataMappingId;

    @ApiModelProperty(value = "前后位置,front,behind")
    private String position;

    @ApiModelProperty(value = "选中的列")
    private String colName;

    private List<AddCol> addCols;

    @Data
    @Accessors(chain = true)
    public static class AddCol {

        private String fromColName;

        private String fromColType;

        private String newColName;
    }
}
