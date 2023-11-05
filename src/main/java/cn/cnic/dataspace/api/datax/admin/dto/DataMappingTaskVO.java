package cn.cnic.dataspace.api.datax.admin.dto;

import lombok.Data;
import java.util.Date;

@Data
public class DataMappingTaskVO {

    private Long id;

    private Long dataMappingId;

    private String dataMappingName;

    /**
     * DatasourceImport
     * ExcelExport
     * ExcelImport
     */
    private String taskType;

    private String taskDesc;

    private Date createDate;

    private Date finishDate;

    private Integer status;

    private String log;
}
