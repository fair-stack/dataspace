package cn.cnic.dataspace.api.datax.admin.service.impl;

import cn.cnic.dataspace.api.datax.admin.dto.ImportFromDataSourceVo;
import cn.cnic.dataspace.api.datax.admin.service.DataMappingService;
import cn.cnic.dataspace.api.datax.admin.service.DatasourceQueryService;
import cn.cnic.dataspace.api.datax.admin.tool.database.ColumnInfo;
import cn.cnic.dataspace.api.datax.admin.tool.database.TableInfo;
import cn.cnic.dataspace.api.datax.core.util.Constants;
import cn.cnic.dataspace.api.util.CommonUtils;
import cn.cnic.dataspace.api.util.RSAEncrypt;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.enums.ApiErrorCode;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.cnic.dataspace.api.datax.admin.entity.JobDatasource;
import cn.cnic.dataspace.api.datax.admin.mapper.JobDatasourceMapper;
import cn.cnic.dataspace.api.datax.admin.service.JobDatasourceService;
import cn.cnic.dataspace.api.datax.admin.tool.query.BaseQueryTool;
import cn.cnic.dataspace.api.datax.admin.tool.query.MongoDBQueryTool;
import cn.cnic.dataspace.api.datax.admin.tool.query.QueryToolFactory;
import cn.cnic.dataspace.api.datax.admin.util.AESUtil;
import cn.cnic.dataspace.api.datax.admin.util.JdbcConstants;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.CollectionUtils;
import javax.annotation.Resource;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by  on 2020/01/30
 */
@Service
@Transactional
public class JobDatasourceServiceImpl extends ServiceImpl<JobDatasourceMapper, JobDatasource> implements JobDatasourceService {

    @Resource
    private JobDatasourceMapper datasourceMapper;

    @Resource
    private DatasourceQueryService datasourceQueryService;

    @Resource
    private DataMappingService dataMappingService;

    @Override
    public Boolean dataSourceTest(JobDatasource jobDatasource) throws IOException {
        String userName = AESUtil.decrypt(jobDatasource.getJdbcUsername());
        // Determine whether the account secret is ciphertext
        if (userName == null) {
            jobDatasource.setJdbcUsername(AESUtil.encrypt(jobDatasource.getJdbcUsername()));
        }
        String pwd = AESUtil.decrypt(jobDatasource.getJdbcPassword());
        if (pwd == null) {
            jobDatasource.setJdbcPassword(AESUtil.encrypt(jobDatasource.getJdbcPassword()));
        }
        if (JdbcConstants.MONGODB.equals(jobDatasource.getDatasource())) {
            return new MongoDBQueryTool(jobDatasource).dataSourceTest(jobDatasource.getDatabaseName());
        }
        BaseQueryTool queryTool = QueryToolFactory.getByDbType(jobDatasource);
        return queryTool.dataSourceTest();
    }

    @Override
    public int update(JobDatasource datasource) {
        return datasourceMapper.update(datasource);
    }

    @Override
    public List<JobDatasource> selectAllDatasource(String spaceId) {
        QueryWrapper<JobDatasource> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id,datasource_name,datasource,space_id");
        queryWrapper.eq("space_id", spaceId);
        return datasourceMapper.selectList(queryWrapper);
    }

    @Override
    public List<JobDatasource> selectHotDatasource(String spaceId) {
        List<String> hotType = datasourceMapper.selectHostDataSourceType(spaceId);
        if (CollectionUtils.isEmpty(hotType)) {
            return Lists.newArrayList();
        }
        List<JobDatasource> hostDatasource = hotType.stream().map(var -> {
            return datasourceMapper.selectTopOneBySourceType(spaceId, var);
        }).collect(Collectors.toList());
        return hostDatasource;
    }

    @Override
    public List<String> selectHostDatasourceType(String spaceId) {
        List<String> hotType = datasourceMapper.selectHostDataSourceType(spaceId);
        if (CollectionUtils.isEmpty(hotType)) {
            return Lists.newArrayList();
        }
        return hotType;
    }

