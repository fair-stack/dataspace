package cn.cnic.dataspace.api.datax.admin.service.impl;

import cn.cnic.dataspace.api.datax.admin.dto.*;
import cn.cnic.dataspace.api.datax.admin.entity.DataMapping;
import cn.cnic.dataspace.api.datax.admin.entity.JobInfo;
import cn.cnic.dataspace.api.datax.admin.enums.SourceType;
import cn.cnic.dataspace.api.datax.admin.mapper.DataMappingMapper;
import cn.cnic.dataspace.api.datax.admin.service.DataMappingLockService;
import cn.cnic.dataspace.api.datax.admin.service.DataMappingOperService;
import cn.cnic.dataspace.api.datax.admin.service.DataMappingService;
import cn.cnic.dataspace.api.datax.admin.service.JobService;
import cn.cnic.dataspace.api.datax.admin.tool.database.ColumnInfo;
import cn.cnic.dataspace.api.datax.admin.tool.sql.CommonDBUtils;
import cn.cnic.dataspace.api.datax.admin.tool.sql.JdbcConnectionFactory;
import cn.cnic.dataspace.api.datax.admin.tool.sql.SqlUtils;
import cn.cnic.dataspace.api.datax.admin.util.JdbcConstants;
import cn.cnic.dataspace.api.datax.admin.util.excel.NoModelDataListener;
import cn.cnic.dataspace.api.datax.core.biz.model.ReturnT;
import cn.cnic.dataspace.api.model.space.Space;
import cn.cnic.dataspace.api.model.space.SpaceSvnLog;
import cn.cnic.dataspace.api.model.space.child.Operator;
import cn.cnic.dataspace.api.repository.SpaceRepository;
import cn.cnic.dataspace.api.repository.SvnSpaceLogRepository;
import cn.cnic.dataspace.api.repository.UserRepository;
import cn.cnic.dataspace.api.util.CommonUtils;
import com.baomidou.mybatisplus.extension.api.IErrorCode;
import com.baomidou.mybatisplus.extension.api.R;
import com.google.common.collect.Lists;
// import com.mysql.jdbc.MysqlDataTruncation;
import com.mysql.cj.jdbc.exceptions.MysqlDataTruncation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import javax.annotation.Resource;
import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class DataMappingOperServiceImpl implements DataMappingOperService {

    @Resource
    private DataMappingMapper dataMappingMapper;

    @Resource
    private SpaceRepository spaceRepository;

    @Resource
    private SvnSpaceLogRepository svnSpaceLogRepository;

    @Resource
    private UserRepository userRepository;

    @Resource
    private DataMappingService dataMappingService;

    @Resource
    private JobService jobService;

    @Resource
    private DataMappingLockService dataMappingLockService;

    @Override
    public R<Boolean> updateData(String spaceId, String userId, Long dataMappingId, String primaryKeyVal, String col, String data) {
        DataMapping dataMapping = dataMappingMapper.getByPrimaryKeyAndSpaceId(dataMappingId, spaceId);
        if (dataMapping == null) {
            return R.failed(CommonUtils.messageInternational("structured_data_not_found"));
        }
        if (!dataMappingLockService.tryLockDataMapping(dataMappingId, spaceId, false)) {
            return R.failed(CommonUtils.messageInternational("data_lock"));
        }
        Space space = spaceRepository.findById(spaceId).get();
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = new JdbcConnectionFactory(space.getDbName()).getConnection();
            ArrayList<String> params = Lists.newArrayList(col);
            String updateSql = SqlUtils.generateUpdateSql(dataMapping.getName(), params);
            preparedStatement = connection.prepareStatement(updateSql);
            if (StringUtils.isEmpty(data)) {
                preparedStatement.setString(1, null);
            } else {
                preparedStatement.setString(1, data);
            }
            preparedStatement.setString(2, primaryKeyVal);
            preparedStatement.executeUpdate();
            // AddLog (spaceId, userId, String. format ("Update structured data (% s) id (% s) column (% s) to% s", dataMapping. getName(), primaryKeyVal, col, data));
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            if (e instanceof MysqlDataTruncation) {
                return R.failed(CommonUtils.messageInternational("update_data_type_error"));
            }
            if (StringUtils.isNotEmpty(e.getMessage()) && e.getMessage().startsWith("Data truncated")) {
                return R.failed(CommonUtils.messageInternational("update_data_type_error"));
            }
            return R.failed(CommonUtils.messageInternational("update_data_failed"));
        } finally {
            dataMappingLockService.releaseLock(dataMappingId, spaceId, false);
            CommonDBUtils.closeDBResources(connection);
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        DataMapping update = new DataMapping();
        update.setId(dataMappingId);
        update.setUpdateBy(userId);
        dataMappingMapper.updateById(update);
        return R.ok(true);
    }

    @Override
    public R<Boolean> alterColNameAndType(String spaceId, String currentUserId, Long dataMappingId, String oldColName, String newColName, String type) {
        DataMapping dataMapping = dataMappingMapper.getByPrimaryKeyAndSpaceId(dataMappingId, spaceId);
        if (dataMapping == null) {
            return R.failed(CommonUtils.messageInternational("structured_data_not_found"));
        }
        if (!dataMappingLockService.tryLockDataMapping(dataMappingId, spaceId, false)) {
            return R.failed(CommonUtils.messageInternational("data_lock"));
        }
        Space space = spaceRepository.findById(spaceId).get();
        String alterColSql = SqlUtils.generateAlterColSql(space.getDbName(), dataMapping.getName(), oldColName, newColName, type);
        Connection connection = null;
        try {
            connection = new JdbcConnectionFactory(space.getDbName()).getConnection();
            CommonDBUtils.executeSql(connection, alterColSql);
            addLog(spaceId, currentUserId, String.format("结构化数据 (%s) 更改列 (%s) 为 (%s)", dataMapping.getName(), oldColName, newColName + type));
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            dataMappingLockService.releaseLock(dataMappingId, spaceId, false);
            IErrorCode code = new IErrorCode() {

                @Override
                public long getCode() {
                    return 502;
                }

                @Override
                public String getMsg() {
                    return getErrorMsg(e, newColName, type);
                }
            };
            return R.failed(code);
            // return R.ok(true);
        } finally {
            CommonDBUtils.closeDBResources(connection);
        }
        DataMapping update = new DataMapping();
        update.setId(dataMappingId);
        update.setUpdateBy(currentUserId);
        dataMappingMapper.updateById(update);
        // update jobInfo
        if (dataMapping.getSourceType() == SourceType.DATASOURCE) {
            if (!oldColName.equals(newColName)) {
                JobInfo jobByDataMappingId = jobService.getJobByDataMappingId(dataMappingId);
                if (jobByDataMappingId != null) {
                    ImportFromDataSourceVo importFromDataSourceVo = jobByDataMappingId.getImportFromDataSourceVo();
                    List<ColumnInfo> oldWriterColumns = importFromDataSourceVo.getWriterColumns();
                    oldWriterColumns.forEach(var -> {
                        if (var.getName().equals(oldColName)) {
                            var.setName(newColName);
                            var.setType(type);
                        }
                    });
                    importFromDataSourceVo.setUpdateDate(new Date().getTime());
                    jobService.update(jobByDataMappingId);
                }
            }
        }
        dataMappingLockService.releaseLock(dataMappingId, spaceId, true);
        return R.ok(true);
    }

    private String getErrorMsg(Exception e, String colName, String colType) {
        String message = e.getMessage();
        if (message.contains("Data truncation: Incorrect") || message.contains("Data truncated for column")) {
            return String.format(CommonUtils.messageInternational("alter_col_failed_type_error"), colType);
        } else if (message.contains("Duplicate column name")) {
            return String.format(CommonUtils.messageInternational("column_name_duplicate"), colName);
        } else {
            return CommonUtils.messageInternational("alter_col_failed");
        }
    }

    @Override
    public R<Boolean> dropCol(String spaceId, String currentUserId, DropColVO dropColVO) {
        DataMapping dataMapping = dataMappingMapper.getByPrimaryKeyAndSpaceId(dropColVO.getDataMappingId(), spaceId);
        if (dataMapping == null) {
            return R.failed(CommonUtils.messageInternational("structured_data_not_found"));
        }
        if (CollectionUtils.isEmpty(dropColVO.getColName())) {
            return R.ok(true);
        }
        if (!dataMappingLockService.tryLockDataMapping(dataMapping.getId(), spaceId, false)) {
            return R.failed(CommonUtils.messageInternational("data_lock"));
        }
        Space space = spaceRepository.findById(spaceId).get();
        Connection connection = null;
        try {
            connection = new JdbcConnectionFactory(space.getDbName()).getConnection();
            for (String col : dropColVO.getColName()) {
                String dropColSql = SqlUtils.generateDropColSql(space.getDbName(), dataMapping.getName(), col);
                CommonDBUtils.executeSql(connection, dropColSql);
                addLog(spaceId, currentUserId, String.format("结构化数据 (%s) 删除列 (%s)", dataMapping.getName(), col));
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            dataMappingLockService.releaseLock(dataMapping.getId(), spaceId, true);
            return R.failed(CommonUtils.messageInternational("drop_col_failed"));
        } finally {
            CommonDBUtils.closeDBResources(connection);
        }
        DataMapping update = new DataMapping();
        update.setId(dropColVO.getDataMappingId());
        update.setUpdateBy(currentUserId);
        dataMappingMapper.updateById(update);
        // update jobInfo
        if (dataMapping.getSourceType() == SourceType.DATASOURCE) {
            JobInfo jobByDataMappingId = jobService.getJobByDataMappingId(dropColVO.getDataMappingId());
            if (jobByDataMappingId != null) {
                ImportFromDataSourceVo importFromDataSourceVo = jobByDataMappingId.getImportFromDataSourceVo();
                List<ColumnInfo> oldWriterColumns = importFromDataSourceVo.getWriterColumns();
                List<ColumnInfo> newWriterColumns = Lists.newArrayList();
                oldWriterColumns.forEach(var -> {
                    if (!dropColVO.getColName().contains(var.getName())) {
                        // If the column to be deleted does not include this column
                        newWriterColumns.add(var);
                    }
                });
                importFromDataSourceVo.setWriterColumns(newWriterColumns);
                importFromDataSourceVo.setUpdateDate(new Date().getTime());
                ReturnT<String> returnT = jobService.update(jobByDataMappingId);
                log.error(returnT.getMsg());
            }
        }
        dataMappingLockService.releaseLock(dataMapping.getId(), spaceId, true);
        return R.ok(true);
    }

    @Override
    public R<Boolean> addEmptyCol(String spaceId, String currentUserId, AddEmptyColVO addEmptyColVO) {
        DataMapping dataMapping = dataMappingMapper.getByPrimaryKeyAndSpaceId(addEmptyColVO.getDataMappingId(), spaceId);
        if (dataMapping == null) {
            return R.failed(CommonUtils.messageInternational("structured_data_not_found"));
        }
        if (addEmptyColVO.getCount() < 1) {
            return R.failed(CommonUtils.messageInternational("param_error"));
        }
        if (!dataMappingLockService.tryLockDataMapping(dataMapping.getId(), spaceId, false)) {
            return R.failed(CommonUtils.messageInternational("data_lock"));
        }
        Space space = spaceRepository.findById(spaceId).get();
        Connection connection = null;
        ResultSet resultSet = null;
        List<String> successAddCol = new ArrayList<>();
        try {
            connection = new JdbcConnectionFactory(space.getDbName()).getConnection();
            if ("behind".equals(addEmptyColVO.getPosition())) {
                // Add Later
                for (int i = addEmptyColVO.getCount(); i > 0; i--) {
                    String addColName = JdbcConstants.COL_PRE + NoModelDataListener.getRandomStringByLength(4);
                    String addColSql = SqlUtils.generateAddAfterColSql(space.getDbName(), dataMapping.getName(), addEmptyColVO.getColName(), addColName, "文本");
                    CommonDBUtils.executeSql(connection, addColSql);
                    successAddCol.add(addColName);
                }
            } else {
                // Previously added
                // The column name of the previous column
                String preColName = "";
                String selectSqlNoResult = SqlUtils.generateSelectSqlNoResult(space.getDbName(), dataMapping.getName());
                resultSet = CommonDBUtils.query(connection, selectSqlNoResult, 1);
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                for (int i = 0; i < columnCount; i++) {
                    // Query the column names of the previous column
                    String columnLabel = metaData.getColumnLabel(i + 1);
                    if (columnLabel.equals(addEmptyColVO.getColName())) {
                        break;
                    }
                    preColName = columnLabel;
                }
                resultSet.close();
                for (int i = 0; i < addEmptyColVO.getCount(); i++) {
                    String addColName = JdbcConstants.COL_PRE + NoModelDataListener.getRandomStringByLength(4);
                    String addColSql = SqlUtils.generateAddAfterColSql(space.getDbName(), dataMapping.getName(), preColName, addColName, "文本");
                    CommonDBUtils.executeSql(connection, addColSql);
                    successAddCol.add(addColName);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            dataMappingLockService.releaseLock(dataMapping.getId(), spaceId, true);
            dropCol(connection, space.getDbName(), dataMapping.getName(), successAddCol);
            return R.failed(CommonUtils.messageInternational("add_col_failed"));
        } finally {
            CommonDBUtils.closeDBResources(connection);
        }
        DataMapping update = new DataMapping();
        update.setId(addEmptyColVO.getDataMappingId());
        update.setUpdateBy(currentUserId);
        dataMappingMapper.updateById(update);
        // add Log
        successAddCol.forEach(var -> {
            addLog(spaceId, currentUserId, String.format("结构化数据 (%s) 添加列 (%s) ", dataMapping.getName(), var));
        });
        // update jobInfo
        if (dataMapping.getSourceType() == SourceType.DATASOURCE) {
            JobInfo jobByDataMappingId = jobService.getJobByDataMappingId(addEmptyColVO.getDataMappingId());
            if (jobByDataMappingId != null) {
                ImportFromDataSourceVo importFromDataSourceVo = jobByDataMappingId.getImportFromDataSourceVo();
                List<ColumnInfo> oldWriterColumns = importFromDataSourceVo.getWriterColumns();
                successAddCol.forEach(var -> {
                    ColumnInfo columnInfo = new ColumnInfo();
                    columnInfo.setName(var);
                    columnInfo.setType("文本");
                    oldWriterColumns.add(columnInfo);
                });
                importFromDataSourceVo.setUpdateDate(new Date().getTime());
                ReturnT<String> returnT = jobService.update(jobByDataMappingId);
                if (returnT.getCode() != ReturnT.SUCCESS_CODE) {
                    log.error(returnT.getMsg());
                }
            }
        }
        dataMappingLockService.releaseLock(dataMapping.getId(), spaceId, true);
        return R.ok(true);
    }

    @Override
    public R<Boolean> addLine(String spaceId, String userId, AddLineVo addLineVo) {
        Long dataMappingId = addLineVo.getDataMappingId();
        DataMapping dataMapping = dataMappingMapper.getByPrimaryKeyAndSpaceId(dataMappingId, spaceId);
        if (dataMapping == null) {
            return R.failed(CommonUtils.messageInternational("structured_data_not_found"));
        }
        if (!dataMappingLockService.tryLockDataMapping(dataMapping.getId(), spaceId, false)) {
            return R.failed(CommonUtils.messageInternational("data_lock"));
        }
        Space space = spaceRepository.findById(spaceId).get();
        Map<String, String> data = addLineVo.getData();
        String insertSql = SqlUtils.generateInsertSql(space.getDbName(), dataMapping.getName(), Lists.newArrayList(data.keySet()));
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = new JdbcConnectionFactory(space.getDbName()).getConnection();
            List<String> vals = new ArrayList<>(data.values());
            preparedStatement = connection.prepareStatement(insertSql);
            for (int i = 0; i < vals.size(); i++) {
                String val = vals.get(i);
                if (StringUtils.isEmpty(val)) {
                    preparedStatement.setString(i + 1, null);
                } else {
                    preparedStatement.setString(i + 1, val);
                }
            }
            preparedStatement.execute();
            addLog(spaceId, userId, String.format("结构化数据(%s)添加一行数据", dataMapping.getName()));
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            if (e instanceof MysqlDataTruncation) {
                return R.failed(CommonUtils.messageInternational("add_line_type_error"));
            }
            return R.failed(CommonUtils.messageInternational("add_line_failed"));
        } finally {
            CommonDBUtils.closeDBResources(connection);
            dataMappingLockService.releaseLock(dataMappingId, spaceId, false);
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        DataMapping update = new DataMapping();
        update.setId(dataMappingId);
        update.setUpdateBy(userId);
        dataMappingMapper.updateById(update);
        return R.ok(true);
    }

    @Override
    public R<Boolean> copyLine(String spaceId, String userId, CopyLineVO copyLineVO) {
        Long dataMappingId = copyLineVO.getDataMappingId();
        DataMapping dataMapping = dataMappingMapper.getByPrimaryKeyAndSpaceId(dataMappingId, spaceId);
        if (dataMapping == null) {
            return R.failed(CommonUtils.messageInternational("structured_data_not_found"));
        }
        if (!dataMappingLockService.tryLockDataMapping(dataMapping.getId(), spaceId, false)) {
            return R.failed(CommonUtils.messageInternational("data_lock"));
        }
        Space space = spaceRepository.findById(spaceId).get();
        List<Map<String, String>> datas = copyLineVO.getDatas();
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = new JdbcConnectionFactory(space.getDbName()).getConnection();
            for (Map<String, String> data : datas) {
                String insertSql = SqlUtils.generateInsertSql(space.getDbName(), dataMapping.getName(), Lists.newArrayList(data.keySet()));
                List<String> vals = new ArrayList<>(data.values());
                preparedStatement = connection.prepareStatement(insertSql);
                for (int i = 0; i < vals.size(); i++) {
                    String val = vals.get(i);
                    if (StringUtils.isEmpty(val)) {
                        preparedStatement.setString(i + 1, null);
                    } else {
                        preparedStatement.setString(i + 1, val);
                    }
                }
                preparedStatement.execute();
                // AddLog (spaceId, userId, String. format ("Adding a row of structured data (% s)", dataMapping. getName()));
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            if (e instanceof MysqlDataTruncation) {
                return R.failed(CommonUtils.messageInternational("add_line_type_error"));
            }
            return R.failed(CommonUtils.messageInternational("add_line_failed"));
        } finally {
            CommonDBUtils.closeDBResources(connection);
            dataMappingLockService.releaseLock(dataMappingId, spaceId, false);
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        DataMapping update = new DataMapping();
        update.setId(dataMappingId);
        update.setUpdateBy(userId);
        dataMappingMapper.updateById(update);
        return R.ok(true);
    }

    @Override
    public R<Boolean> copyAddCol(String spaceId, String currentUserId, CopyAddColVO copyAddColVO) {
        DataMapping dataMapping = dataMappingMapper.getByPrimaryKeyAndSpaceId(copyAddColVO.getDataMappingId(), spaceId);
        if (dataMapping == null) {
            return R.failed(CommonUtils.messageInternational("structured_data_not_found"));
        }
        if (!dataMappingLockService.tryLockDataMapping(dataMapping.getId(), spaceId, false)) {
            return R.failed(CommonUtils.messageInternational("data_lock"));
        }
        Space space = spaceRepository.findById(spaceId).get();
        Connection connection = null;
        ResultSet resultSet = null;
        List<CopyAddColVO.AddCol> successAddCol = new ArrayList<>();
        try {
            connection = new JdbcConnectionFactory(space.getDbName()).getConnection();
            if ("behind".equals(copyAddColVO.getPosition())) {
                for (int i = copyAddColVO.getAddCols().size() - 1; i >= 0; i--) {
                    CopyAddColVO.AddCol addCol = copyAddColVO.getAddCols().get(i);
                    if (StringUtils.isEmpty(addCol.getNewColName())) {
                        addCol.setNewColName(addCol.getFromColName() + "_" + NoModelDataListener.getRandomStringByLength(4));
                    }
                    String addColSql = SqlUtils.generateAddAfterColSql(space.getDbName(), dataMapping.getName(), copyAddColVO.getColName(), addCol.getNewColName(), addCol.getFromColType());
                    CommonDBUtils.executeSql(connection, addColSql);
                    successAddCol.add(addCol);
                    String setValueSql = SqlUtils.generateSetValueSql(space.getDbName(), dataMapping.getName(), addCol.getFromColName(), addCol.getNewColName());
                    CommonDBUtils.executeSql(connection, setValueSql);
                }
            } else {
                // The column name of the previous column
                String preColName = "";
                String selectSqlNoResult = SqlUtils.generateSelectSqlNoResult(space.getDbName(), dataMapping.getName());
                resultSet = CommonDBUtils.query(connection, selectSqlNoResult, 1);
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                for (int j = 0; j < columnCount; j++) {
                    // Query the column names of the previous column
                    String columnLabel = metaData.getColumnLabel(j + 1);
                    if (columnLabel.equals(copyAddColVO.getColName())) {
                        break;
                    }
                    preColName = columnLabel;
                }
                resultSet.close();
                for (int i = copyAddColVO.getAddCols().size() - 1; i >= 0; i--) {
                    CopyAddColVO.AddCol addCol = copyAddColVO.getAddCols().get(i);
                    if (StringUtils.isEmpty(addCol.getNewColName())) {
                        addCol.setNewColName(addCol.getFromColName() + "_" + NoModelDataListener.getRandomStringByLength(4));
                    }
                    String addColSql = SqlUtils.generateAddAfterColSql(space.getDbName(), dataMapping.getName(), preColName, addCol.getNewColName(), addCol.getFromColType());
                    CommonDBUtils.executeSql(connection, addColSql);
                    successAddCol.add(addCol);
                    String setValueSql = SqlUtils.generateSetValueSql(space.getDbName(), dataMapping.getName(), addCol.getFromColName(), addCol.getNewColName());
                    CommonDBUtils.executeSql(connection, setValueSql);
                }
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            dataMappingLockService.releaseLock(dataMapping.getId(), spaceId, false);
            dropCol(connection, space.getDbName(), dataMapping.getName(), successAddCol.stream().map(var -> var.getNewColName()).collect(Collectors.toList()));
            return R.failed(CommonUtils.messageInternational("copy_failed"));
        } finally {
            CommonDBUtils.closeDBResources(connection);
        }
        // add Log
        successAddCol.forEach(var -> {
            addLog(spaceId, currentUserId, String.format("结构化数据(%s)复制列(%s)到(%s)", dataMapping.getName(), var.getFromColName(), var.getNewColName()));
        });
        DataMapping update = new DataMapping();
        update.setId(copyAddColVO.getDataMappingId());
        update.setUpdateBy(currentUserId);
        dataMappingMapper.updateById(update);
        // update jobInfo
        if (dataMapping.getSourceType() == SourceType.DATASOURCE) {
            JobInfo jobByDataMappingId = jobService.getJobByDataMappingId(copyAddColVO.getDataMappingId());
            if (jobByDataMappingId != null) {
                ImportFromDataSourceVo importFromDataSourceVo = jobByDataMappingId.getImportFromDataSourceVo();
                List<ColumnInfo> oldWriterColumns = importFromDataSourceVo.getWriterColumns();
                successAddCol.forEach(var -> {
                    ColumnInfo columnInfo = new ColumnInfo();
                    columnInfo.setName(var.getNewColName());
                    columnInfo.setType(var.getFromColType());
                    oldWriterColumns.add(columnInfo);
                });
                importFromDataSourceVo.setUpdateDate(new Date().getTime());
                ReturnT<String> returnT = jobService.update(jobByDataMappingId);
                if (returnT.getCode() != ReturnT.SUCCESS_CODE) {
                    log.error(returnT.getMsg());
                }
            }
        }
        dataMappingLockService.releaseLock(dataMapping.getId(), spaceId, true);
        return R.ok(true);
    }

    @Override
    public R<Boolean> setCol2Null(String spaceId, String currentUserId, SetCol2NullVO setCol2NullVO) {
        DataMapping dataMapping = dataMappingMapper.getByPrimaryKeyAndSpaceId(setCol2NullVO.getDataMappingId(), spaceId);
        if (dataMapping == null) {
            return R.failed(CommonUtils.messageInternational("structured_data_not_found"));
        }
        if (CollectionUtils.isEmpty(setCol2NullVO.getColName())) {
            return R.ok(true);
        }
        if (!dataMappingLockService.tryLockDataMapping(dataMapping.getId(), spaceId, false)) {
            return R.failed(CommonUtils.messageInternational("data_lock"));
        }
        Space space = spaceRepository.findById(spaceId).get();
        Connection connection = null;
        try {
            connection = new JdbcConnectionFactory(space.getDbName()).getConnection();
            for (String colName : setCol2NullVO.getColName()) {
                String setCol2NullSql = SqlUtils.generateSetCol2NullSql(space.getDbName(), dataMapping.getName(), colName);
                CommonDBUtils.executeSql(connection, setCol2NullSql);
                addLog(spaceId, currentUserId, String.format("结构化数据(%s)清空列(%s)", dataMapping.getName(), colName));
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            return R.failed(CommonUtils.messageInternational(""));
        } finally {
            CommonDBUtils.closeDBResources(connection);
            dataMappingLockService.releaseLock(dataMapping.getId(), spaceId, false);
        }
        DataMapping update = new DataMapping();
        update.setId(setCol2NullVO.getDataMappingId());
        update.setUpdateBy(currentUserId);
        dataMappingMapper.updateById(update);
        return R.ok(true);
    }

    @Override
    public R<Boolean> deleteLine(String spaceId, String userId, DeleteLineVO deleteLineVO) {
        DataMapping dataMapping = dataMappingMapper.getByPrimaryKeyAndSpaceId(deleteLineVO.getDataMappingId(), spaceId);
        if (dataMapping == null) {
            return R.failed(CommonUtils.messageInternational("structured_data_not_found"));
        }
        if (CollectionUtils.isEmpty(deleteLineVO.getPrimaryKey())) {
            return R.ok(true);
        }
        if (!dataMappingLockService.tryLockDataMapping(dataMapping.getId(), spaceId, false)) {
            return R.failed(CommonUtils.messageInternational("data_lock"));
        }
        Space space = spaceRepository.findById(spaceId).get();
        Connection connection = null;
        try {
            connection = new JdbcConnectionFactory(space.getDbName()).getConnection();
            for (String primaryKey : deleteLineVO.getPrimaryKey()) {
                String deleteByPrimarySql = SqlUtils.generateDeleteByPrimarySql(space.getDbName(), dataMapping.getName(), primaryKey);
                CommonDBUtils.executeSql(connection, deleteByPrimarySql);
                // AddLog (spaceId, userId, String. format ("Delete a row of data id (% s) from structured data (% s)", dataMapping. getName(), primaryKey));
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            return R.failed(CommonUtils.messageInternational("delete_data_failed"));
        } finally {
            CommonDBUtils.closeDBResources(connection);
            dataMappingLockService.releaseLock(dataMapping.getId(), spaceId, false);
        }
        DataMapping update = new DataMapping();
        update.setId(deleteLineVO.getDataMappingId());
        update.setUpdateBy(userId);
        dataMappingMapper.updateById(update);
        return R.ok(true);
    }

    @Override
    public R<Map<String, Object>> searchData(String spaceId, String userId, String dataMappingId, String searchVal, Integer current, Integer size) {
        DataMapping dataMapping = dataMappingMapper.getByPrimaryKeyAndSpaceId(dataMappingId, spaceId);
        if (dataMapping == null) {
            return R.failed(CommonUtils.messageInternational("structured_data_not_found"));
        }
        if (StringUtils.isEmpty(searchVal)) {
            return dataMappingService.getData(spaceId, userId, dataMappingId, 1, current, size);
        }
        if (!dataMappingLockService.tryLockDataMapping(dataMapping.getId(), spaceId, false)) {
            Map<String, Object> ret = new HashMap<>();
            ret.put("total", 0);
            ret.put("current", 1);
            ret.put("colInfos", Lists.newArrayList());
            ret.put("size", 10);
            ret.put("records", Lists.newArrayList());
            return R.ok(ret);
        }
        // data
        List<List<Object>> lines = new ArrayList<>();
        Space space = spaceRepository.findById(spaceId).get();
        Connection connection = null;
        try {
            connection = new JdbcConnectionFactory(space.getDbName()).getConnection();
            String count = "0";
            String selectSqlNoResult = SqlUtils.generateSelectSqlNoResult(space.getDbName(), dataMapping.getName());
            ResultSet query1 = CommonDBUtils.query(connection, selectSqlNoResult, 1);
            ResultSetMetaData metaData = query1.getMetaData();
            int columnCount = metaData.getColumnCount();
            // Field Name Type
            List<Map<String, String>> colInfos = new ArrayList<>();
            // List<QueryDataMappingDataVO.QueryFilter> filters = new ArrayList<>();
            // Fuzzy query conditions
            Map<String, QueryDataMappingDataVO.QuerySortFilter> querySortFilterMap = new HashMap<>();
            for (int i = 0; i < columnCount; i++) {
                Map<String, String> colInfo = new HashMap<>();
                String columnLabel = metaData.getColumnLabel(i + 1);
                String columnTypeName = metaData.getColumnTypeName(i + 1);
                colInfo.put("key", columnLabel);
                colInfo.put("type", SqlUtils.getViewType(columnTypeName));
                colInfos.add(colInfo);
                if (!JdbcConstants.PRIMARY_KEY.equals(columnLabel)) {
                    // Do not query automatically generated IDs as criteria
                    QueryDataMappingDataVO.QuerySortFilter filter = new QueryDataMappingDataVO.QuerySortFilter();
                    filter.setFilterType(1);
                    filter.setFilter(new QueryDataMappingDataVO.QueryFilter().setCondition(new QueryDataMappingDataVO.Condition().setOper("文本包含").setValue(searchVal)));
                    querySortFilterMap.put(columnLabel, filter);
                }
            }
            // select count
            String selectCountByFilterOrSql = SqlUtils.generateSelectCountByFilterOrSql(space.getDbName(), dataMapping.getName(), querySortFilterMap);
            ResultSet query = CommonDBUtils.query(connection, selectCountByFilterOrSql, 1);
            while (query.next()) {
                count = query.getString(1);
            }
            query.close();
            // select data
            String selectBySortAndFilterOrSql = SqlUtils.generateSelectBySortAndFilterOrSql(space.getDbName(), dataMapping.getName(), (current - 1) * size, size, querySortFilterMap);
            ResultSet query2 = CommonDBUtils.query(connection, selectBySortAndFilterOrSql, 100);
            while (query2.next()) {
                List<Object> line = new ArrayList<>();
                for (int i = 0; i < columnCount; i++) {
                    Object object = query2.getObject(i + 1);
                    line.add(object);
                }
                lines.add(line);
            }
            query2.close();
            Map<String, Object> ret = new HashMap<>();
            ret.put("total", count);
            ret.put("current", current);
            ret.put("colInfos", colInfos);
            ret.put("size", size);
            ret.put("records", lines);
            return R.ok(ret);
        } catch (SQLException e) {
            log.error(CommonUtils.messageInternational("query_data_failed"));
            log.error(e.getMessage(), e);
            Map<String, Object> ret = new HashMap<>();
            ret.put("total", 0);
            ret.put("current", current);
            ret.put("colInfos", Lists.newArrayList());
            ret.put("size", size);
            ret.put("records", Lists.newArrayList());
            return R.ok(ret);
        } finally {
            dataMappingLockService.releaseLock(dataMapping.getId(), spaceId, false);
            CommonDBUtils.closeDBResources(connection);
        }
    }

    @Override
    public R<Boolean> toUpper(String spaceId, String currentUserId, Long dataMappingId, String colName) {
        DataMapping dataMapping = dataMappingMapper.getByPrimaryKeyAndSpaceId(dataMappingId, spaceId);
        if (dataMapping == null) {
            return R.failed(CommonUtils.messageInternational("structured_data_not_found"));
        }
        if (!dataMappingLockService.tryLockDataMapping(dataMapping.getId(), spaceId, false)) {
            return R.failed(CommonUtils.messageInternational("data_lock"));
        }
        Space space = spaceRepository.findById(spaceId).get();
        Connection connection = null;
        String toUpperSql = SqlUtils.generateToUpperSql(space.getDbName(), dataMapping.getName(), colName);
        try {
            connection = new JdbcConnectionFactory(space.getDbName()).getConnection();
            CommonDBUtils.executeSql(connection, toUpperSql);
            addLog(spaceId, currentUserId, String.format("结构化数据 (%s) 列 (%s)转为大写", dataMapping.getName(), colName));
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            return R.ok(true);
        } finally {
            CommonDBUtils.closeDBResources(connection);
            dataMappingLockService.releaseLock(dataMappingId, spaceId, false);
        }
        return R.ok(true);
    }

    @Override
    public R<Boolean> toLower(String spaceId, String currentUserId, Long dataMappingId, String colName) {
        DataMapping dataMapping = dataMappingMapper.getByPrimaryKeyAndSpaceId(dataMappingId, spaceId);
        if (dataMapping == null) {
            return R.failed(CommonUtils.messageInternational("structured_data_not_found"));
        }
        if (!dataMappingLockService.tryLockDataMapping(dataMapping.getId(), spaceId, false)) {
            return R.failed(CommonUtils.messageInternational("data_lock"));
        }
        Space space = spaceRepository.findById(spaceId).get();
        Connection connection = null;
        String toLowerSql = SqlUtils.generateToLowerSql(space.getDbName(), dataMapping.getName(), colName);
        try {
            connection = new JdbcConnectionFactory(space.getDbName()).getConnection();
            CommonDBUtils.executeSql(connection, toLowerSql);
            addLog(spaceId, currentUserId, String.format("结构化数据 (%s) 列 (%s)转为小写", dataMapping.getName(), colName));
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            return R.ok(true);
        } finally {
            CommonDBUtils.closeDBResources(connection);
            dataMappingLockService.releaseLock(dataMappingId, spaceId, false);
        }
        return R.ok(true);
    }

    @Override
    public R<Boolean> addPrex(String spaceId, String currentUserId, Long dataMappingId, String colName, String prex) {
        DataMapping dataMapping = dataMappingMapper.getByPrimaryKeyAndSpaceId(dataMappingId, spaceId);
        if (dataMapping == null) {
            return R.failed(CommonUtils.messageInternational("structured_data_not_found"));
        }
        if (!dataMappingLockService.tryLockDataMapping(dataMapping.getId(), spaceId, false)) {
            return R.failed(CommonUtils.messageInternational("data_lock"));
        }
        Space space = spaceRepository.findById(spaceId).get();
        Connection connection = null;
        String addPrexSql = SqlUtils.generateAddPrexSql(space.getDbName(), dataMapping.getName(), colName, prex);
        try {
            connection = new JdbcConnectionFactory(space.getDbName()).getConnection();
            CommonDBUtils.executeSql(connection, addPrexSql);
            addLog(spaceId, currentUserId, String.format("结构化数据 (%s) 列 (%s)增加前缀(%s)", dataMapping.getName(), colName, prex));
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            return R.ok(true);
        } finally {
            CommonDBUtils.closeDBResources(connection);
            dataMappingLockService.releaseLock(dataMappingId, spaceId, false);
        }
        return R.ok(true);
    }

    @Override
    public R<Boolean> addSuffix(String spaceId, String currentUserId, Long dataMappingId, String colName, String suffix) {
        DataMapping dataMapping = dataMappingMapper.getByPrimaryKeyAndSpaceId(dataMappingId, spaceId);
        if (dataMapping == null) {
            return R.failed(CommonUtils.messageInternational("structured_data_not_found"));
        }
        if (!dataMappingLockService.tryLockDataMapping(dataMapping.getId(), spaceId, false)) {
            return R.failed(CommonUtils.messageInternational("data_lock"));
        }
        Space space = spaceRepository.findById(spaceId).get();
        Connection connection = null;
        String suffixSql = SqlUtils.generateAddSuffixSql(space.getDbName(), dataMapping.getName(), colName, suffix);
        try {
            connection = new JdbcConnectionFactory(space.getDbName()).getConnection();
            CommonDBUtils.executeSql(connection, suffixSql);
            addLog(spaceId, currentUserId, String.format("结构化数据 (%s) 列 (%s)增加后缀(%s)", dataMapping.getName(), colName, suffix));
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            return R.ok(true);
        } finally {
            CommonDBUtils.closeDBResources(connection);
            dataMappingLockService.releaseLock(dataMappingId, spaceId, false);
        }
        return R.ok(true);
    }

    @Override
    public R<Boolean> replace(String spaceId, String currentUserId, Long dataMappingId, String colName, String search, String replace) {
        DataMapping dataMapping = dataMappingMapper.getByPrimaryKeyAndSpaceId(dataMappingId, spaceId);
        if (dataMapping == null) {
            return R.failed(CommonUtils.messageInternational("structured_data_not_found"));
        }
        if (!dataMappingLockService.tryLockDataMapping(dataMapping.getId(), spaceId, false)) {
            return R.failed(CommonUtils.messageInternational("data_lock"));
        }
        Space space = spaceRepository.findById(spaceId).get();
        Connection connection = null;
        String replaceSql = SqlUtils.generateReplaceSql(space.getDbName(), dataMapping.getName(), colName, search, replace);
        try {
            connection = new JdbcConnectionFactory(space.getDbName()).getConnection();
            CommonDBUtils.executeSql(connection, replaceSql);
            addLog(spaceId, currentUserId, String.format("结构化数据 (%s) 列 (%s) 替换内容 (%s) 为 (%s)", dataMapping.getName(), colName, search, replace));
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            return R.ok(true);
        } finally {
            CommonDBUtils.closeDBResources(connection);
            dataMappingLockService.releaseLock(dataMappingId, spaceId, false);
        }
        return R.ok(true);
    }

    @Override
    public R<List<Map<String, String>>> getGroupVal(String spaceId, String currentUserId, QueryDataMappingGroupVO queryDataMappingGroupVO) {
        DataMapping dataMapping = dataMappingMapper.getByPrimaryKeyAndSpaceId(queryDataMappingGroupVO.getDataMappingId(), spaceId);
        if (dataMapping == null) {
            return R.failed(CommonUtils.messageInternational("structured_data_not_found"));
        }
        if (!dataMappingLockService.tryLockDataMapping(dataMapping.getId(), spaceId, false)) {
            return R.failed(CommonUtils.messageInternational("data_lock"));
        }
        Space space = spaceRepository.findById(dataMapping.getSpaceId()).get();
        List<Map<String, String>> vals = new ArrayList<>();
        Connection connection = null;
        ResultSet resultSet = null;
        try {
            connection = new JdbcConnectionFactory(space.getDbName()).getConnection();
            String groupBySql = SqlUtils.generateGroupBySql(space.getDbName(), dataMapping.getName(), queryDataMappingGroupVO);
            resultSet = CommonDBUtils.query(connection, groupBySql, 10);
            while (resultSet.next()) {
                Map<String, String> col = new HashMap<>();
                String colName = resultSet.getString(1);
                // String count = resultSet.getString(2);
                col.put("col", colName);
                col.put("count", "0");
                vals.add(col);
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            return R.ok(vals);
        } finally {
            dataMappingLockService.releaseLock(dataMapping.getId(), spaceId, false);
            CommonDBUtils.closeDBResources(resultSet, connection);
        }
        return R.ok(vals);
    }

    @Override
    public R<Boolean> mergeCol(String spaceId, String currentUserId, MergeColVO mergeColVO) {
        if (CollectionUtils.isEmpty(mergeColVO.getColName()) || mergeColVO.getColName().size() < 2) {
            return R.failed(CommonUtils.messageInternational("param_error"));
        }
        for (String col : mergeColVO.getColName()) {
            if (StringUtils.isEmpty(col)) {
                return R.failed(CommonUtils.messageInternational("param_error"));
            }
        }
        DataMapping dataMapping = dataMappingMapper.getByPrimaryKeyAndSpaceId(mergeColVO.getDataMappingId(), spaceId);
        if (dataMapping == null) {
            return R.failed(CommonUtils.messageInternational("structured_data_not_found"));
        }
        if (!dataMappingLockService.tryLockDataMapping(dataMapping.getId(), spaceId, true)) {
            return R.failed(CommonUtils.messageInternational("data_lock"));
        }
        Space space = spaceRepository.findById(spaceId).get();
        if (StringUtils.isEmpty(mergeColVO.getNewCol())) {
            // If the new field name is empty, the default naming convention is field 1_ Field 2_ Field 3
            mergeColVO.setNewCol(String.join("_", mergeColVO.getColName()));
        }
        if (StringUtils.isEmpty(mergeColVO.getSplit())) {
            // If the merge separator is empty, use '' by default
            mergeColVO.setSplit("");
        }
        // Last column name
        String preCol = mergeColVO.getColName().get(mergeColVO.getColName().size() - 1);
        String addColSql = SqlUtils.generateAddAfterColSql(space.getDbName(), dataMapping.getName(), preCol, mergeColVO.getNewCol(), "文本");
        Connection connection = null;
        try {
            connection = new JdbcConnectionFactory(space.getDbName()).getConnection();
            CommonDBUtils.executeSql(connection, addColSql);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            dataMappingLockService.releaseLock(dataMapping.getId(), spaceId, false);
            CommonDBUtils.closeDBResources(connection);
            // return R.ok(true);
            IErrorCode code = new IErrorCode() {

                @Override
                public long getCode() {
                    return 502;
                }

                @Override
                public String getMsg() {
                    return getErrorMsg(e, mergeColVO.getNewCol(), "");
                }
            };
            return R.failed(code);
            // return R.failed(getErrorMsg(e, mergeColVO.getNewCol(), ""));
        }
        try {
            String mergeSql = SqlUtils.generateMergeSql(space.getDbName(), dataMapping.getName(), mergeColVO.getColName(), mergeColVO.getNewCol(), mergeColVO.getSplit());
            CommonDBUtils.executeSql(connection, mergeSql);
            addLog(spaceId, currentUserId, String.format("结构化数据 (%s) 列 (%s)合并到列(%s)", dataMapping.getName(), String.join(",", mergeColVO.getColName()), mergeColVO.getNewCol()));
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            dataMappingLockService.releaseLock(dataMapping.getId(), spaceId, true);
            return R.ok(true);
            // return R.failed(CommonUtils.messageInternational(""));
        } finally {
            CommonDBUtils.closeDBResources(connection);
        }
        // update jobInfo
        if (dataMapping.getSourceType() == SourceType.DATASOURCE) {
            JobInfo jobByDataMappingId = jobService.getJobByDataMappingId(mergeColVO.getDataMappingId());
            if (jobByDataMappingId != null) {
                ImportFromDataSourceVo importFromDataSourceVo = jobByDataMappingId.getImportFromDataSourceVo();
                List<ColumnInfo> oldWriterColumns = importFromDataSourceVo.getWriterColumns();
                ColumnInfo columnInfo = new ColumnInfo();
                columnInfo.setName(mergeColVO.getNewCol());
                columnInfo.setType("文本");
                oldWriterColumns.add(columnInfo);
                importFromDataSourceVo.setUpdateDate(new Date().getTime());
                ReturnT<String> returnT = jobService.update(jobByDataMappingId);
                log.error(returnT.getMsg());
            }
        }
        dataMappingLockService.releaseLock(dataMapping.getId(), spaceId, true);
        return R.ok(true);
    }

    @Override
    public R<Boolean> split(String spaceId, String currentUserId, Long dataMappingId, String splitCol, String split, String left, String right) {
        if (StringUtils.isAnyEmpty(splitCol, split)) {
            return R.failed(CommonUtils.messageInternational("param_error"));
        }
        DataMapping dataMapping = dataMappingMapper.getByPrimaryKeyAndSpaceId(dataMappingId, spaceId);
        if (dataMapping == null) {
            return R.failed(CommonUtils.messageInternational("structured_data_not_found"));
        }
        if (!dataMappingLockService.tryLockDataMapping(dataMapping.getId(), spaceId, false)) {
            return R.failed(CommonUtils.messageInternational("data_lock"));
        }
        if (StringUtils.isEmpty(left)) {
            left = splitCol + "_left";
        }
        if (StringUtils.isEmpty(right)) {
            right = splitCol + "_right";
        }
        Space space = spaceRepository.findById(spaceId).get();
        Connection connection = null;
        String addLeftColSql = SqlUtils.generateAddAfterColSql(space.getDbName(), dataMapping.getName(), splitCol, left, "文本");
        String addRightColSql = SqlUtils.generateAddAfterColSql(space.getDbName(), dataMapping.getName(), left, right, "文本");
        String splitColSql = SqlUtils.generateSplitColSql(space.getDbName(), dataMapping.getName(), splitCol, left, right, split);
        List<String> successAddCol = new ArrayList<>();
        String currentCol = "";
        try {
            connection = new JdbcConnectionFactory(space.getDbName()).getConnection();
            currentCol = left;
            CommonDBUtils.executeSql(connection, addLeftColSql);
            successAddCol.add(left);
            currentCol = right;
            CommonDBUtils.executeSql(connection, addRightColSql);
            successAddCol.add(right);
        } catch (SQLException e) {
            dataMappingLockService.releaseLock(dataMappingId, spaceId, true);
            dropCol(connection, space.getDbName(), dataMapping.getName(), successAddCol);
            CommonDBUtils.closeDBResources(connection);
            log.error(e.getMessage(), e);
            final String finalCol = currentCol;
            IErrorCode code = new IErrorCode() {

                @Override
                public long getCode() {
                    return 502;
                }

                @Override
                public String getMsg() {
                    return getErrorMsg(e, finalCol, "");
                }
            };
            return R.failed(code);
            // return R.failed(getErrorMsg(e, currentCol, ""));
        }
        try {
            CommonDBUtils.executeSql(connection, splitColSql);
            addLog(spaceId, currentUserId, String.format("结构化数据 (%s) 列 (%s) 拆分为 (%s),(%s)", dataMapping.getName(), splitCol, left, right));
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        } finally {
            CommonDBUtils.closeDBResources(connection);
        }
        // update jobInfo
        if (dataMapping.getSourceType() == SourceType.DATASOURCE) {
            JobInfo jobByDataMappingId = jobService.getJobByDataMappingId(dataMappingId);
            if (jobByDataMappingId != null) {
                ImportFromDataSourceVo importFromDataSourceVo = jobByDataMappingId.getImportFromDataSourceVo();
                List<ColumnInfo> oldWriterColumns = importFromDataSourceVo.getWriterColumns();
                ColumnInfo leftColumnInfo = new ColumnInfo();
                leftColumnInfo.setName(left);
                leftColumnInfo.setType("文本");
                oldWriterColumns.add(leftColumnInfo);
                ColumnInfo rightColumnInfo = new ColumnInfo();
                rightColumnInfo.setName(right);
                rightColumnInfo.setType("文本");
                oldWriterColumns.add(rightColumnInfo);
                oldWriterColumns.add(rightColumnInfo);
                importFromDataSourceVo.setUpdateDate(new Date().getTime());
                ReturnT<String> returnT = jobService.update(jobByDataMappingId);
                log.error(returnT.getMsg());
            }
        }
        dataMappingLockService.releaseLock(dataMappingId, spaceId, true);
        return R.ok(true);
    }

    /**
     * Batch Delete Columns
     */
    private void dropCol(Connection connection, String dbName, String tableName, List<String> cols) {
        cols.forEach(var -> {
            String dropColSql = SqlUtils.generateDropColSql(dbName, tableName, var);
            try {
                CommonDBUtils.executeSql(connection, dropColSql);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        });
    }

    /**
     * Add Action Log
     */
    private void addLog(String spaceId, String userId, String desc) {
        svnSpaceLogRepository.save(SpaceSvnLog.builder().spaceId(spaceId).version(SpaceSvnLog.ACTION_VALUE).description(desc).action(SpaceSvnLog.ACTION_TABLE).operatorId(userId).operator(new Operator(userRepository.findById(userId).get())).createTime(new Date()).build());
    }
}
