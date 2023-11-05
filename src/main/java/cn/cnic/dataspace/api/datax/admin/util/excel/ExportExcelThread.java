package cn.cnic.dataspace.api.datax.admin.util.excel;

import cn.cnic.dataspace.api.config.space.FileOperationFactory;
import cn.cnic.dataspace.api.datax.admin.entity.ExportExcelTask;
import cn.cnic.dataspace.api.datax.admin.mapper.ExportExcelTaskMapper;
import cn.cnic.dataspace.api.model.space.child.Operator;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.nio.file.Paths;
import java.util.Date;
import static cn.cnic.dataspace.api.model.space.SpaceSvnLog.FILE_MODIFY;
import static cn.cnic.dataspace.api.model.space.SpaceSvnLog.TABLE;

@Data
@Slf4j
@AllArgsConstructor
public class ExportExcelThread extends Thread {

    private String targetPath;

    private String dbName;

    private String tableName;

    private ExportExcelTaskMapper exportExcelTaskMapper;

    private ExportExcelTask exportExcelTask;

    private Operator operator;

    @Override
    public void run() {
        try {
            ExportExcelUtils.export(targetPath, null, dbName, tableName);
            // Update spatial file metadata
            FileOperationFactory.getFileMappingManage().transit(FILE_MODIFY, exportExcelTask.getSpaceId(), Paths.get(targetPath), false, false, TABLE, new File(targetPath).length(), operator);
            updateStatus(2, "导出数据成功");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            updateStatus(0, "导出数据失败");
            return;
        }
    }

    private void updateStatus(Integer status, String log) {
        exportExcelTask.setStatus(status);
        exportExcelTask.setLog(log);
        exportExcelTask.setFinishDate(new Date());
        exportExcelTaskMapper.updateById(exportExcelTask);
    }
}
