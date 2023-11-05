package cn.cnic.dataspace.api.datax.admin.service;

import cn.cnic.dataspace.api.datax.admin.entity.JobDatasource;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.service.IService;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Jdbc data source configuration table service interface
 */
public interface JobDatasourceService extends IService<JobDatasource> {

    /**
     * Test Data Source
     */
    Boolean dataSourceTest(JobDatasource jdbcDatasource) throws IOException;

    /**
     * Update data source information
     */
    int update(JobDatasource datasource);

    /**
     * Get all data sources
     */
    List<JobDatasource> selectAllDatasource(String spaceId);

    /**
     * Obtain recently used data sources
     */
    List<JobDatasource> selectHotDatasource(String spaceId);

    List<String> selectHostDatasourceType(String spaceId);

    /**
     * Save data source information and import data for exams
     */
    R<Boolean> saveAndImport(String spaceId, String userId, JobDatasource entity) throws IOException;

    /**
     * Test data source connection. If successful, return all table information
     */
    List<Map<String, Object>> dataSourceTestAndReturnTablesAndDataCount(JobDatasource jobJdbcDatasource) throws IOException;

    List<String> dataSourceTestAndReturnTables(JobDatasource jobJdbcDatasource) throws IOException;

    /**
     * Query the amount of data in a table under the data source
     */
    R<Long> selectCountByTableName(String datasourceId, String tableName);
}
