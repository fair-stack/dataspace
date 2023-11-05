package cn.cnic.dataspace.api.quartz;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import cn.cnic.dataspace.api.config.SpringUtil;
import cn.cnic.dataspace.api.model.backup.BackupSpaceMain;
import cn.cnic.dataspace.api.model.backup.BackupSpaceSubtasks;
import cn.cnic.dataspace.api.model.backup.FtpHost;
import cn.cnic.dataspace.api.queue.FTPUtils;
import cn.cnic.dataspace.api.util.CommonUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

/**
 * @ Description:
 */
@Slf4j
@Component
public class MyJob implements Job {

    @SneakyThrows
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Date startDate = new Date();
        MongoTemplate mongoTemplate = SpringUtil.getBean(MongoTemplate.class);
        String jobId = context.getJobDetail().getKey().getName();
        log.info("任务运行开始-------- start --------" + jobId);
        FtpHost invoke = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("invoke").is(true)), FtpHost.class);
        BackupSpaceMain backupSpaceMain = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("jobId").is(jobId)), BackupSpaceMain.class);
        if (null == invoke || null == backupSpaceMain) {
            log.error(jobId + " ------ 任务无法运行: ftpHost: {}" + invoke);
            log.error(jobId + " ------ 任务无法运行: backupMain  : {}" + backupSpaceMain);
            return;
        }
        BackupUtils backupUtils = new BackupUtils(invoke);
        Date nextDate = backupUtils.getNextDate(startDate, backupSpaceMain.getExecutionCycle());
        // Create a subtask for this execution
        BackupSpaceSubtasks backupSpaceSubtasks = new BackupSpaceSubtasks();
        backupSpaceSubtasks.setId(CommonUtils.generateSnowflake());
        backupSpaceSubtasks.setJobId(jobId);
        backupSpaceSubtasks.setCreateTime(new Date());
        backupSpaceSubtasks.setStartTime(startDate);
        backupSpaceSubtasks.setDuration("等待统计");
        updateState(backupSpaceSubtasks, mongoTemplate, 1);
        FTPClient ftpClient;
        try {
            ftpClient = backupUtils.login();
        } catch (IOException e) {
            e.printStackTrace();
            // Link ftp failed
            updateState("连接ftp失败：{} " + e.getMessage(), backupSpaceSubtasks, mongoTemplate);
            updateMinTaskState(backupSpaceMain.getBackup_total(), 0, 1, startDate, nextDate, jobId, mongoTemplate);
            return;
        }
        String spacePath = backupSpaceMain.getSpacePath();
        File spaceFile = new File(spacePath);
        if (spaceFile.exists()) {
            // Statistical total
            try {
                backupUtils.getFileSize(spaceFile);
            } catch (Exception e) {
                e.printStackTrace();
                updateState("统计空间总量和文件数量时出现错误：{} " + e.getMessage(), backupSpaceSubtasks, mongoTemplate);
                updateMinTaskState(backupSpaceMain.getBackup_total(), 0, 1, startDate, nextDate, jobId, mongoTemplate);
                return;
            }
            long total_fileNum = backupUtils.getTotal_fileNum();
            long total_fileSize = backupUtils.getTotal_fileSize();
            backupSpaceSubtasks.setTotalFileNum(total_fileNum);
            backupSpaceSubtasks.setTotalSize(total_fileSize);
            try {
                // Switch working directory
                ftpClient.changeWorkingDirectory(new String(invoke.getPath().getBytes(), FTPUtils.CHARSET));
                // strategy
                backupUtils.deleteFile(ftpClient, backupSpaceMain.getStrategy(), backupSpaceMain.getSpaceId(), invoke.getPath());
                // upload
                backupUtils.strategy(spaceFile, ftpClient, backupSpaceMain.getSpaceId());
            } catch (Exception e) {
                e.printStackTrace();
                long fileNum = backupUtils.getFileNum();
                long progress = backupUtils.getProgress();
                backupSpaceSubtasks.setFileNum(fileNum);
                backupSpaceSubtasks.setSchedule(progress);
                updateState("上传中断：{} " + e.getMessage(), backupSpaceSubtasks, mongoTemplate);
                updateMinTaskState(backupSpaceMain.getBackup_total(), 0, 1, startDate, nextDate, jobId, mongoTemplate);
                return;
            } finally {
                try {
                    ftpClient.logout();
                    if (ftpClient.isConnected()) {
                        ftpClient.disconnect();
                    }
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
            Date endDate = new Date();
            backupSpaceSubtasks.setEndTime(endDate);
            backupSpaceSubtasks.setDuration(CommonUtils.getMin(backupSpaceSubtasks.getStartTime(), endDate));
            // success
            updateState(backupSpaceSubtasks, mongoTemplate, 2);
            updateMinTaskState(backupSpaceMain.getBackup_total() + total_fileSize, 1, 0, startDate, nextDate, jobId, mongoTemplate);
        } else {
            updateState("该空间地址不存在：{} " + backupSpaceMain.getSpaceId(), backupSpaceSubtasks, mongoTemplate);
            updateMinTaskState(backupSpaceMain.getBackup_total(), 0, 1, startDate, nextDate, jobId, mongoTemplate);
        }
        log.info("任务运行结束-------- end --------" + jobId);
        return;
    }

    private void updateState(String error, BackupSpaceSubtasks backupSpaceSubtasks, MongoTemplate mongoTemplate) {
        backupSpaceSubtasks.setState(3);
        Date endDate = new Date();
        backupSpaceSubtasks.setEndTime(endDate);
        backupSpaceSubtasks.setDuration(CommonUtils.getMin(backupSpaceSubtasks.getStartTime(), endDate));
        backupSpaceSubtasks.setError(error);
        mongoTemplate.save(backupSpaceSubtasks);
        return;
    }

    private void updateState(BackupSpaceSubtasks backupSpaceSubtasks, MongoTemplate mongoTemplate, int state) {
        backupSpaceSubtasks.setState(state);
        mongoTemplate.save(backupSpaceSubtasks);
        return;
    }

    private void updateMinTaskState(long total, int success, int error, Date date, Date dataTo, String jobId, MongoTemplate mongoTemplate) {
        Query query = new Query().addCriteria(Criteria.where("jobId").is(jobId));
        Update update = new Update();
        update.set("backup_total", total);
        update.set("recentlyTime", date);
        update.set("nextTime", dataTo);
        update.inc("success", success);
        update.inc("error", error);
        mongoTemplate.upsert(query, update, BackupSpaceMain.class);
        return;
    }
}
