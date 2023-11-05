package cn.cnic.dataspace.api.datax.admin.dto;

import lombok.Data;
import java.util.List;

@Data
public class DeleteLineVO {

    private Long dataMappingId;

    private List<String> primaryKey;
}
