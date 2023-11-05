package cn.cnic.dataspace.api.datax.admin.service.impl;

import cn.cnic.dataspace.api.datax.admin.tool.database.TableInfo;
import cn.cnic.dataspace.api.util.CommonUtils;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.api.R;
import com.google.common.collect.Lists;
import cn.cnic.dataspace.api.datax.admin.entity.JobDatasource;
import cn.cnic.dataspace.api.datax.admin.service.DatasourceQueryService;
import cn.cnic.dataspace.api.datax.admin.service.JobDatasourceService;
import cn.cnic.dataspace.api.datax.admin.tool.query.*;
import cn.cnic.dataspace.api.datax.admin.util.JdbcConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * datasource query
 *
 * @author
 * @ClassName JdbcDatasourceQueryServiceImpl
 * @Version 1.0
 * @since 2019/7/31 20:51
 */
@Service
public class DatasourceQueryServiceImpl implements DatasourceQueryService {

    @Autowired
    private JobDatasourceService jobDatasourceService;

    @Override
    public List<String> getDBs(Long id) throws IOException {
        // Get Data Source Object
        JobDatasource datasource = jobDatasourceService.getById(id);
        // QueryTool assembly
        if (ObjectUtil.isNull(datasource)) {
            return Lists.newArrayList();
        }
        if (JdbcConstants.MONGODB.equals(datasource.getDatasource())) {
            return new MongoDBQueryTool(datasource).getDBNames();
        } else {
            return Lists.newArrayList();
        }
    }

    @Override
    public List<String> getTables(Long id, String tableSchema) throws IOException {
        // Get Data Source Object
        JobDatasource datasource = jobDatasourceService.getById(id);
        // QueryTool assembly
        if (ObjectUtil.isNull(datasource)) {
            return Lists.newArrayList();
        }
        if (JdbcConstants.MONGODB.equals(datasource.getDatasource())) {
            return new MongoDBQueryTool(datasource).getCollectionNames(datasource.getDatabaseName());
        } else {
            BaseQueryTool qTool = QueryToolFactory.getByDbType(datasource);
            if (StringUtils.isBlank(tableSchema)) {
                return qTool.getTableNames();
            } else {
                return qTool.getTableNames(tableSchema);
            }
        }
    }

    @Override
    public List<Map<String, Object>> getTableNameAndDataCount(Long datasourceId, String tableSchema) throws IOException {
        // Get Data Source Object
        JobDatasource datasource = jobDatasourceService.getById(datasourceId);
        // QueryTool assembly
        if (ObjectUtil.isNull(datasource)) {
            return Lists.newArrayList();
        }
        if (JdbcConstants.MONGODB.equals(datasource.getDatasource())) {
            MongoDBQueryTool mongoDBQueryTool = new MongoDBQueryTool(datasource);
            List<String> collectionNames = mongoDBQueryTool.getCollectionNames(datasource.getDatabaseName());
            if (CollectionUtils.isEmpty(collectionNames)) {
                return Lists.newArrayList();
            }
            List<Map<String, Object>> collect = collectionNames.stream().map(var -> {
                Map<String, Object> r = new HashMap<>();
                long dataCount = mongoDBQueryTool.getDataCount(var);
                r.put("tableName", var);
                r.put("dataCount", dataCount);
                return r;
            }).collect(Collectors.toList());
            return collect;
        } else {
            BaseQueryTool qTool = QueryToolFactory.getByDbType(datasource);
            List<String> tableNames = Lists.newArrayList();
            if (StringUtils.isBlank(tableSchema)) {
                tableNames = qTool.getTableNames();
            } else {
                tableNames = qTool.getTableNames(tableSchema);
            }
            if (CollectionUtils.isEmpty(tableNames)) {
                return Lists.newArrayList();
            }
            List<Map<String, Object>> collect = tableNames.stream().map(var -> {
                Map<String, Object> r = new HashMap<>();
                long dataCount = qTool.getDataCount(var);
                r.put("tableName", var);
                r.put("dataCount", dataCount);
                return r;
            }).collect(Collectors.toList());
            return collect;
        }
    }

