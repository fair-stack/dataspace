package cn.cnic.dataspace.api.datax.admin.dto;

import cn.cnic.dataspace.api.datax.admin.tool.database.ColumnInfo;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class ImportFromDataSourceVo {

    /**
     * Data Source ID
     */
    @ApiModelProperty("数据源ID")
    private Long datasourceId;

    /**
     * Data source table name
     */
    @ApiModelProperty("选择的数据源表名")
    private String readerTableName;

    /**
     * Name of structured data to import
     */
    @ApiModelProperty("要导入的结构化数据名称")
    private String writerTableName;

    /**
     * Read Fields
     */
    @ApiModelProperty("读取的字段")
    private List<ColumnInfo> readerColumns;

    /**
     * Fields to write
     */
    @ApiModelProperty("要写入的字段")
    private List<ColumnInfo> writerColumns;

    /**
     * 0 full unconditional
     */
    @ApiModelProperty("抽取条件. 0 全量无条件;1 指定字段条件;2 自定义where条件")
    private Integer whereType;

    /**
     * whereType = 1
     */
    @ApiModelProperty("指定字段条件")
    private List<FilterCondition> filterConditions;

    /**
     * whereType = 2
     */
    @ApiModelProperty("自定义where条件")
    private String where;

    /**
     * Default to 0
     */
    @ApiModelProperty("定期抽取类型. 0 立即执行一次; 1 按照定时执行 2 暂不执行")
    private Integer importType;

    /**
     * Cron expression
     */
    @ApiModelProperty("cron表达式")
    private String cron;

    /**
     * Space ID
     */
    @ApiModelProperty("空间ID")
    private String spaceId;

    /**
     * Structured Data ID
     */
    private Long dataMappingId;

    @JsonIgnore
    private String dbName;

    private Long updateDate;

    @Data
    public static class FilterCondition {

        private String colName;

        private String operType;

        private Object val;
    }
}
