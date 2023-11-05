package cn.cnic.dataspace.api.datax.admin.controller;

import cn.cnic.dataspace.api.datax.core.biz.ExecutorBiz;
import cn.cnic.dataspace.api.datax.core.biz.model.LogResult;
import cn.cnic.dataspace.api.datax.core.biz.model.ReturnT;
import cn.cnic.dataspace.api.datax.core.glue.GlueTypeEnum;
import cn.cnic.dataspace.api.datax.core.util.DateUtil;
import cn.cnic.dataspace.api.datax.admin.core.kill.KillJob;
import cn.cnic.dataspace.api.datax.admin.core.scheduler.JobScheduler;
import cn.cnic.dataspace.api.datax.admin.core.util.I18nUtil;
import cn.cnic.dataspace.api.datax.admin.entity.JobInfo;
import cn.cnic.dataspace.api.datax.admin.entity.JobLog;
import cn.cnic.dataspace.api.datax.admin.mapper.JobInfoMapper;
import cn.cnic.dataspace.api.datax.admin.mapper.JobLogMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by  on 2019/11/17
 */
@RestController
@RequestMapping("/api/log")
@Api(tags = "任务运行日志接口")
public class JobLogController {

    private static Logger logger = LoggerFactory.getLogger(JobLogController.class);

    @Resource
    public JobInfoMapper jobInfoMapper;

    @Resource
    public JobLogMapper jobLogMapper;

    @GetMapping("/pageList")
    @ApiOperation("运行日志列表")
    public ReturnT<Map<String, Object>> pageList(@RequestParam(required = false, defaultValue = "0") int current, @RequestParam(required = false, defaultValue = "10") int size, int jobGroup, int jobId, int logStatus, String filterTime) {
        // valid permission
        // JobInfoController. validPermission (request, jobGroup); //Only administrators support querying all; Ordinary users only support querying job groups with permissions
        // parse param
        Date triggerTimeStart = null;
        Date triggerTimeEnd = null;
        if (filterTime != null && filterTime.trim().length() > 0) {
            String[] temp = filterTime.split(" - ");
            if (temp.length == 2) {
                triggerTimeStart = DateUtil.parseDateTime(temp[0]);
                triggerTimeEnd = DateUtil.parseDateTime(temp[1]);
            }
        }
        // page query
        List<JobLog> data = jobLogMapper.pageList((current - 1) * size, size, jobGroup, jobId, triggerTimeStart, triggerTimeEnd, logStatus);
        int cnt = jobLogMapper.pageListCount((current - 1) * size, size, jobGroup, jobId, triggerTimeStart, triggerTimeEnd, logStatus);
        // package result
        Map<String, Object> maps = new HashMap<>();
        // Total Records
        maps.put("recordsTotal", cnt);
        // Total number of filtered records
        maps.put("recordsFiltered", cnt);
        // Pagination List
        maps.put("data", data);
        return new ReturnT<>(maps);
    }

    @RequestMapping(value = "/logDetailCat", method = RequestMethod.GET)
    @ApiOperation("运行日志详情")
    public ReturnT<LogResult> logDetailCat(String executorAddress, long triggerTime, long logId, int fromLineNum) {
        try {
            ExecutorBiz executorBiz = JobScheduler.getExecutorBiz(executorAddress);
            ReturnT<LogResult> logResult = executorBiz.log(triggerTime, logId, fromLineNum);
            // is end
            if (logResult.getContent() != null && fromLineNum > logResult.getContent().getToLineNum()) {
                JobLog jobLog = jobLogMapper.load(logId);
                if (jobLog.getHandleCode() > 0) {
                    logResult.getContent().setEnd(true);
                }
            }
            return logResult;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new ReturnT<>(ReturnT.FAIL_CODE, e.getMessage());
        }
    }

    @RequestMapping(value = "/logKill", method = RequestMethod.POST)
    @ApiOperation("kill任务")
    public ReturnT<String> logKill(long id) {
        // base check
        JobLog log = jobLogMapper.load(id);
        JobInfo jobInfo = jobInfoMapper.selectById(log.getJobId());
        if (jobInfo == null) {
            return new ReturnT<>(500, I18nUtil.getString("jobinfo_glue_jobid_invalid"));
        }
        if (ReturnT.SUCCESS_CODE != log.getTriggerCode()) {
            return new ReturnT<>(500, I18nUtil.getString("joblog_kill_log_limit"));
        }
        // request of kill
        ReturnT<String> runResult;
        try {
            ExecutorBiz executorBiz = JobScheduler.getExecutorBiz(log.getExecutorAddress());
            runResult = executorBiz.kill(jobInfo.getId());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            runResult = new ReturnT<>(500, e.getMessage());
        }
        if (ReturnT.SUCCESS_CODE == runResult.getCode()) {
            log.setHandleCode(ReturnT.FAIL_CODE);
            log.setHandleMsg(I18nUtil.getString("joblog_kill_log_byman") + ":" + (runResult.getMsg() != null ? runResult.getMsg() : ""));
            log.setHandleTime(new Date());
            jobLogMapper.updateHandleInfo(log);
            return new ReturnT<>(runResult.getMsg());
        } else {
            return new ReturnT<>(500, runResult.getMsg());
        }
    }

    @PostMapping("/clearLog")
    @ApiOperation("清理日志")
    public ReturnT<String> clearLog(int jobGroup, int jobId, int type) {
        Date clearBeforeTime = null;
        int clearBeforeNum = 0;
        if (type == 1) {
            // Clean up log data from a month ago
            clearBeforeTime = DateUtil.addMonths(new Date(), -1);
        } else if (type == 2) {
            // Clean up log data three months ago
            clearBeforeTime = DateUtil.addMonths(new Date(), -3);
        } else if (type == 3) {
            // Clean up log data from six months ago
            clearBeforeTime = DateUtil.addMonths(new Date(), -6);
        } else if (type == 4) {
            // Clean up log data from a year ago
            clearBeforeTime = DateUtil.addYears(new Date(), -1);
        } else if (type == 5) {
            // Clean up a thousand previous log data
            clearBeforeNum = 1000;
        } else if (type == 6) {
            // Clean up 10000 previous log data
            clearBeforeNum = 10000;
        } else if (type == 7) {
            // Clean up 30000 previous log data
            clearBeforeNum = 30000;
        } else if (type == 8) {
            // Clean up 100000 previous log data
            clearBeforeNum = 100000;
        } else if (type == 9) {
            // Clean up all log data
            clearBeforeNum = 0;
        } else {
            return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("joblog_clean_type_invalid"));
        }
        List<Long> logIds;
        do {
            logIds = jobLogMapper.findClearLogIds(jobGroup, jobId, clearBeforeTime, clearBeforeNum, 1000);
            if (logIds != null && logIds.size() > 0) {
                jobLogMapper.clearLog(logIds);
            }
        } while (logIds != null && logIds.size() > 0);
        return ReturnT.SUCCESS;
    }

    @ApiOperation("停止该job作业")
    @PostMapping("/killJob")
    public ReturnT<String> killJob(@RequestBody JobLog log) {
        JobInfo jobInfo = jobInfoMapper.selectById(log.getJobId());
        if (GlueTypeEnum.match(jobInfo.getGlueType()) == GlueTypeEnum.DATAX || GlueTypeEnum.match(jobInfo.getGlueType()).isScript()) {
            return KillJob.trigger(log.getId(), log.getTriggerTime(), log.getExecutorAddress(), log.getProcessId());
        } else {
            return this.logKill(log.getId());
        }
    }
}
