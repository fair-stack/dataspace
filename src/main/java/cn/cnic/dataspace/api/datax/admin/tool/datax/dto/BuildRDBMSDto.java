package cn.cnic.dataspace.api.datax.admin.tool.datax.dto;

import cn.cnic.dataspace.api.datax.admin.dto.ImportFromDataSourceVo;
import cn.cnic.dataspace.api.datax.admin.dto.RdbmsReaderDto;
import cn.cnic.dataspace.api.datax.admin.dto.RdbmsWriterDto;
import cn.cnic.dataspace.api.datax.admin.entity.JobDatasource;
import cn.cnic.dataspace.api.datax.admin.mapper.JobDatasourceMapper;
import cn.cnic.dataspace.api.datax.admin.tool.database.ColumnInfo;
import com.beust.jcommander.internal.Lists;
import org.mapstruct.ap.internal.util.Strings;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import java.util.List;
import java.util.stream.Collectors;
import static cn.cnic.dataspace.api.datax.admin.util.JdbcConstants.*;
import static cn.cnic.dataspace.api.datax.admin.util.JdbcConstants.ORACLE;

public class BuildRDBMSDto extends BuildDto {

    public BuildRDBMSDto(ImportFromDataSourceVo importFromDataSourceVo, JobDatasourceMapper jobDatasourceMapper) {
        super(importFromDataSourceVo, jobDatasourceMapper);
    }

    @Override
    void setReaderColumns() {
        int writerColSize = importFromDataSourceVo.getWriterColumns().size();
        List<ColumnInfo> readerColumns = null;
        // If the reader column is longer than the writer column, truncate the same length column
        if (writerColSize > importFromDataSourceVo.getReaderColumns().size()) {
            readerColumns = importFromDataSourceVo.getReaderColumns();
        } else {
            readerColumns = importFromDataSourceVo.getReaderColumns().subList(0, writerColSize);
        }
        List<String> collect = readerColumns.stream().map(var -> doConvertKeywordsColumn(jobDatasource.getDatasource(), var.getName())).collect(Collectors.toList());
        dataXJsonBuildDto.setReaderColumns(collect);
    }

    @Override
    void buildRdbmsReader() {
        RdbmsReaderDto rdbmsReaderDto = new RdbmsReaderDto();
        Integer whereType = importFromDataSourceVo.getWhereType();
        if (whereType == null) {
            whereType = 0;
        }
        String where = "";
        switch(whereType) {
            case 0:
                where = "";
                break;
            case 1:
                List<ImportFromDataSourceVo.FilterCondition> filterConditions = importFromDataSourceVo.getFilterConditions();
                List<String> collect = filterConditions.stream().map(var -> {
                    String colName = var.getColName();
                    String operType = var.getOperType();
                    Object val = var.getVal();
                    if ("like".equals(operType)) {
                        return doConvertKeywordsColumn(jobDatasource.getDatasource(), colName) + " like '%" + val.toString() + "%'";
                    } else {
                        return doConvertKeywordsColumn(jobDatasource.getDatasource(), colName) + " " + operType + " '" + val.toString() + "'";
                    }
                }).collect(Collectors.toList());
                where = Strings.join(collect, " and ");
                break;
            case 2:
                where = importFromDataSourceVo.getWhere();
                break;
            default:
        }
        rdbmsReaderDto.setWhereParams(where);
        dataXJsonBuildDto.setRdbmsReader(rdbmsReaderDto);
    }
}
