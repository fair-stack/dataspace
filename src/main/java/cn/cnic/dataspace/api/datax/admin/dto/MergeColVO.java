package cn.cnic.dataspace.api.datax.admin.dto;

import lombok.Data;
import java.util.List;

@Data
public class MergeColVO {

    private Long dataMappingId;

    private String split;

    private List<String> colName;

    private String newCol;
}