    @Override
    public R<Boolean> saveAndImport(String spaceId, String userId, JobDatasource entity) throws IOException {
        try {
            String first = RSAEncrypt.decrypt(entity.getJdbcPassword());
            if (first == null) {
                return R.failed(CommonUtils.messageInternational("param_error"));
            }
            entity.setJdbcPassword(first);
            datasourceMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            log.error(e.getMessage(), e);
            return R.failed(CommonUtils.messageInternational("datasource_name_duplicate"));
        }
        Long id = entity.getId();
        JobDatasource selectById = datasourceMapper.selectById(id);
        List<ColumnInfo> cols = Lists.newArrayList();
        if (JdbcConstants.MONGODB.equals(entity.getDatasource())) {
            MongoDBQueryTool mongoDBQueryTool = new MongoDBQueryTool(selectById);
            List<String> columns = mongoDBQueryTool.getColumns(entity.getTableName());
            cols = columns.stream().map(var -> {
                ColumnInfo c = new ColumnInfo();
                c.setName(var.split(Constants.SPLIT_SCOLON)[0]);
                c.setType("longtext");
                return c;
            }).collect(Collectors.toList());
        } else {
            BaseQueryTool byDbType = QueryToolFactory.getByDbType(selectById);
            TableInfo tableInfo = byDbType.buildTableInfo(entity.getTableName());
            cols = tableInfo.getColumns();
        }
        ImportFromDataSourceVo importFromDataSourceVo = new ImportFromDataSourceVo();
        importFromDataSourceVo.setDatasourceId(id);
        importFromDataSourceVo.setReaderTableName(entity.getTableName());
        importFromDataSourceVo.setSpaceId(spaceId);
        importFromDataSourceVo.setWhereType(0);
        // Set default
        importFromDataSourceVo.setCron("00 * * ? * * *");
        importFromDataSourceVo.setImportType(0);
        importFromDataSourceVo.setWriterTableName(entity.getTableName());
        importFromDataSourceVo.setReaderColumns(cols);
        importFromDataSourceVo.setWriterColumns(cols);
        R<Boolean> result = dataMappingService.importFromDataSource(spaceId, userId, importFromDataSourceVo);
        if (ApiErrorCode.FAILED.getCode() == result.getCode()) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> dataSourceTestAndReturnTablesAndDataCount(JobDatasource datasource) throws IOException {
        // QueryTool assembly
        if (ObjectUtil.isNull(datasource)) {
            return Lists.newArrayList();
        }
        String userName = AESUtil.decrypt(datasource.getJdbcUsername());
        // Determine whether the account secret is ciphertext
        if (userName == null) {
            datasource.setJdbcUsername(AESUtil.encrypt(datasource.getJdbcUsername()));
        }
        String pwd = AESUtil.decrypt(datasource.getJdbcPassword());
        if (pwd == null) {
            datasource.setJdbcPassword(AESUtil.encrypt(datasource.getJdbcPassword()));
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
            tableNames = qTool.getTableNames();
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
    public List<String> dataSourceTestAndReturnTables(JobDatasource datasource) throws IOException {
        // QueryTool assembly
        if (ObjectUtil.isNull(datasource)) {
            return Lists.newArrayList();
        }
        String userName = AESUtil.decrypt(datasource.getJdbcUsername());
        // Determine whether the account secret is ciphertext
        if (userName == null) {
            datasource.setJdbcUsername(AESUtil.encrypt(datasource.getJdbcUsername()));
        }
        String pwd = AESUtil.decrypt(datasource.getJdbcPassword());
        if (pwd == null) {
            datasource.setJdbcPassword(AESUtil.encrypt(datasource.getJdbcPassword()));
        }
        if (JdbcConstants.MONGODB.equals(datasource.getDatasource())) {
            MongoDBQueryTool mongoDBQueryTool = new MongoDBQueryTool(datasource);
            List<String> collectionNames = mongoDBQueryTool.getCollectionNames(datasource.getDatabaseName());
            if (CollectionUtils.isEmpty(collectionNames)) {
                return Lists.newArrayList();
            }
            return collectionNames;
        } else {
            BaseQueryTool qTool = QueryToolFactory.getByDbType(datasource);
            List<String> tableNames = Lists.newArrayList();
            tableNames = qTool.getTableNames();
            if (CollectionUtils.isEmpty(tableNames)) {
                return Lists.newArrayList();
            }
            return tableNames;
        }
    }

    @Override
    public R<Long> selectCountByTableName(String datasourceId, String tableName) {
        JobDatasource datasource = datasourceMapper.selectById(datasourceId);
        if (ObjectUtil.isNull(datasource)) {
            return R.ok(0L);
        }
        String userName = AESUtil.decrypt(datasource.getJdbcUsername());
        // Determine whether the account secret is ciphertext
        if (userName == null) {
            datasource.setJdbcUsername(AESUtil.encrypt(datasource.getJdbcUsername()));
        }
        String pwd = AESUtil.decrypt(datasource.getJdbcPassword());
        if (pwd == null) {
            datasource.setJdbcPassword(AESUtil.encrypt(datasource.getJdbcPassword()));
        }
        if (JdbcConstants.MONGODB.equals(datasource.getDatasource())) {
            MongoDBQueryTool mongoDBQueryTool = new MongoDBQueryTool(datasource);
            long dataCount = mongoDBQueryTool.getDataCount(tableName);
            return R.ok(dataCount);
        } else {
            BaseQueryTool qTool = QueryToolFactory.getByDbType(datasource);
            long dataCount = qTool.getDataCount(tableName);
            return R.ok(dataCount);
        }
    }
}