    @Override
    public List<String> getTableSchema(Long id) {
        // Get Data Source Object
        JobDatasource datasource = jobDatasourceService.getById(id);
        // QueryTool assembly
        if (ObjectUtil.isNull(datasource)) {
            return Lists.newArrayList();
        }
        BaseQueryTool qTool = QueryToolFactory.getByDbType(datasource);
        return qTool.getTableSchema();
    }

    @Override
    public List<Map<String, Object>> getDatas(Long datasourceId, String tableName) throws IOException {
        // Get Data Source Object
        JobDatasource datasource = jobDatasourceService.getById(datasourceId);
        // QueryTool assembly
        if (ObjectUtil.isNull(datasource)) {
            return Lists.newArrayList();
        }
        if (JdbcConstants.MONGODB.equals(datasource.getDatasource())) {
            MongoDBQueryTool mongoDBQueryTool = new MongoDBQueryTool(datasource);
            return mongoDBQueryTool.getDatas(tableName);
        } else {
            BaseQueryTool queryTool = QueryToolFactory.getByDbType(datasource);
            return queryTool.getDatas(tableName);
        }
    }

    @Override
    public R<TableInfo> getTableInfo(Long dataSourceId, String tableName) throws IOException {
        // Get Data Source Object
        JobDatasource datasource = jobDatasourceService.getById(dataSourceId);
        // QueryTool assembly
        if (ObjectUtil.isNull(datasource)) {
            return R.failed(CommonUtils.messageInternational("datasource_not_found"));
        }
        if (JdbcConstants.MONGODB.equals(datasource.getDatasource())) {
            MongoDBQueryTool mongoDBQueryTool = new MongoDBQueryTool(datasource);
            return R.ok(mongoDBQueryTool.buildTableInfo(tableName));
        } else {
            BaseQueryTool qTool = QueryToolFactory.getByDbType(datasource);
            return R.ok(qTool.buildTableInfo(tableName));
        }
    }

    @Override
    public List<String> getCollectionNames(long id, String dbName) throws IOException {
        // Get Data Source Object
        JobDatasource datasource = jobDatasourceService.getById(id);
        // QueryTool assembly
        if (ObjectUtil.isNull(datasource)) {
            return Lists.newArrayList();
        }
        if (JdbcConstants.MONGODB.equals(datasource.getDatasource())) {
            return new MongoDBQueryTool(datasource).getCollectionNames(dbName);
        } else {
            return Lists.newArrayList();
        }
    }

    @Override
    public List<String> getColumns(Long id, String tableName) throws IOException {
        // Get Data Source Object
        JobDatasource datasource = jobDatasourceService.getById(id);
        // QueryTool assembly
        if (ObjectUtil.isNull(datasource)) {
            return Lists.newArrayList();
        }
        if (JdbcConstants.MONGODB.equals(datasource.getDatasource())) {
            return new MongoDBQueryTool(datasource).getColumns(tableName);
        } else {
            BaseQueryTool queryTool = QueryToolFactory.getByDbType(datasource);
            return queryTool.getColumnNames(tableName, datasource.getDatasource());
        }
    }

    @Override
    public List<String> getColumnsByQuerySql(Long datasourceId, String querySql) throws SQLException {
        // Get Data Source Object
        JobDatasource jdbcDatasource = jobDatasourceService.getById(datasourceId);
        // QueryTool assembly
        if (ObjectUtil.isNull(jdbcDatasource)) {
            return Lists.newArrayList();
        }
        BaseQueryTool queryTool = QueryToolFactory.getByDbType(jdbcDatasource);
        return queryTool.getColumnsByQuerySql(querySql);
    }
}
