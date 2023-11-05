package cn.cnic.dataspace.api.datax.admin.dto;

import lombok.Data;
import java.util.List;

@Data
public class SetCol2NullVO {

    private Long dataMappingId;

    private List<String> colName;
}
