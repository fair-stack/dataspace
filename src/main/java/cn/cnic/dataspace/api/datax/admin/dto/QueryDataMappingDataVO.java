package cn.cnic.dataspace.api.datax.admin.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class QueryDataMappingDataVO {

    private Integer current;

    private Integer size;

    private Long dataMappingId;

    private Map<String, QuerySortFilter> querySortFilter;

    @Data
    @Accessors(chain = true)
    public static class QuerySortFilter {

        @ApiModelProperty(value = "排序规则 desc(降序) asc(升序)")
        private String sort;

        /**
         * 0 according to options
         */
        @ApiModelProperty(value = "0按照选项查询 1 按照条件查询")
        private Integer filterType;

        private QueryFilter filter;
    }

    @Data
    @Accessors(chain = true)
    public static class QueryFilter {

        // According to options
        @ApiModelProperty(value = "按照选项查询选中的选项")
        private List<Select> select;

        @ApiModelProperty(value = "过滤条件")
        private Condition condition;
    }

    @Data
    @Accessors(chain = true)
    public static class Select {

        private boolean isNull;

        private String value;
    }

    @Data
    @Accessors(chain = true)
    public static class Condition {

        /**
         * > >= < <= = like = oper
         */
        @ApiModelProperty(value = "过滤条件类型")
        private String oper;

        @ApiModelProperty(value = "过滤条件的值")
        private String value;

        @ApiModelProperty(value = "过滤条件的值,区间查询的时候使用")
        private String value2;
    }
}
