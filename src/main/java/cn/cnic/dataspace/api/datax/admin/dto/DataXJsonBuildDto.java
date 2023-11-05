package cn.cnic.dataspace.api.datax.admin.dto;

import cn.cnic.dataspace.api.datax.admin.entity.JobDatasource;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * Building JSON DTO
 */
@Data
public class DataXJsonBuildDto implements Serializable {

    private Long readerDatasourceId;

    private List<String> readerTables;

    private List<String> readerColumns;

    private JobDatasource writerDatasource;

    private List<String> writerTables;

    private List<String> writerColumns;

    private HiveReaderDto hiveReader;

    private HiveWriterDto hiveWriter;

    private HbaseReaderDto hbaseReader;

    private HbaseWriterDto hbaseWriter;

    private RdbmsReaderDto rdbmsReader;

    private RdbmsWriterDto rdbmsWriter;

    private MongoDBReaderDto mongoDBReader;

    private MongoDBWriterDto mongoDBWriter;
}
