package cn.cnic.dataspace.api.queue.space;

import cn.cnic.dataspace.api.cacheLoading.CacheLoading;
import cn.cnic.dataspace.api.config.space.FileMappingManage;
import cn.cnic.dataspace.api.config.space.FileOperationFactory;
import cn.cnic.dataspace.api.exception.CommonException;
import cn.cnic.dataspace.api.model.network.NetworkConf;
import cn.cnic.dataspace.api.model.space.SpaceSvnLog;
import cn.cnic.dataspace.api.model.space.child.Operator;
import cn.cnic.dataspace.api.model.harvest.MiningTask;
import cn.cnic.dataspace.api.model.harvest.TaskFileImp;
import cn.cnic.dataspace.api.model.network.*;
import cn.cnic.dataspace.api.model.user.ConsumerDO;
import cn.cnic.dataspace.api.queue.FTPUtils;
import cn.cnic.dataspace.api.queue.SpaceQuery;
import cn.cnic.dataspace.api.util.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static cn.cnic.dataspace.api.model.space.SpaceSvnLog.ACTION_VALUE;
import static cn.cnic.dataspace.api.util.CommonUtils.*;
import static cn.cnic.dataspace.api.util.CommonUtils.messageInternational;

@Slf4j
public class SpaceTreadPool {

    private static SpaceTreadPool spaceTreadPool = null;

    private final Cache<String, Object> spaceStatistic = CaffeineUtil.getSpaceStatistic();

    private ExecutorService exeCuter;

    private MongoTemplate mongoTemplate;

    private final static int MAX = 20;

    private SpaceTreadPool(MongoTemplate mongoTemplate) {
        exeCuter = Executors.newFixedThreadPool(MAX);
        this.mongoTemplate = mongoTemplate;
    }

    public synchronized static SpaceTreadPool getInstance(MongoTemplate mon) {
        if (spaceTreadPool == null) {
            spaceTreadPool = new SpaceTreadPool(mon);
        }
        return spaceTreadPool;
    }

    private FileMappingManage fileMappingManage() {
        return FileOperationFactory.getFileMappingManage();
    }

    public void execute(final String userId) {
        exeCuter.execute(new Runnable() {

            @SneakyThrows
            @Override
            public void run() {
                SpaceQuery.SpaceTask spaceTask = SpaceQuery.getInstance().getNextElement(userId);
                while (spaceTask != null) {
                    // Core processes
                    MiningTask miningTask = spaceTask.getMiningTask();
                    SpaceTaskUtils spaceTaskUtils = spaceTask.getSpaceTaskUtils();
                    switch(miningTask.getType()) {
                        case Constants.TaskType.SHARE_IMP:
                            try {
                                ftpDownTask(miningTask, spaceTaskUtils);
                            } catch (Exception e) {
                                log.info("分享链接导入失败 -- {} ：" + e.getMessage());
                            }
                            break;
                        case Constants.TaskType.SYS_IMP:
                            try {
                                sysSpaceImport(miningTask, spaceTaskUtils);
                            } catch (Exception e) {
                                log.info("空间导入导入失败 -- {} ： " + e.getMessage());
                            }
                            break;
                        case Constants.TaskType.NET_IMP:
                            try {
                                networkImp(miningTask, spaceTaskUtils);
                            } catch (Exception e) {
                                log.info("网盘导入导入失败 -- {} ：" + e.getMessage());
                            }
                            break;
                    }
                    spaceTask = SpaceQuery.getInstance().getNextElement(userId);
                }
            }
        });
    }

