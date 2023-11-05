package cn.cnic.dataspace.api.datax.admin.service.impl;

import cn.cnic.dataspace.api.datax.admin.core.scheduler.JobScheduler;
import cn.cnic.dataspace.api.datax.admin.dto.DataMappingTaskVO;
import cn.cnic.dataspace.api.datax.admin.entity.ExportExcelTask;
import cn.cnic.dataspace.api.datax.admin.entity.ImportExcelTask;
import cn.cnic.dataspace.api.datax.admin.entity.JobLog;
import cn.cnic.dataspace.api.datax.admin.mapper.ExportExcelTaskMapper;
import cn.cnic.dataspace.api.datax.admin.mapper.ImportExcelTaskMapper;
import cn.cnic.dataspace.api.datax.admin.mapper.JobLogMapper;
import cn.cnic.dataspace.api.datax.admin.mapper.TaskMapper;
import cn.cnic.dataspace.api.datax.admin.service.DataMappingTaskService;
import cn.cnic.dataspace.api.datax.core.biz.ExecutorBiz;
import cn.cnic.dataspace.api.datax.core.biz.model.LogResult;
import cn.cnic.dataspace.api.datax.core.biz.model.ReturnT;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@Slf4j
public class DataMappingTaskServiceImpl implements DataMappingTaskService {

    @Resource
    private TaskMapper taskMapper;

    @Resource
    private JobLogMapper jobLogMapper;

    @Override
    public IPage<DataMappingTaskVO> getPaging(String spaceId, Long dataMappingId, Integer status, Integer current, Integer size) {
        IPage<DataMappingTaskVO> page = new Page<>(current, size);
        IPage<DataMappingTaskVO> dataMappingTaskVOIPage = taskMapper.selectByPaging(page, dataMappingId, status, spaceId);
        List<DataMappingTaskVO> dataMappingTaskVOS = dataMappingTaskVOIPage.getRecords();
        for (DataMappingTaskVO dataMappingTaskVO : dataMappingTaskVOS) {
            if ("DatasourceImport".equals(dataMappingTaskVO.getTaskType())) {
                try {
                    JobLog jobLog = jobLogMapper.load(dataMappingTaskVO.getId());
                    ExecutorBiz executorBiz = JobScheduler.getExecutorBiz(jobLog.getExecutorAddress());
                    ReturnT<LogResult> logResult = executorBiz.log(jobLog.getTriggerTime().getTime(), jobLog.getId(), 1);
                    if (logResult.getContent() != null) {
                        dataMappingTaskVO.setLog(logResult.getContent().getLogContent());
                    }
                    // is end
                    if (logResult.getContent() != null && 1 > logResult.getContent().getToLineNum()) {
                        if (jobLog.getHandleCode() > 0) {
                            logResult.getContent().setEnd(true);
                        }
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return dataMappingTaskVOIPage;
    }

    @Override
    public List<DataMappingTaskVO> getAll(String spaceId, Long dataMappingId, Integer status) {
        List<DataMappingTaskVO> dataMappingTaskVOS = taskMapper.selectList(dataMappingId, status, spaceId);
        for (DataMappingTaskVO dataMappingTaskVO : dataMappingTaskVOS) {
            if ("DatasourceImport".equals(dataMappingTaskVO.getTaskType())) {
                try {
                    JobLog jobLog = jobLogMapper.load(dataMappingTaskVO.getId());
                    ExecutorBiz executorBiz = JobScheduler.getExecutorBiz(jobLog.getExecutorAddress());
                    ReturnT<LogResult> logResult = executorBiz.log(jobLog.getTriggerTime().getTime(), jobLog.getId(), 1);
                    if (logResult.getContent() != null) {
                        dataMappingTaskVO.setLog(logResult.getContent().getLogContent());
                    }
                    // is end
                    if (logResult.getContent() != null && 1 > logResult.getContent().getToLineNum()) {
                        if (jobLog.getHandleCode() > 0) {
                            logResult.getContent().setEnd(true);
                        }
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return taskMapper.selectList(dataMappingId, status, spaceId);
    }
}
