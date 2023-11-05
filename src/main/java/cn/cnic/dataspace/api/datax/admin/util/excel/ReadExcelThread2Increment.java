package cn.cnic.dataspace.api.datax.admin.util.excel;

import cn.cnic.dataspace.api.datax.admin.entity.ImportExcelTask;
import cn.cnic.dataspace.api.datax.admin.mapper.ImportExcelTaskMapper;
import cn.cnic.dataspace.api.datax.admin.service.DataMappingLockService;
import com.alibaba.excel.EasyExcel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@Slf4j
public class ReadExcelThread2Increment extends Thread {

    private String path;

    private String isHeader;

    private String sheetName;

    private String dbName;

    private String tableName;

    private List<String> columns;

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
                    EasyExcel.read(path, new NoModelDataListener2Increment(dbName, tableName, columns)).sheet(sheetName).headRowNumber(Integer.parseInt(isHeader)).doRead();
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
                EasyExcel.read(path, new NoModelDataListener2Increment(dbName, tableName, columns)).sheet(sheetName).headRowNumber(0).doRead();
            }
            ImportExcelTask update = new ImportExcelTask();
            update.setId(importExcelTask.getId());
            update.setFinishDate(new Date());
            update.setStatus(2);
            update.setLog("解析入库成功");
            importExcelTaskMapper.updateById(update);
            // release lock
            dataMappingLockService.releaseLock(importExcelTask.getDataMappingId(), importExcelTask.getSpaceId(), true);
        } catch (Exception e) {
            log.error("解析excel入库失败");
            log.error(e.getMessage(), e);
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
}
