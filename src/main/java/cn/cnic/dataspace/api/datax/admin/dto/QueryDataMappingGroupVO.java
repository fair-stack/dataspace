package cn.cnic.dataspace.api.datax.admin.dto;

import lombok.Data;
import lombok.experimental.Accessors;
import java.util.Map;

@Data
@Accessors(chain = true)
public class QueryDataMappingGroupVO {

    private Long dataMappingId;

    private String currentColName;

    private Map<String, QueryDataMappingDataVO.QuerySortFilter> querySortFilter;
}
