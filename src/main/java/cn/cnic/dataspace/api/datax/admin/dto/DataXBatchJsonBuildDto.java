package cn.cnic.dataspace.api.datax.admin.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * Building JSON DTO
 */
@Data
public class DataXBatchJsonBuildDto implements Serializable {

    private Long readerDatasourceId;

    private List<String> readerTables;

    private Long writerDatasourceId;

    private List<String> writerTables;

    private int templateId;

    private RdbmsReaderDto rdbmsReader;

    private RdbmsWriterDto rdbmsWriter;
}
