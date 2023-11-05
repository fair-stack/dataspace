package cn.cnic.dataspace.api.datax.admin.tool.datax.dto;

import cn.cnic.dataspace.api.datax.admin.dto.HiveReaderDto;
import cn.cnic.dataspace.api.datax.admin.dto.ImportFromDataSourceVo;
import cn.cnic.dataspace.api.datax.admin.mapper.JobDatasourceMapper;

public class BuildHiveDto extends BuildDto {

    public BuildHiveDto(ImportFromDataSourceVo importFromDataSourceVo, JobDatasourceMapper jobDatasourceMapper) {
        super(importFromDataSourceVo, jobDatasourceMapper);
    }

    @Override
    void buildHiveReader() {
        HiveReaderDto hiveReaderDto = new HiveReaderDto();
        dataXJsonBuildDto.setHiveReader(hiveReaderDto);
    }
}
