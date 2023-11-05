package cn.cnic.dataspace.api.datax.executor.service.jobhandler;

import cn.cnic.dataspace.api.datax.admin.entity.DataMapping;
import cn.cnic.dataspace.api.datax.admin.mapper.DataMappingMapper;
import cn.cnic.dataspace.api.datax.admin.service.DataMappingLockService;
import cn.cnic.dataspace.api.datax.admin.service.DataMappingService;
import cn.cnic.dataspace.api.datax.admin.service.JobService;
import cn.cnic.dataspace.api.datax.core.biz.model.HandleProcessCallbackParam;
import cn.cnic.dataspace.api.datax.core.biz.model.ReturnT;
import cn.cnic.dataspace.api.datax.core.biz.model.TriggerParam;
import cn.cnic.dataspace.api.datax.core.handler.IJobHandler;
import cn.cnic.dataspace.api.datax.core.handler.annotation.JobHandler;
import cn.cnic.dataspace.api.datax.core.log.JobLogger;
import cn.cnic.dataspace.api.datax.core.thread.ProcessCallbackThread;
import cn.cnic.dataspace.api.datax.core.util.ProcessUtil;
import cn.cnic.dataspace.api.datax.executor.service.logparse.LogStatistics;
import cn.cnic.dataspace.api.datax.executor.util.SystemUtils;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.FutureTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static cn.cnic.dataspace.api.datax.executor.service.command.BuildCommand.*;
import static cn.cnic.dataspace.api.datax.executor.service.jobhandler.DataXConstant.DEFAULT_JSON;
import static cn.cnic.dataspace.api.datax.executor.service.logparse.AnalysisStatistics.analysisStatisticsLog;

/**
 * DataX task running
 */
@JobHandler(value = "executorJobHandler")
@Component
@Slf4j
public class ExecutorJobHandler extends IJobHandler {

    @Value("${datax.executor.jsonpath}")
    private String jsonPath;

    @Value("${datax.pypath}")
    private String dataXPyPath;

    @Resource
    private DataMappingMapper dataMappingMapper;

    @Resource
    private JobService jobService;

    @Resource
    private DataMappingLockService dataMappingLockService;

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("(\\$)\\{?(\\w+)\\}?");

    @Override
    public ReturnT<String> execute(TriggerParam trigger) {
        int exitValue = -1;
        Thread errThread = null;
        String tmpFilePath;
        LogStatistics logStatistics = null;
        HashMap<String, String> keyValueMap = buildDataXParamToMap(trigger);
        String jobJson = replaceVariable(trigger.getJobJson(), keyValueMap);
        Map<String, String> buildin = builtInVar();
        jobJson = replaceVariable(jobJson, buildin);
        // Generate JSON temporary file
        tmpFilePath = generateTemJsonFile(jobJson);
        DataMapping dataMapping = dataMappingMapper.selectById(trigger.getDataMappingId());
        try {
            if (dataMapping == null) {
                log.error("---datax executor dataMapping not found, stop task---");
                int jobId = trigger.getJobId();
                jobService.stop(jobId);
                return new ReturnT<>(IJobHandler.FAIL.getCode(), "结构化数据可能已经被删除, 已经停止定时任务");
            }
            if (!dataMappingLockService.tryLockDataMapping(trigger.getDataMappingId(), dataMapping.getSpaceId(), true)) {
                // lock, no run this task
                log.error("---datax executor dataMapping lock, no run task---");
                return new ReturnT<>(IJobHandler.FAIL.getCode(), "结构化数据锁定中, 放弃本次执行任务");
            }
            String[] cmdarrayFinal = buildDataXExecutorCmd(trigger, tmpFilePath, dataXPyPath);
            final Process process = Runtime.getRuntime().exec(cmdarrayFinal);
            String prcsId = ProcessUtil.getProcessId(process);
            JobLogger.log("------------------DataX process id: " + prcsId);
            // jobTmpFiles.put(prcsId, tmpFilePath);
            // update datax process id
            HandleProcessCallbackParam prcs = new HandleProcessCallbackParam(trigger.getLogId(), trigger.getLogDateTime(), prcsId);
            ProcessCallbackThread.pushCallBack(prcs);
            // log-thread
            Thread futureThread = null;
            FutureTask<LogStatistics> futureTask = new FutureTask<>(() -> analysisStatisticsLog(new BufferedInputStream(process.getInputStream())));
            futureThread = new Thread(futureTask);
            futureThread.start();
            errThread = new Thread(() -> {
                try {
                    analysisStatisticsLog(new BufferedInputStream(process.getErrorStream()));
                } catch (IOException e) {
                    JobLogger.log(e);
                }
            });
            logStatistics = futureTask.get();
            errThread.start();
            // process-wait
            // exit code: 0=success, 1=error
            exitValue = process.waitFor();
            // log-thread join
            errThread.join();
        } catch (Exception e) {
            JobLogger.log(e);
        } finally {
            // Unlock structured data state
            dataMappingLockService.releaseLock(trigger.getDataMappingId(), dataMapping.getSpaceId(), true);
            if (errThread != null && errThread.isAlive()) {
                errThread.interrupt();
            }
            // Delete temporary files
            // if (FileUtil.exist(tmpFilePath)) {
            // FileUtil.del(new File(tmpFilePath));
            // }
        }
        if (exitValue == 0) {
            return new ReturnT<>(200, logStatistics.toString());
        } else {
            return new ReturnT<>(IJobHandler.FAIL.getCode(), "command exit value(" + exitValue + ") is failed");
        }
    }

    /**
     * Replace JSON variable
     */
    public static String replaceVariable(final String param, Map<String, String> variableMap) {
        if (variableMap == null || variableMap.size() < 1) {
            return param;
        }
        Map<String, String> mapping = new HashMap<String, String>();
        Matcher matcher = VARIABLE_PATTERN.matcher(param);
        while (matcher.find()) {
            String variable = matcher.group(2);
            String value = variableMap.get(variable);
            if (StringUtils.isBlank(value)) {
                value = matcher.group();
            }
            mapping.put(matcher.group(), value);
        }
        String retString = param;
        for (final String key : mapping.keySet()) {
            retString = retString.replace(key, mapping.get(key));
        }
        return retString;
    }

    private String generateTemJsonFile(String jobJson) {
        String tmpFilePath;
        String dataXHomePath = SystemUtils.getDataXHomePath();
        if (StringUtils.isNotEmpty(dataXHomePath)) {
            jsonPath = dataXHomePath + DEFAULT_JSON;
        }
        if (!FileUtil.exist(jsonPath)) {
            FileUtil.mkdir(jsonPath);
        }
        tmpFilePath = jsonPath + File.separator + "jobTmp-" + IdUtil.simpleUUID() + ".conf";
        // Write to a temporary local file based on JSON
        try (PrintWriter writer = new PrintWriter(tmpFilePath, "UTF-8")) {
            writer.println(jobJson);
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            JobLogger.log("JSON 临时文件写入异常：" + e.getMessage());
        }
        return tmpFilePath;
    }
}
