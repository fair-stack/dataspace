package cn.cnic.dataspace.api.datax.admin.tool.datax.dto;

import cn.cnic.dataspace.api.datax.admin.dto.DataXJsonBuildDto;
import cn.cnic.dataspace.api.datax.admin.dto.ImportFromDataSourceVo;
import cn.cnic.dataspace.api.datax.admin.dto.RdbmsWriterDto;
import cn.cnic.dataspace.api.datax.admin.entity.JobDatasource;
import cn.cnic.dataspace.api.datax.admin.mapper.JobDatasourceMapper;
import cn.cnic.dataspace.api.datax.admin.tool.database.ColumnInfo;
import com.beust.jcommander.internal.Lists;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.stream.Collectors;
import static cn.cnic.dataspace.api.datax.admin.util.JdbcConstants.*;

@Data
@Slf4j
public abstract class BuildDto {

    DataXJsonBuildDto dataXJsonBuildDto;

    ImportFromDataSourceVo importFromDataSourceVo;

    JobDatasourceMapper jobDatasourceMapper;

    JobDatasource jobDatasource;

    public BuildDto(ImportFromDataSourceVo importFromDataSourceVo, JobDatasourceMapper jobDatasourceMapper) {
        this.importFromDataSourceVo = importFromDataSourceVo;
        this.dataXJsonBuildDto = new DataXJsonBuildDto();
        this.jobDatasourceMapper = jobDatasourceMapper;
        this.jobDatasource = jobDatasourceMapper.selectById(importFromDataSourceVo.getDatasourceId());
        this.build();
    }

    void build() {
        setReaderColumns();
        setReaderTables();
        setWriterColumns();
        setWriterTables();
        buildHbaseReader();
        buildHiveReader();
        buildMongoDBReader();
        buildRdbmsReader();
        buildRdbmsWriter();
        this.dataXJsonBuildDto.setReaderDatasourceId(importFromDataSourceVo.getDatasourceId());
    }

    void setReaderColumns() {
    }

    void setReaderTables() {
        String readerTableName = importFromDataSourceVo.getReaderTableName();
        readerTableName = doConvertKeywordsColumn(jobDatasource.getDatasource(), readerTableName);
        dataXJsonBuildDto.setReaderTables(Lists.newArrayList(readerTableName));
    }

    void setWriterColumns() {
        int readerColSize = importFromDataSourceVo.getReaderColumns().size();
        List<ColumnInfo> writerColumns = null;
        // If the writer column is longer than the reader column, truncate the same length column
        if (readerColSize > importFromDataSourceVo.getWriterColumns().size()) {
            writerColumns = importFromDataSourceVo.getWriterColumns();
        } else {
            writerColumns = importFromDataSourceVo.getWriterColumns().subList(0, readerColSize);
        }
        List<String> collect = writerColumns.stream().map(var -> doConvertKeywordsColumn(MYSQL, var.getName())).collect(Collectors.toList());
        dataXJsonBuildDto.setWriterColumns(collect);
    }

    void setWriterTables() {
        String writerTableName = importFromDataSourceVo.getWriterTableName();
        writerTableName = doConvertKeywordsColumn(MYSQL, writerTableName);
        dataXJsonBuildDto.setWriterTables(Lists.newArrayList(writerTableName));
    }

    void buildHbaseReader() {
    }

    void buildHbaseWriter() {
    }

    void buildHiveReader() {
    }

    void buildHiveWriter() {
    }

    void buildMongoDBReader() {
    }

    void buildMongoDBWriter() {
    }

    void buildRdbmsReader() {
    }

    void buildRdbmsWriter() {
        RdbmsWriterDto rdbmsWriterDto = new RdbmsWriterDto();
        rdbmsWriterDto.setDbName(importFromDataSourceVo.getDbName());
        dataXJsonBuildDto.setRdbmsWriter(rdbmsWriterDto);
    }

    /**
     * Add escape characters based on database type
     */
    String doConvertKeywordsColumn(String dbType, String column) {
        if (column == null) {
            return null;
        }
        column = column.trim();
        column = column.replace("[", "");
        column = column.replace("]", "");
        column = column.replace("`", "");
        column = column.replace("\"", "");
        column = column.replace("'", "");
        switch(dbType) {
            case MYSQL:
                return String.format("`%s`", column);
            case SQL_SERVER:
                return String.format("[%s]", column);
            case POSTGRESQL:
            case ORACLE:
                return String.format("\"%s\"", column);
            default:
                return column;
        }
    }
}
