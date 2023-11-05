package cn.cnic.dataspace.api.datax.admin.util.excel;

import cn.cnic.dataspace.api.datax.admin.entity.ImportExcelTask;
import cn.cnic.dataspace.api.datax.admin.mapper.ImportExcelTaskMapper;
import cn.cnic.dataspace.api.datax.admin.service.DataMappingLockService;
import cn.cnic.dataspace.api.datax.admin.tool.database.ColumnInfo;
import cn.cnic.dataspace.api.datax.admin.tool.database.TableInfo;
import cn.cnic.dataspace.api.datax.admin.tool.sql.CommonDBUtils;
import cn.cnic.dataspace.api.datax.admin.tool.sql.JdbcConnectionFactory;
import cn.cnic.dataspace.api.datax.admin.tool.sql.SqlUtils;
import com.alibaba.excel.EasyExcel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@Slf4j
public class ReadExcelThread extends Thread {

    private String path;

    private String isHeader;

    private String sheetName;

    private Integer sheetNum;

    private String dbName;

    private String tableName;

    private ImportExcelTask importExcelTask;

    private ImportExcelTaskMapper importExcelTaskMapper;

    private DataMappingLockService dataMappingLockService;

    @Override
    public void run() {
        try {
            // lock
            dataMappingLockService.tryLockDataMapping(importExcelTask.getDataMappingId(), importExcelTask.getSpaceId(), false);
            if (StringUtils.isNotEmpty(isHeader)) {
                if ("1".equals(isHeader) || "0".equals(isHeader)) {
                    if (sheetNum != null) {
                        // Default priority time sheetNum
                        EasyExcel.read(path, new NoModelDataListener(dbName, tableName, isHeader)).sheet(sheetNum).headRowNumber(Integer.parseInt(isHeader)).doRead();
                    } else {
                        EasyExcel.read(path, new NoModelDataListener(dbName, tableName, isHeader)).sheet(sheetName).headRowNumber(Integer.parseInt(isHeader)).doRead();
                    }
                } else {
                    log.error("param {isHeader} error, value is " + isHeader);
                    ImportExcelTask update = new ImportExcelTask();
                    update.setId(importExcelTask.getId());
                    update.setStatus(0);
                    update.setFinishDate(new Date());
                    update.setLog("解析excel入库失败," + "param {isHeader} error, value is " + isHeader);
                    importExcelTaskMapper.updateById(update);
                    // release lock
                    dataMappingLockService.releaseLock(importExcelTask.getDataMappingId(), importExcelTask.getSpaceId(), true);
                    return;
                }
            } else {
                if (sheetNum != null) {
                    // Default priority time sheetNum
                    EasyExcel.read(path, new NoModelDataListener(dbName, tableName, "0")).sheet(sheetNum).headRowNumber(0).doRead();
                } else {
                    EasyExcel.read(path, new NoModelDataListener(dbName, tableName, "0")).sheet(sheetName).headRowNumber(0).doRead();
                }
            }
            boolean isCreateTable = createTableByDefault();
            ImportExcelTask update = new ImportExcelTask();
            update.setId(importExcelTask.getId());
            update.setFinishDate(new Date());
            if (isCreateTable) {
                update.setStatus(2);
                update.setLog("解析入库成功");
            } else {
                update.setStatus(0);
                update.setLog("选择的sheet可能不存在或者excel是一个空文件");
            }
            importExcelTaskMapper.updateById(update);
            // release lock
            dataMappingLockService.releaseLock(importExcelTask.getDataMappingId(), importExcelTask.getSpaceId(), true);
        } catch (Exception e) {
            log.error("解析excel入库失败");
            log.error(e.getMessage(), e);
            boolean isCreateTable = createTableByDefault();
            ImportExcelTask update = new ImportExcelTask();
            update.setId(importExcelTask.getId());
            update.setStatus(0);
            update.setLog("解析excel入库失败," + e.getMessage());
            update.setFinishDate(new Date());
            importExcelTaskMapper.updateById(update);
            // release lock
            dataMappingLockService.releaseLock(importExcelTask.getDataMappingId(), importExcelTask.getSpaceId(), true);
        }
    }

    private boolean createTableByDefault() {
        // Verify if the table has been successfully created
        String selectSqlNoResult = SqlUtils.generateSelectSqlNoResult(dbName, tableName);
        Connection connection = null;
        boolean isCreateTable = true;
        try {
            connection = new JdbcConnectionFactory(dbName).getConnection();
            CommonDBUtils.query(connection, selectSqlNoResult, 1);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            isCreateTable = false;
            // Create default table
            try {
                log.info("使用默认字段建表");
                List<String> columnByIntNum = NoModelDataListener.getColumnByIntNum(10);
                TableInfo tableInfo = new TableInfo();
                tableInfo.setName(tableName);
                List<ColumnInfo> columnInfos = columnByIntNum.stream().map(var -> {
                    ColumnInfo columnInfo = new ColumnInfo();
                    columnInfo.setName(var);
                    return columnInfo;
                }).collect(Collectors.toList());
                tableInfo.setColumns(columnInfos);
                String createTableSqlWithDefaultId = SqlUtils.generateCreateTableSqlWithDefaultId(dbName, tableInfo);
                CommonDBUtils.executeSql(connection, createTableSqlWithDefaultId);
            } catch (Exception e1) {
                log.error(e1.getMessage(), e1);
            }
        } finally {
            CommonDBUtils.closeDBResources(connection);
        }
        return isCreateTable;
    }
}
