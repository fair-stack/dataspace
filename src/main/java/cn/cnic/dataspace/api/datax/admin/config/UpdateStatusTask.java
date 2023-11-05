package cn.cnic.dataspace.api.datax.admin.config;

import cn.cnic.dataspace.api.datax.admin.entity.DataMappingLock;
import cn.cnic.dataspace.api.datax.admin.entity.ExportExcelTask;
import cn.cnic.dataspace.api.datax.admin.entity.ImportExcelTask;
import cn.cnic.dataspace.api.datax.admin.entity.JobLog;
import cn.cnic.dataspace.api.datax.admin.mapper.DataMappingLockMapper;
import cn.cnic.dataspace.api.datax.admin.mapper.ExportExcelTaskMapper;
import cn.cnic.dataspace.api.datax.admin.mapper.ImportExcelTaskMapper;
import cn.cnic.dataspace.api.datax.admin.mapper.JobLogMapper;
import cn.cnic.dataspace.api.datax.admin.upgrade.DSManager;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import static cn.cnic.dataspace.api.datax.admin.config.Constant.SERVICE_RESTART;

/**
 * Updating the running task status to failed after service restart
 */
@Component
@Slf4j
@Order(10002)
public class UpdateStatusTask {

    @Resource
    private ExportExcelTaskMapper exportExcelTaskMapper;

    @Resource
    private ImportExcelTaskMapper importExcelTaskMapper;

    @Resource
    private DataMappingLockMapper dataMappingLockMapper;

    @Resource
    private JobLogMapper jobLogMapper;

    public void init() {
        List<DataMappingLock> dataMappingLocks = dataMappingLockMapper.selectList(null);
        for (DataMappingLock dataMappingLock : dataMappingLocks) {
            dataMappingLockMapper.deleteById(dataMappingLock.getDataMappingId());
            log.error("release lock id " + dataMappingLock.getDataMappingId());
        }
        QueryWrapper<ExportExcelTask> exportExcelTaskQueryWrapper = new QueryWrapper<>();
        exportExcelTaskQueryWrapper.eq("status", 1);
        List<ExportExcelTask> exportExcelTasks = exportExcelTaskMapper.selectList(exportExcelTaskQueryWrapper);
        for (ExportExcelTask exportExcelTask : exportExcelTasks) {
            ExportExcelTask update = new ExportExcelTask();
            update.setId(exportExcelTask.getId());
            update.setStatus(0);
            update.setLog(SERVICE_RESTART);
            exportExcelTaskMapper.updateById(update);
            log.info("---更新导出空间任务({})状态为失败---", exportExcelTask.getId());
        }
        QueryWrapper<ImportExcelTask> importExcelTaskQueryWrapper = new QueryWrapper<>();
        importExcelTaskQueryWrapper.eq("status", 1);
        List<ImportExcelTask> importExcelTasks = importExcelTaskMapper.selectList(importExcelTaskQueryWrapper);
        for (ImportExcelTask importExcelTask : importExcelTasks) {
            ImportExcelTask update = new ImportExcelTask();
            update.setId(importExcelTask.getId());
            update.setStatus(0);
            update.setLog(SERVICE_RESTART);
            importExcelTaskMapper.updateById(update);
            log.info("---更新离线导入excel任务({})状态为失败---", importExcelTask.getId());
        }
        // find running task
        List<JobLog> listByHandleCode = jobLogMapper.getListByHandleCode(0);
        for (JobLog jobLog : listByHandleCode) {
            JobLog update = new JobLog();
            update.setId(jobLog.getId());
            update.setHandleTime(new Date());
            update.setHandleCode(500);
            update.setHandleMsg(SERVICE_RESTART);
            jobLogMapper.updateHandleInfo(update);
            log.info("---更新在线导入任务({})状态为失败---", jobLog.getId());
        }
    }
}
