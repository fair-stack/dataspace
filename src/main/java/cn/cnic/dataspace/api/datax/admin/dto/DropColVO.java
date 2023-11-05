package cn.cnic.dataspace.api.datax.admin.dto;

import lombok.Data;
import java.util.List;

@Data
public class DropColVO {

    private Long dataMappingId;

    private List<String> colName;
}