    // public void shutdown(){
    // try {
    // //Question answered
    // exeCuter.shutdown();
    // //(When all tasks have ended, return TRUE)
    // if(!exeCuter.awaitTermination(awaitTime, TimeUnit.MILLISECONDS)){
    // //When timeout occurs, an interrupt is issued to all threads in the thread pool.
    // List<Runnable> runnables = exeCuter.shutdownNow();
    // }
    // exeCuter = Executors.newFixedThreadPool(MAX);;
    // } catch (InterruptedException e) {
    // //When the awaitTermination method is interrupted, it also terminates the execution of all threads in the thread pool.
    // exeCuter.shutdownNow();
    // exeCuter = Executors.newFixedThreadPool(MAX);;
    // }
    // }
    /**
     * FTP Download
     */
    private void ftpDownTask(MiningTask miningTask, SpaceTaskUtils spaceTaskUtils) {
        String userId = miningTask.getUserId();
        String spaceId = miningTask.getSpaceId();
        String ftpHost = miningTask.getFtpHost();
        String username = miningTask.getFtpUserName();
        String password = miningTask.getFtpPassword();
        String targetPath = miningTask.getTargetRootPath();
        List<String> rootPathList = miningTask.getSourcePaths();
        log.info("userId: " + userId + " taskName" + miningTask.getSpaceName() + " 任务处理开始 ------");
        FTPClient ftpClient = null;
        spaceTaskUtils.setUser(miningTask.getEmail());
        spaceTaskUtils.setRootId(miningTask.getTaskId());
        boolean type = true;
        long errorCount = 0L;
        // Determine whether to update or add
        if (!spaceTaskUtils.getPathSize()) {
            type = false;
            errorCount = mongoTemplate.count(new Query().addCriteria(Criteria.where("rootId").is(miningTask.getTaskId()).and("state").is(3)), TaskFileImp.class);
        }
        try {
            ftpClient = spaceTaskUtils.login(ftpHost, miningTask.getFtpPort(), username, password);
        } catch (IOException e) {
            // Failed to connect to FTP service
            e.printStackTrace();
            if (type) {
                mainTaskError(miningTask, spaceTaskUtils, "连接ftp时错误: {} " + e.getMessage());
            } else {
                updateLeveState(miningTask, spaceTaskUtils, "连接ftp时错误: {} " + e.getMessage());
            }
            return;
        }
        try {
            if (type) {
                spaceTaskUtils.getFilePathList(ftpClient, rootPathList);
            }
        } catch (IOException e) {
            // Failed to obtain task file list
            e.printStackTrace();
            mainTaskError(miningTask, spaceTaskUtils, "get fileTaskList error: {} " + e.getMessage());
            return;
        }
        // handle
        taskHeadDeal(miningTask, spaceTaskUtils, type);
        List<TaskFileImp> taskFileImpList = spaceTaskUtils.getTaskFileImpList();
        List<TaskFileImp> errorFileImpList = spaceTaskUtils.getErrorFileImpList();
        boolean error = true;
        if (!taskFileImpList.isEmpty()) {
            // Process queue tasks
            socketMessage(Constants.SocketType.TS_START, miningTask.getTaskId(), miningTask.getEmail(), spaceTaskUtils);
            boolean result = doDownloadFile(ftpClient, spaceTaskUtils, taskFileImpList, targetPath, miningTask.getEmail(), spaceId);
            if (result && errorFileImpList.isEmpty() && errorCount == 0) {
                // All successful
                error = false;
            }
        }
        // Close Connection
        try {
            ftpClient.logout();
            if (ftpClient.isConnected()) {
                ftpClient.disconnect();
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        long totalData = spaceTaskUtils.getTotalData();
        if (totalData > 0) {
            FileOperationFactory.getSpaceStatisticConfig().dataFlow("fairLink", totalData, miningTask.getSpaceId(), false);
        }
        taskTailDeal(miningTask, spaceTaskUtils, error);
        log.info("userId: " + userId + " taskName" + miningTask.getSpaceName() + " 任务处理结束 ------");
        if (type) {
            String content = "分享链接导入：通过分享链接导入" + miningTask.getDesc() + " 等 " + miningTask.getFileCount() + " 个文件到 主目录";
            spaceLogSave(spaceId, content, userId);
        }
        return;
    }

    private void mainTaskError(MiningTask miningTask, SpaceTaskUtils spaceTaskUtils, String error) {
        miningTask.setState(3);
        miningTask.setError(error);
        mongoTemplate.save(miningTask);
        socketMessage(Constants.SocketType.TS_ERROR, miningTask.getTaskId(), miningTask.getEmail(), spaceTaskUtils);
    }

    private boolean doDownloadFile(FTPClient ftpClient, SpaceTaskUtils spaceTaskUtils, List<TaskFileImp> taskFileImpList, String downPath, String email, String spaceId) {
        boolean result = true;
        if (!taskFileImpList.isEmpty()) {
            Iterator<TaskFileImp> iterator = taskFileImpList.iterator();
            while (iterator.hasNext()) {
                TaskFileImp fileImp = iterator.next();
                String rootId = fileImp.getRootId();
                String taskId = fileImp.getTaskId();
                try {
                    // sleep
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (SpaceSizeControl.validation(spaceId)) {
                    updateDownState(3, fileImp.getTaskId(), "空间储存已满，请申请扩容后操作!");
                    sendSocketMessage(rootId, email, taskId, spaceTaskUtils);
                    result = false;
                    continue;
                }
                String path = fileImp.getPath();
                String substring = path.substring(1);
                String curePath = "/";
                if (substring.contains("/")) {
                    curePath = substring.substring(substring.indexOf("/"));
                }
                spaceTaskUtils.makeDirs(downPath + curePath);
                int type = fileImp.getType();
                File localFile = new File(downPath + curePath + fileImp.getFileName());
                if (localFile.exists()) {
                    updateDownState(3, fileImp.getTaskId(), type == 1 ? "文件夹已存在!" : "文件已存在!");
                    sendSocketMessage(rootId, email, taskId, spaceTaskUtils);
                    result = false;
                    continue;
                }
                updateDownState(1, fileImp.getTaskId());
                boolean judge;
                if (type == 1) {
                    // Empty file
                    judge = localFile.mkdirs();
                } else {
                    try {
                        // Switch working directory
                        ftpClient.changeWorkingDirectory(new String(path.getBytes(), FTPUtils.CHARSET));
                        // Download a single file
                        judge = spaceTaskUtils.downloadFile(ftpClient, fileImp.getFileName(), downPath + curePath, fileImp.getSize(), fileImp.getTaskId());
                    } catch (Exception ioException) {
                        updateDownState(3, taskId, "文件导入失败! {} " + ioException.getMessage());
                        sendSocketMessage(rootId, email, taskId, spaceTaskUtils);
                        result = false;
                        continue;
                    }
                }
                long progress = spaceTaskUtils.getProgress();
                if (judge) {
                    // Download successful
                    // File Mapping Database
                    fileMapping(downPath, curePath, fileImp.getFileName(), spaceId, email);
                    updateDownState(2, fileImp.getTaskId());
                } else {
                    // Download failed
                    double showMain = fileImp.getSize() > 0 ? ((new BigDecimal((float) progress / fileImp.getSize()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()) * 100) : 0.0;
                    updateDownState(3, fileImp.getTaskId(), progress, showMain);
                    result = false;
                }
                // Change spatial statistics size
                SpaceSizeControl.updateActual(spaceId, progress);
                spaceTaskUtils.setProgress(0L);
                // Main Task Message
                sendSocketMessage(rootId, email, taskId, spaceTaskUtils);
            }
        }
        return result;
    }

    /**
     * File database mapping processing
     */
    private void fileMapping(String downPath, String parentPath, String fileName, String spaceId, String email) {
        String[] split = parentPath.split("\\/");
        List<String> pathList = new ArrayList<>();
        for (String path : split) {
            if (StringUtils.isNotEmpty(path)) {
                downPath = downPath + "/" + path;
                pathList.add(downPath);
            }
        }
        pathList.add(downPath + "/" + fileName);
        fileMappingManage().mappingFileOrFolder(pathList, spaceId, email);
    }

    private void updateDownState(int state, String taskId) {
        Query query = new Query().addCriteria(Criteria.where("taskId").is(taskId));
        Update update = new Update();
        update.set("state", state);
        mongoTemplate.upsert(query, update, TaskFileImp.class);
    }

    private void updateDownState(int state, String taskId, long schedule, double show) {
        Query query = new Query().addCriteria(Criteria.where("taskId").is(taskId));
        Update update = new Update();
        update.set("state", state);
        update.set("schedule", schedule);
        update.set("showSchedule", show);
        mongoTemplate.upsert(query, update, TaskFileImp.class);
    }

    private void updateDownState(int state, String taskId, String error) {
        Query query = new Query().addCriteria(Criteria.where("taskId").is(taskId));
        Update update = new Update();
        update.set("state", state);
        update.set("error", error);
        mongoTemplate.upsert(query, update, TaskFileImp.class);
    }

    private void updateLeveState(MiningTask miningTask, SpaceTaskUtils spaceTaskUtils, String error) {
        miningTask.setState(4);
        miningTask.setError(error);
        mongoTemplate.save(miningTask);
        Query query = new Query().addCriteria(Criteria.where("rootId").is(miningTask.getTaskId()));
        query.addCriteria(new Criteria().orOperator(Criteria.where("state").is(0), Criteria.where("state").is(1)));
        Update update = new Update();
        update.set("state", 3);
        mongoTemplate.upsert(query, update, TaskFileImp.class);
        socketMessage(Constants.SocketType.TS_ERROR, miningTask.getTaskId(), miningTask.getEmail(), spaceTaskUtils);
    }

    private void updateDownState(int state, String spaceId, String userId, String taskId, double schedule) {
        Query query = new Query().addCriteria(Criteria.where("taskId").is(taskId));
        query.addCriteria(Criteria.where("spaceId").is(spaceId));
        query.addCriteria(Criteria.where("userId").is(userId));
        Update update = new Update();
        update.set("state", state);
        update.set("schedule", schedule);
        mongoTemplate.upsert(query, update, MiningTask.class);
    }

    private void socketMessage(String type, String taskId, String email, SpaceTaskUtils spaceTaskUtils) {
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("type", type);
        messageMap.put("taskId", taskId);
        messageMap.put("mark", "space");
        try {
            spaceTaskUtils.sendMessage(email, JSONObject.toJSONString(messageMap));
        } catch (Exception e) {
            log.error("空间汇交与收割任务消息通知发送失败: {} " + e.getMessage());
        }
    }

    private void taskCount(String userId, String spaceId, int count) {
        Object taskCount = spaceStatistic.getIfPresent("task:" + userId + spaceId);
        if (null != taskCount) {
            spaceStatistic.put("task:" + userId + spaceId, (long) taskCount + count);
        }
    }

    /**
     * Operation logging
     */
    private void spaceLogSave(String spaceId, String content, String userId) {
        ConsumerDO id = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(userId)), ConsumerDO.class);
        mongoTemplate.insert(SpaceSvnLog.builder().spaceSvnId(generateSnowflake()).spaceId(spaceId).description(content).operatorId(userId).operator(new Operator(id)).action(SpaceSvnLog.ACTION_FILE).createTime(new Date()).version(ACTION_VALUE).build());
    }

    /**
     * Space Import
     */
    private void sysSpaceImport(MiningTask miningTask, SpaceTaskUtils spaceTaskUtils) {
        String userId = miningTask.getUserId();
        String spaceId = miningTask.getSpaceId();
        String sourceRootPath = miningTask.getSourceRootPath();
        String sourceId = sourceRootPath.substring(sourceRootPath.lastIndexOf("/") + 1);
        String targetRootPath = miningTask.getTargetRootPath();
        List<String> sourcePaths = miningTask.getSourcePaths();
        log.info("userId: " + userId + " taskName" + miningTask.getSpaceName() + " 任务处理开始 ------");
        boolean type = true;
        long errorCount = 0L;
        if (!spaceTaskUtils.getPathSize()) {
            // Determine whether to update or add
            type = false;
            errorCount = mongoTemplate.count(new Query().addCriteria(Criteria.where("rootId").is(miningTask.getTaskId()).and("state").is(3)), TaskFileImp.class);
        }
        try {
            if (type) {
                // Resolve file path
                spaceTaskUtils.loadingTaskList(sourcePaths, sourceId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            mainTaskError(miningTask, spaceTaskUtils, "解析文件列表时失败: {} " + e.getMessage());
            return;
        }
        // Processing task information
        taskHeadDeal(miningTask, spaceTaskUtils, type);
        List<TaskFileImp> taskFileImpList = spaceTaskUtils.getTaskFileImpList();
        List<TaskFileImp> errorFileImpList = spaceTaskUtils.getErrorFileImpList();
        boolean error = true;
        if (!taskFileImpList.isEmpty()) {
            // Process queue tasks
            socketMessage(Constants.SocketType.TS_START, miningTask.getTaskId(), miningTask.getEmail(), spaceTaskUtils);
            try {
                boolean result = fileCopy(taskFileImpList, spaceTaskUtils, sourceRootPath, targetRootPath, miningTask.getEmail(), spaceId);
                if (result && errorFileImpList.isEmpty() && errorCount == 0) {
                    // All successful
                    error = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        long totalData = spaceTaskUtils.getTotalData();
        if (totalData > 0) {
            FileOperationFactory.getSpaceStatisticConfig().dataFlow("web", totalData, miningTask.getSpaceId(), false);
        }
        taskTailDeal(miningTask, spaceTaskUtils, error);
        log.info("userId: " + userId + " taskName" + miningTask.getSpaceName() + " 任务处理结束 ------");
        if (type) {
            // String replace = targetRootPath.replaceAll(spaceId, "~");
            // String substring = replace.substring(replace.indexOf("~") + 1);
            String content = miningTask.getSpaceName() + miningTask.getDesc() + " 等 " + miningTask.getFileCount() + " 个文件到空间根目录";
            spaceLogSave(spaceId, content, userId);
        }
        return;
    }

    private boolean fileCopy(List<TaskFileImp> taskFileImpList, SpaceTaskUtils spaceTaskUtils, String source, String target, String email, String spaceId) {
        boolean result = true;
        Iterator<TaskFileImp> iterator = taskFileImpList.iterator();
        while (iterator.hasNext()) {
            try {
                // sleep
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            TaskFileImp taskFileImp = iterator.next();
            String path = taskFileImp.getPath();
            String rootId = taskFileImp.getRootId();
            String fileName = taskFileImp.getFileName();
            String taskId = taskFileImp.getTaskId();
            if (SpaceSizeControl.validation(spaceId)) {
                updateDownState(3, taskId, "空间储存已满，请申请扩容后操作!");
                sendSocketMessage(rootId, email, taskId, spaceTaskUtils);
                result = false;
                continue;
            }
            File sourceFile = new File(source + path + "/" + fileName);
            if (!sourceFile.exists()) {
                // Verify if the source file exists
                updateDownState(3, taskId, "文件在原空间未找到!");
                sendSocketMessage(rootId, email, taskId, spaceTaskUtils);
                result = false;
                continue;
            }
            File localFile = new File(target + path);
            if (!localFile.exists()) {
                localFile.mkdirs();
            }
            // Final Catalog
            File endFile = new File(target + path + "/" + fileName);
            if (endFile.exists()) {
                updateDownState(3, taskId, taskFileImp.getType() == 1 ? "文件夹已存在!" : "文件已存在!");
                sendSocketMessage(rootId, email, taskId, spaceTaskUtils);
                result = false;
                continue;
            }
            updateDownState(1, taskId);
            spaceTaskUtils.setProgress(0L);
            boolean judge;
            if (taskFileImp.getType() == 1) {
                // Empty folder
                judge = endFile.mkdirs();
            } else {
                try {
                    // Copying a Single File
                    judge = spaceTaskUtils.copyFile(sourceFile, endFile, taskFileImp.getSize(), taskId);
                } catch (CommonException commonException) {
                    updateDownState(3, taskId, "文件导入失败! {} " + commonException.getMessage());
                    sendSocketMessage(rootId, email, taskId, spaceTaskUtils);
                    result = false;
                    continue;
                }
            }
            // File Progress
            long progress = spaceTaskUtils.getProgress();
            if (judge) {
                // File Mapping Database
                fileMapping(target, path, fileName, spaceId, email);
                updateDownState(2, taskId);
            } else {
                // Download failed
                double showMain = taskFileImp.getSize() > 0 ? ((new BigDecimal((float) progress / taskFileImp.getSize()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()) * 100) : 0.0;
                updateDownState(3, taskId, progress, showMain);
                result = false;
            }
            SpaceSizeControl.updateActual(spaceId, progress);
            sendSocketMessage(rootId, email, taskId, spaceTaskUtils);
        }
        return result;
    }

    /**
     * Single file task message push
     */
    private void sendSocketMessage(String rootId, String email, String taskId, SpaceTaskUtils taskUtils) {
        // Main Task Message
        sendTask(rootId, email, taskUtils);
        socketMessage(Constants.SocketType.TS_LEVEL_END, taskId, email, taskUtils);
    }

    /**
     * Network disk import
     */
    private void networkImp(MiningTask miningTask, SpaceTaskUtils spaceTaskUtils) {
        String userId = miningTask.getUserId();
        String spaceId = miningTask.getSpaceId();
        String targetRootPath = miningTask.getTargetRootPath();
        List<Long> sourcePaths = new ArrayList<Long>(miningTask.getSourcePaths().size());
        for (String sourcePath : miningTask.getSourcePaths()) {
            sourcePaths.add(Long.valueOf(sourcePath));
        }
        // Get token
        String uk = miningTask.getParam();
        log.info("userId: " + userId + " taskName" + miningTask.getSpaceName() + " 任务(网盘)处理开始 ------");
        boolean type = true;
        long errorCount = 0L;
        // Determine whether to update or add
        if (!spaceTaskUtils.getPathSize()) {
            type = false;
            errorCount = mongoTemplate.count(new Query().addCriteria(Criteria.where("rootId").is(miningTask.getTaskId()).and("state").is(3)), TaskFileImp.class);
        }
        if (type) {
            // total
            List<FileDeData.FileDe> fileDeList = new ArrayList<>(sourcePaths.size());
            List<String> dirPath = new ArrayList<>();
            List<FileDeData.FileDe> netFile;
            try {
                netFile = getNetFile(uk, sourcePaths);
            } catch (Exception e) {
                e.printStackTrace();
                mainTaskError(miningTask, spaceTaskUtils, "获取网盘文件列表时失败: {} " + e.getMessage());
                return;
            }
            for (FileDeData.FileDe fileDe : netFile) {
                if (fileDe.getIsdir() == 1) {
                    dirPath.add(fileDe.getPath());
                } else {
                    fileDeList.add(fileDe);
                }
            }
            // Get files under the folder
            List<Long> fileIds = new ArrayList<>(16);
            // Empty folder record
            List<String> nullDirList = new ArrayList<>(16);
            try {
                getDirFileLink(dirPath, fileIds, uk, nullDirList);
                List<FileDeData.FileDe> dirFile = getNetFile(uk, fileIds);
                fileDeList.addAll(dirFile);
            } catch (Exception e) {
                e.printStackTrace();
                mainTaskError(miningTask, spaceTaskUtils, "获取网盘文件列表时失败: {} " + e.getMessage());
                return;
            }
            try {
                spaceTaskUtils.netFileTaskCov(fileDeList, nullDirList);
            } catch (Exception e) {
                e.printStackTrace();
                mainTaskError(miningTask, spaceTaskUtils, "解析文件列表时失败: {} " + e.getMessage());
                return;
            }
        }
        // Save Queue
        taskHeadDeal(miningTask, spaceTaskUtils, type);
        List<TaskFileImp> taskFileImpList = spaceTaskUtils.getTaskFileImpList();
        List<TaskFileImp> errorFileImpList = spaceTaskUtils.getErrorFileImpList();
        boolean error = true;
        if (!taskFileImpList.isEmpty()) {
            // Process queue tasks
            socketMessage(Constants.SocketType.TS_START, miningTask.getTaskId(), miningTask.getEmail(), spaceTaskUtils);
            // Verify token
            if (!type) {
                try {
                    verify(uk);
                } catch (Exception e) {
                    e.printStackTrace();
                    mainTaskError(miningTask, spaceTaskUtils, e.getMessage());
                }
            }
            try {
                boolean result = networkFileDownTask(taskFileImpList, spaceTaskUtils, targetRootPath, miningTask.getEmail(), uk, spaceId);
                if (result && errorFileImpList.isEmpty() && errorCount == 0) {
                    // All successful
                    error = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        long totalData = spaceTaskUtils.getTotalData();
        if (totalData > 0) {
            FileOperationFactory.getSpaceStatisticConfig().dataFlow("web", totalData, miningTask.getSpaceId(), false);
        }
        taskTailDeal(miningTask, spaceTaskUtils, error);
        log.info("userId: " + userId + " taskName" + miningTask.getSpaceName() + " 任务处理结束 ------");
        if (type) {
            // String replace = targetRootPath.replaceAll(spaceId, "~");
            // String substring = replace.substring(replace.indexOf("~") + 1);
            // String content=miningTask. getSpaceName()+miningTask. getDesc()+"etc."+miningTask. getFileCount()+"files to directory:"+substring;
            String content = miningTask.getSpaceName() + miningTask.getDesc() + " 等 " + miningTask.getFileCount() + " 个文件到空间";
            spaceLogSave(spaceId, content, userId);
        }
        return;
    }

    /**
     * Network disk download
     */
    private boolean networkFileDownTask(List<TaskFileImp> taskFileImpList, SpaceTaskUtils spaceTaskUtils, String target, String email, String uk, String spaceId) {
        boolean result = true;
        Iterator<TaskFileImp> iterator = taskFileImpList.iterator();
        while (iterator.hasNext()) {
            TaskFileImp taskFileImp = iterator.next();
            String path = taskFileImp.getPath();
            String fileName = taskFileImp.getFileName();
            String rootId = taskFileImp.getRootId();
            String taskId = taskFileImp.getTaskId();
            String link = taskFileImp.getLink() + getNet_token(uk);
            File localFile = new File(target + path);
            if (!localFile.exists()) {
                localFile.mkdirs();
            }
            if (SpaceSizeControl.validation(spaceId)) {
                updateDownState(3, taskId, "空间储存已满，请申请扩容后操作!");
                sendSocketMessage(rootId, email, taskId, spaceTaskUtils);
                result = false;
                continue;
            }
            // Final Catalog
            File endFile = new File(target + path + "/" + fileName);
            if (endFile.exists()) {
                updateDownState(3, taskId, taskFileImp.getType() == 1 ? "文件夹已存在!" : "文件已存在!");
                sendSocketMessage(rootId, email, taskId, spaceTaskUtils);
                result = false;
                continue;
            }
            updateDownState(1, taskId);
            boolean judge;
            if (taskFileImp.getType() == 1) {
                judge = endFile.mkdirs();
            } else {
                try {
                    // Download a single file
                    judge = spaceTaskUtils.netDownloadFile(link, endFile, taskFileImp.getSize(), taskId);
                } catch (Exception e) {
                    updateDownState(3, taskId, "文件下载失败! {} " + e.getMessage());
                    sendSocketMessage(rootId, email, taskId, spaceTaskUtils);
                    result = false;
                    continue;
                }
            }
            spaceTaskUtils.setProgress(0L);
            // File Progress
            long progress = spaceTaskUtils.getProgress();
            if (judge) {
                // Download successful
                // File Mapping Database
                fileMapping(target, path, fileName, spaceId, email);
                updateDownState(2, taskId);
            } else {
                // Download failed
                double showMain = taskFileImp.getSize() > 0 ? ((new BigDecimal((float) progress / taskFileImp.getSize()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()) * 100) : 0.0;
                updateDownState(3, taskId, progress, showMain);
                result = false;
            }
            SpaceSizeControl.updateActual(spaceId, progress);
            // Main Task Message
            sendSocketMessage(rootId, email, taskId, spaceTaskUtils);
        }
        return result;
    }

    /**
     * Obtain details of downloading files from the network disk
     */
    private List<FileDeData.FileDe> getNetFile(String uk, List<Long> sourcePaths) {
        String data = "";
        HttpClient httpClient = new HttpClient();
        try {
            data = httpClient.doGetWayTwo(NetworkUrl.getFileDetailUrl(getNet_token(uk), JSON.toJSONString(sourcePaths)));
        } catch (Exception e) {
            e.printStackTrace();
            throw new CommonException(-1, messageInternational("FILE_CONNECT"));
        }
        if (StringUtils.isEmpty(data)) {
            throw new CommonException(-1, messageInternational("FILE_DISK_MSG"));
        }
        FileDeData fileDeData = JSONObject.parseObject(data, FileDeData.class);
        if (fileDeData == null) {
            throw new CommonException(-1, messageInternational("FILE_PARSE_ERROR"));
        }
        if (fileDeData.getErrno() == 111 || fileDeData.getErrno() == 110) {
            // Token expiration is illegal//Refreshing token
            try {
                refToken(uk);
            } catch (Exception e) {
                e.printStackTrace();
                throw new CommonException(-1, "刷新token访问失败：{}" + e.getMessage());
            }
            return getNetFile(uk, sourcePaths);
        } else if (fileDeData.getErrno() != 0) {
            throw new CommonException(-1, messageInternational("FILE_USER_ERROR") + fileDeData.getErrmsg());
        }
        return fileDeData.getList();
    }

    private void getDirFileLink(List<String> pathList, List<Long> fileIdList, String uk, List<String> nullDirList) {
        for (String p : pathList) {
            List<BaiduData.File> fileList = getDirFile(uk, p);
            List<String> dirPathList = new ArrayList<>(8);
            if (fileList.isEmpty()) {
                nullDirList.add(p);
                continue;
            }
            for (BaiduData.File file : fileList) {
                long fs_id = file.getFs_id();
                if (file.getIsdir() == 1) {
                    dirPathList.add(file.getPath());
                } else {
                    fileIdList.add(fs_id);
                }
            }
            if (!dirPathList.isEmpty()) {
                getDirFileLink(dirPathList, fileIdList, uk, nullDirList);
            }
        }
    }

    private List<BaiduData.File> getDirFile(String uk, String path) {
        HttpClient httpClient = new HttpClient();
        String data = "";
        try {
            data = httpClient.doGetWayTwo(NetworkUrl.getFileUrl(getNet_token(uk), path));
        } catch (Exception e) {
            e.printStackTrace();
            throw new CommonException(messageInternational("FILE_CONNECT"));
        }
        if (StringUtils.isEmpty(data)) {
            throw new CommonException(messageInternational("FILE_DISK_MSG"));
        }
        BaiduData baiduData = JSONObject.parseObject(data, BaiduData.class);
        if (baiduData == null) {
            throw new CommonException(messageInternational("FILE_PARSE_ERROR"));
        }
        if (baiduData.getErrno() == 111 || baiduData.getErrno() == 110) {
            // Token expiration is illegal//Refreshing token
            try {
                refToken(uk);
            } catch (Exception e) {
                e.printStackTrace();
                throw new CommonException(-1, "刷新token访问失败：{}" + e.getMessage());
            }
            return getDirFile(uk, path);
        } else if (baiduData.getErrno() != 0) {
            throw new CommonException(messageInternational("FILE_USER_ERROR") + baiduData.getGuid_info());
        }
        return baiduData.getList();
    }

    private String getNet_token(String uk) {
        String token = (String) spaceStatistic.getIfPresent(uk);
        if (null == token) {
            NetworkUser networkUser = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(uk)), NetworkUser.class);
            spaceStatistic.put(uk, networkUser.getAcc_token());
            token = networkUser.getAcc_token();
        }
        return token;
    }

    private void refToken(String uk) throws MalformedURLException, URISyntaxException {
        CacheLoading cacheLoading = new CacheLoading(mongoTemplate);
        Object net = cacheLoading.loadingNet();
        if (net == null) {
            // Not added to the network disk configuration
            throw new CommonException("系统已关闭系统百度网盘使用，请联系管理员。");
        }
        NetworkUser networkUser = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(uk)), NetworkUser.class);
        String refToken = networkUser.getRef_token();
        NetworkConf networkConf = (NetworkConf) net;
        String tokenUrl = NetworkUrl.getRefreshTokenUrl(refToken, networkConf.getAppKey(), networkConf.getSecretKey());
        HttpClient httpClient = new HttpClient();
        String data = httpClient.doGetWayTwo(tokenUrl);
        Auth auth = JSONObject.parseObject(data, Auth.class);
        String access_token = auth.getAccess_token();
        String refresh_token = auth.getRefresh_token();
        if (StringUtils.isEmpty(access_token) || StringUtils.isEmpty(refresh_token)) {
            throw new CommonException("获取不到token: {}" + data);
        }
        // update
        networkUser.setAcc_token(access_token);
        networkUser.setRef_token(refresh_token);
        mongoTemplate.save(networkUser);
        spaceStatistic.put(uk, access_token);
    }

    private void verify(String uk) throws MalformedURLException, URISyntaxException {
        String userUrl = NetworkUrl.getUserUrl(getNet_token(uk));
        HttpClient httpClient = new HttpClient();
        String data = httpClient.doGetWayTwo(userUrl);
        HashMap hashMap = JSONObject.parseObject(data, HashMap.class);
        int errno = (int) hashMap.get("errno");
        if (errno == 0) {
            return;
        } else if (errno == 111 || errno == 110) {
            try {
                refToken(uk);
            } catch (Exception e) {
                e.printStackTrace();
                throw new CommonException(-1, "刷新token访问失败：{}" + e.getMessage());
            }
        } else {
            throw new CommonException(-1, "用于token验证-用户信息验证错误：{}" + data);
        }
    }

    /**
     * Task processing header
     */
    private void taskHeadDeal(MiningTask miningTask, SpaceTaskUtils spaceTaskUtils, boolean judge) {
        if (judge) {
            List<TaskFileImp> taskFileImpList = spaceTaskUtils.getTaskFileImpList();
            List<TaskFileImp> errorFileImpList = spaceTaskUtils.getErrorFileImpList();
            miningTask.setSize(spaceTaskUtils.getTotal());
            miningTask.setFileCount(taskFileImpList.size() + errorFileImpList.size());
            if (!taskFileImpList.isEmpty()) {
                miningTask.setDesc(taskFileImpList.get(0).getFileName());
                miningTask.setState(1);
            } else {
                if (!errorFileImpList.isEmpty()) {
                    miningTask.setState(3);
                    miningTask.setDesc(errorFileImpList.get(0).getFileName());
                }
            }
            // Save Root Task
            mongoTemplate.save(miningTask);
            // Save mongodb task list
            if (!errorFileImpList.isEmpty()) {
                // Save failed file
                mongoTemplate.insertAll(errorFileImpList);
            }
            if (!taskFileImpList.isEmpty()) {
                try {
                    mongoTemplate.insertAll(taskFileImpList);
                } catch (Exception e) {
                    e.printStackTrace();
                    mainTaskError(miningTask, spaceTaskUtils, "save fileTaskList error: {} " + e.getMessage());
                    return;
                }
            }
        }
    }

    /**
     * Task tail processing
     */
    private void taskTailDeal(MiningTask miningTask, SpaceTaskUtils spaceTaskUtils, boolean error) {
        String spaceId = miningTask.getSpaceId();
        String userId = miningTask.getUserId();
        if (spaceTaskUtils.getTaskFileImpList().isEmpty()) {
            socketMessage(Constants.SocketType.TS_END, miningTask.getTaskId(), miningTask.getEmail(), spaceTaskUtils);
        }
        if (error) {
            long totalProgress = spaceTaskUtils.getTotalProgress();
            double showMain = miningTask.getFileCount() > 0 ? ((new BigDecimal((float) totalProgress / miningTask.getFileCount()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()) * 100) : 0.0;
            updateDownState(4, spaceId, userId, miningTask.getTaskId(), showMain);
            socketMessage(Constants.SocketType.TS_ERROR, miningTask.getTaskId(), miningTask.getEmail(), spaceTaskUtils);
        } else {
            updateDownState(2, spaceId, userId, miningTask.getTaskId(), 100);
            taskCount(userId, spaceId, -1);
            socketMessage(Constants.SocketType.TS_END, miningTask.getTaskId(), miningTask.getEmail(), spaceTaskUtils);
        }
        // Destruction
        spaceTaskUtils.destruction();
    }

    /**
     * Task Message
     */
    private void sendTask(String rootId, String email, SpaceTaskUtils spaceTaskUtils) {
        Map<String, Object> messageMap = new HashMap<>(4);
        messageMap.put("type", Constants.SocketType.TS_TASK_MAIN);
        messageMap.put("rootId", rootId);
        messageMap.put("mark", "space");
        long totalProgress = spaceTaskUtils.getTotalProgress();
        long sort = spaceTaskUtils.getSort();
        double showMain = sort > 0 ? ((new BigDecimal((float) totalProgress / sort).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()) * 100) : 100;
        spaceStatistic.put("task-imp" + rootId, showMain);
        messageMap.put("showMain", showMain);
        spaceTaskUtils.sendMessage(email, JSONObject.toJSONString(messageMap));
    }
}
