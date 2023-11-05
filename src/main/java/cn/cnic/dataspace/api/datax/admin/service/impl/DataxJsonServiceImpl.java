package cn.cnic.dataspace.api.datax.admin.service.impl;

import cn.cnic.dataspace.api.datax.admin.core.util.I18nUtil;
import cn.cnic.dataspace.api.datax.admin.tool.sql.JdbcConnectionFactory;
import com.alibaba.fastjson.JSON;
import cn.cnic.dataspace.api.datax.admin.dto.DataXJsonBuildDto;
import cn.cnic.dataspace.api.datax.admin.entity.JobDatasource;
import cn.cnic.dataspace.api.datax.admin.service.DataxJsonService;
import cn.cnic.dataspace.api.datax.admin.service.JobDatasourceService;
import cn.cnic.dataspace.api.datax.admin.tool.datax.DataxJsonHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * Com.wugui.datax JSON Build Implementation Class
 */
@Service
public class DataxJsonServiceImpl implements DataxJsonService {

    @Autowired
    private JobDatasourceService jobJdbcDatasourceService;

    @Override
    public String buildJobJson(DataXJsonBuildDto dataXJsonBuildDto) {
        DataxJsonHelper dataxJsonHelper = new DataxJsonHelper();
        // reader
        JobDatasource readerDatasource = jobJdbcDatasourceService.getById(dataXJsonBuildDto.getReaderDatasourceId());
        // reader plugin init
        dataxJsonHelper.initReader(dataXJsonBuildDto, readerDatasource);
        // JobDatasource writerDatasource = jobJdbcDatasourceService.getById(dataXJsonBuildDto.getWriterDatasourceId());
        JobDatasource writerDatasource = new JdbcConnectionFactory(dataXJsonBuildDto.getRdbmsWriter().getDbName()).getDataSource();
        dataxJsonHelper.initWriter(dataXJsonBuildDto, writerDatasource);
        return JSON.toJSONString(dataxJsonHelper.buildJob());
    }
}
