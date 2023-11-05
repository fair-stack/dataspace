package cn.cnic.dataspace.api.datax.admin.tool.datax.dto;

import cn.cnic.dataspace.api.datax.admin.dto.ImportFromDataSourceVo;
import cn.cnic.dataspace.api.datax.admin.entity.JobDatasource;
import cn.cnic.dataspace.api.datax.admin.mapper.JobDatasourceMapper;
import cn.cnic.dataspace.api.datax.admin.util.JdbcConstants;

public class BuildDtoHelper {

    public BuildDto init(JobDatasource datasource, ImportFromDataSourceVo importFromDataSourceVo, JobDatasourceMapper jobDatasourceMapper) {
        if (JdbcConstants.MONGODB.equals(datasource.getDatasource())) {
            return new BuildMongoDBDto(importFromDataSourceVo, jobDatasourceMapper);
        } else {
            return new BuildRDBMSDto(importFromDataSourceVo, jobDatasourceMapper);
        }
    }
}
