package cn.cnic.dataspace.api.service.harvest;

import cn.cnic.dataspace.api.config.space.SpaceControlConfig;
import cn.cnic.dataspace.api.exception.CommonException;
import cn.cnic.dataspace.api.model.harvest.FTPShort;
import cn.cnic.dataspace.api.model.harvest.*;
import cn.cnic.dataspace.api.model.harvest.ShareLink;
import cn.cnic.dataspace.api.model.space.Space;
import cn.cnic.dataspace.api.queue.space.SpaceTaskUtils;
import cn.cnic.dataspace.api.queue.SpaceQuery;
import cn.cnic.dataspace.api.service.space.ShareService;
import cn.cnic.dataspace.api.util.*;
import cn.cnic.dataspace.api.websocket.WebSocketProcess;
import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@EnableAsync
public class HarvestService {

    @Autowired
    private ShareService shareService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private SpaceUrl spaceUrl;

    @Autowired
    private JwtTokenUtils jwtTokenUtils;

    @Autowired
    private WebSocketProcess webSocketProcess;

    @Autowired
    private SpaceControlConfig spaceControlConfig;

    private final Cache<String, Object> spaceStatistic = CaffeineUtil.getSpaceStatistic();

    public ResponseResult<Object> getFtpInfo(FilePathRequest filePathRequest) {
        List<String> validation = CommonUtils.validation(filePathRequest);
        if (!validation.isEmpty()) {
            return ResultUtil.error(validation.toString());
        }
        String linkId = filePathRequest.getLinkId();
        String password = filePathRequest.getPassword();
        ShareLink shareLink = shareService.getShareLink(linkId);
        if (!shareLink.getType().equals("space")) {
            return ResultUtil.success();
        }
        shareService.passwordIf(shareLink.getPasWay(), password, shareLink.getPassword());
        Space space = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(shareLink.getSpaceId()).and("state").is("1")), Space.class);
        if (null == space) {
            return ResultUtil.errorInternational("SPACE_OFFLINE_FORBIDDEN");
        }
        // Create User
        String username = "";
        String userPwd = "";
        String ftpUserId = shareLink.getFtpUserId();
        if (StringUtils.isEmpty(ftpUserId)) {
            username = CommonUtils.getCode(6);
            userPwd = CommonUtils.getCode(6);
            FtpUser ftpUser = new FtpUser();
            ftpUser.setUsername(username);
            ftpUser.setPassword(RSAEncrypt.encrypt(userPwd));
            FtpUser insert = mongoTemplate.insert(ftpUser);
            shareLink.setFtpUserId(insert.getId());
            ftpUserId = insert.getId();
            mongoTemplate.save(shareLink);
        } else {
            FtpUser ftpUser = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(ftpUserId)), FtpUser.class);
            username = ftpUser.getUsername();
            userPwd = RSAEncrypt.decrypt(ftpUser.getPassword());
        }
        String ftpHost = spaceUrl.getFtpHost();
        String port = spaceUrl.getShow();
        if (ftpHost.contains(":")) {
            ftpHost = ftpHost.substring(0, ftpHost.indexOf(":"));
        }
        if (!port.equals("21")) {
            ftpHost = ftpHost + ":" + port;
        }
        Query query = new Query().addCriteria(Criteria.where("userId").is(ftpUserId).and("spaceId").is(shareLink.getSpaceId()));
        FTPShort ftpShort = mongoTemplate.findOne(query, FTPShort.class);
        String shortChain = "";
        if (ftpShort != null) {
            shortChain = ftpShort.getShortChain();
        } else {
            shortChain = space.getSpaceShort();
            ftpShort = new FTPShort();
            ftpShort.setUserId(ftpUserId);
            ftpShort.setSpaceId(shareLink.getSpaceId());
            ftpShort.setShortChain(shortChain);
            mongoTemplate.insert(ftpShort);
        }
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("spaceName", space.getSpaceName());
        resultMap.put("host", ftpHost + "/" + shortChain);
        resultMap.put("username", username);
        resultMap.put("password", userPwd);
        List<String> hashList = filePathRequest.getHashList();
        if (null != hashList && !hashList.isEmpty()) {
            List<String> conversion = shareService.conversion(hashList, space.getSpaceShort(), space.getSpaceId());
            resultMap.put("pathList", conversion);
        }
        return ResultUtil.success(resultMap);
    }

    // Change ftp user permissions
    private void updateFtpAuth(String email, String userId, String spaceId, String type) {
        Map<String, String> authoritiesCache = CaffeineUtil.getShortChain(email);
        if (authoritiesCache != null) {
            if (type.equals("add")) {
                authoritiesCache.put(userId, spaceId);
            } else if (type.equals("delete")) {
                Query query = new Query().addCriteria(Criteria.where("userId").is(userId).and("spaceId").is(spaceId));
                FTPShort one = mongoTemplate.findOne(query, FTPShort.class);
                String shortChain = one.getShortChain();
                authoritiesCache.remove(shortChain);
            } else {
                return;
            }
            CaffeineUtil.setShortChain(email, authoritiesCache);
        }
    }

    /**
     * Space Import - Cross System (Asynchronous)
     */
    public ResponseResult<Object> taskSpaceImp(String token, TaskImpRequest taskImpRequest) {
        Token user = jwtTokenUtils.getToken(token);
        List<String> validation = CommonUtils.validation(taskImpRequest);
        if (!validation.isEmpty()) {
            return ResultUtil.error(validation.toString());
        }
        if (!taskImpRequest.getWay().equals("all")) {
            if (null == taskImpRequest.getHashList() || taskImpRequest.getHashList().isEmpty()) {
                return ResultUtil.errorInternational("PARAMETER_ERROR");
            }
        }
        // Permission verification
        spaceControlConfig.validateSpacePermissions(user.getEmailAccounts(), taskImpRequest.getSpaceId(), SpaceRoleEnum.F_OTHER_IM.getRole());
        // Intercept access links
        String linkUrl = taskImpRequest.getLinkUrl();
        String host = getHost(linkUrl);
        Space space = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(taskImpRequest.getSpaceId()).and("state").is("1")), Space.class);
        if (null == space) {
            return ResultUtil.errorInternational("SPACE_REVIEW");
        }
        // Verify space capacity
        if (SpaceSizeControl.validation(space.getSpaceId())) {
            return ResultUtil.errorInternational("FILE_SIZE_FULL");
        }
        // Obtain FTP information based on the link
        HttpClient httpClient = new HttpClient();
        Map paramMap = new HashMap<>(3);
        paramMap.put("linkId", getLinkId(linkUrl));
        paramMap.put("password", taskImpRequest.getPassword());
        paramMap.put("hashList", taskImpRequest.getHashList());
        String body = httpClient.doPostJsonWayTwo(JSONObject.toJSONString(paramMap), host + spaceUrl.getGetFtp());
        JSONObject objBody = null;
        try {
            objBody = JSONObject.parseObject(body);
        } catch (Exception e) {
            log.error("---- 汇交收割 ：{}  调用DataSpace接口 异常 -----");
            return ResultUtil.errorInternational("SYSTEM_ERROR");
        }
        String code = objBody.getString("code");
        if (StringUtils.isNotBlank(code) && "-1".equals(code)) {
            log.error("汇交收割导入接口返回错误 --- {} " + objBody.getString("message"));
            return ResultUtil.error(objBody.getString("message"));
        }
        Map data = (Map) objBody.get("data");
        String spaceName = data.get("spaceName").toString();
        String ftpHost = data.get("host").toString();
        String username = data.get("username").toString();
        String password = data.get("password").toString();
        String rootPath = ftpHost.substring(ftpHost.lastIndexOf("/"));
        // Parse the sourceHash ftp root directory and import it all - obtain all roots, and partially import according to the corresponding roots
        List<String> rootPathList = new ArrayList<>();
        if (taskImpRequest.getWay().equals("all")) {
            rootPathList.add(rootPath);
        } else {
            // Call the interface to obtain the returned path file and handle it separately
            List<String> pathList = (List) data.get("pathList");
            if (null == pathList || pathList.isEmpty()) {
                return ResultUtil.errorInternational("HARVEST_TASK_IMP");
            }
            for (String path : pathList) {
                rootPathList.add(rootPath + path);
            }
        }
        MiningTask miningTask = new MiningTask();
        miningTask.setCreateTime(new Date());
        miningTask.setEmail(user.getEmailAccounts());
        miningTask.setSpaceName("从空间【" + spaceName + "】导入");
        String generateUUID = CommonUtils.generateUUID();
        miningTask.setTaskId(generateUUID);
        miningTask.setSourceRootPath(rootPath);
        miningTask.setSpaceId(taskImpRequest.getSpaceId());
        miningTask.setType(Constants.TaskType.SHARE_IMP);
        miningTask.setUserId(user.getUserId());
        miningTask.setLinkUrl(taskImpRequest.getLinkUrl());
        miningTask.setShowPath("/" + spaceName);
        String ftpUrl = ftpHost.substring(0, ftpHost.lastIndexOf("/"));
        if (ftpUrl.contains(":")) {
            String[] split = ftpUrl.split(":");
            miningTask.setFtpHost(split[0]);
            miningTask.setFtpPort(Integer.valueOf(split[1]));
        } else {
            miningTask.setFtpHost(ftpUrl);
            miningTask.setFtpPort(21);
        }
        miningTask.setFtpUserName(username);
        miningTask.setFtpPassword(password);
        miningTask.setTargetRootPath(space.getFilePath());
        miningTask.setSourcePaths(rootPathList);
        miningTask.setState(5);
        mongoTemplate.save(miningTask);
        socketMessage(Constants.SocketType.TS_DRAW, miningTask.getTaskId(), miningTask.getSpaceName(), miningTask.getEmail());
        Object taskCount = spaceStatistic.getIfPresent("task:" + user.getUserId() + space.getSpaceId());
        if (null != taskCount) {
            spaceStatistic.put("task:" + user.getUserId() + space.getSpaceId(), (long) taskCount + 1);
        }
        // Join the task queue to start processing
        SpaceQuery instance = SpaceQuery.getInstance();
        SpaceTaskUtils spaceTaskUtils = new SpaceTaskUtils(new ArrayList<>(16), webSocketProcess);
        instance.addCache(user.getUserId(), miningTask, mongoTemplate, spaceTaskUtils);
        return ResultUtil.success();
    }

    private void socketMessage(String type, String taskId, String taskName, String email) {
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("type", type);
        messageMap.put("taskId", taskId);
        messageMap.put("taskName", taskName);
        messageMap.put("mark", "space");
        try {
            webSocketProcess.sendMessage(email, JSONObject.toJSONString(messageMap));
        } catch (Exception e) {
            log.error("空间汇交与收割任务消息通知发送失败: {} " + e.getMessage());
        }
    }

    private String getHost(String url) {
        if (!url.contains("http") && !url.contains(":") && !url.contains("//") && !url.contains(".") && url.length() < 30) {
            throw new CommonException(CommonUtils.messageInternational("HARVEST_TASK_URL"));
        }
        String s = url.replaceAll("//", "~");
        int i = s.indexOf("/");
        String host = s.substring(0, i);
        return host.replaceAll("~", "//");
    }

    private String getLinkId(String url) {
        return url.substring(url.indexOf("=") + 1);
    }

    public ResponseResult<Object> taskList(String token, String spaceId, Integer page, Integer size) {
        String userIdFromToken = jwtTokenUtils.getUserIdFromToken(token);
        Query query = new Query().addCriteria(Criteria.where("userId").is(userIdFromToken).and("state").ne(2));
        if (StringUtils.isNotEmpty(spaceId)) {
            // 
            query.addCriteria(Criteria.where("spaceId").is(spaceId));
        }
        long count = mongoTemplate.count(query, MiningTask.class);
        List<Map<String, Object>> miningTasks = null;
        if (count > 0) {
            query.with(PageRequest.of(page - 1, size));
            query.with(Sort.by(Sort.Order.asc("createTime")));
            List<MiningTask> miningTaskList = mongoTemplate.find(query, MiningTask.class);
            miningTasks = new ArrayList<>(miningTaskList.size());
            for (MiningTask miningTask : miningTaskList) {
                String taskId = miningTask.getTaskId();
                Object schedule = spaceStatistic.getIfPresent("task-imp" + taskId);
                if (null != schedule) {
                    miningTask.setSchedule((double) schedule);
                }
                Map<String, Object> map = new HashMap<>(9);
                map.put("id", miningTask.getId());
                map.put("taskId", miningTask.getTaskId());
                map.put("spaceName", miningTask.getSpaceName());
                map.put("state", miningTask.getState());
                map.put("size", miningTask.getSize());
                map.put("fileCount", miningTask.getFileCount());
                map.put("desc", miningTask.getDesc());
                map.put("error", miningTask.getError());
                map.put("schedule", miningTask.getSchedule());
                miningTasks.add(map);
            }
        }
        Map<String, Object> resultMap = new HashMap<>(2);
        resultMap.put("count", count);
        resultMap.put("data", miningTasks);
        return ResultUtil.success(resultMap);
    }

    public ResponseResult<Object> levelTaskList(String token, String taskId, Integer state, Integer page, Integer size) {
        String userIdFromToken = jwtTokenUtils.getUserIdFromToken(token);
        Query query = new Query().addCriteria(Criteria.where("taskId").is(taskId).and("userId").is(userIdFromToken));
        MiningTask one = mongoTemplate.findOne(query, MiningTask.class);
        String showPath = one.getShowPath();
        String sourceRootPath = one.getSourceRootPath();
        long count = 0L;
        List<TaskFileImp> taskFileImpList = null;
        if (null != one) {
            Query taskQuery = new Query().addCriteria(Criteria.where("rootId").is(taskId));
            if (state == 0) {
                Criteria criteria = new Criteria();
                criteria.orOperator(Criteria.where("state").is(0), Criteria.where("state").is(1));
                taskQuery.addCriteria(criteria);
            } else {
                taskQuery.addCriteria(Criteria.where("state").is(state));
            }
            count = mongoTemplate.count(taskQuery, TaskFileImp.class);
            if (count > 0) {
                taskQuery.with(PageRequest.of(page - 1, size));
                taskQuery.with(Sort.by(Sort.Direction.DESC, "state"));
                taskQuery.with(Sort.by(Sort.Direction.ASC, "sort"));
                taskFileImpList = mongoTemplate.find(taskQuery, TaskFileImp.class);
                if (!StringUtils.isEmpty(sourceRootPath)) {
                    if (one.getType().equals(Constants.TaskType.SHARE_IMP)) {
                        for (TaskFileImp taskFileImp : taskFileImpList) {
                            String path = taskFileImp.getPath();
                            taskFileImp.setPath(path.replaceAll(sourceRootPath, showPath));
                        }
                    } else if (one.getType().equals(Constants.TaskType.SYS_IMP)) {
                        for (TaskFileImp taskFileImp : taskFileImpList) {
                            String path = taskFileImp.getPath();
                            taskFileImp.setPath((path.equals("/") ? showPath : showPath + path));
                        }
                    }
                }
            }
        }
        Map<String, Object> resultMap = new HashMap<>(2);
        resultMap.put("count", count);
        resultMap.put("data", taskFileImpList);
        return ResultUtil.success(resultMap);
    }

    /**
     * Main task file failed and retry
     */
    public ResponseResult<Object> mainTaskRetry(String token, String mainTaskId) {
        log.info("调用重试主任务---");
        if (StringUtils.isEmpty(mainTaskId) || StringUtils.isEmpty(mainTaskId.trim())) {
            return ResultUtil.errorInternational("PARAMETER_ERROR");
        }
        String userIdFromToken = jwtTokenUtils.getUserIdFromToken(token);
        Query query = new Query().addCriteria(Criteria.where("userId").is(userIdFromToken).and("taskId").is(mainTaskId));
        MiningTask miningTask = mongoTemplate.findOne(query, MiningTask.class);
        if (null == miningTask) {
            return ResultUtil.errorInternational("PARAMETER_ERROR");
        }
        // Verify space capacity
        if (SpaceSizeControl.validation(miningTask.getSpaceId())) {
            return ResultUtil.errorInternational("FILE_SIZE_FULL");
        }
        int state = miningTask.getState();
        if (state != 3 && state != 4) {
            return ResultUtil.success();
        }
        Query taskQuery = new Query().addCriteria(Criteria.where("rootId").is(mainTaskId).and("state").is(3).and("size").ne(0));
        taskQuery.with(Sort.by(Sort.Direction.ASC, "sort"));
        List<TaskFileImp> taskFileImpList = mongoTemplate.find(taskQuery, TaskFileImp.class);
        if (taskFileImpList.isEmpty()) {
            return ResultUtil.success();
        }
        // Change Status
        Update update = new Update();
        update.set("state", 0);
        mongoTemplate.upsert(new Query().addCriteria(Criteria.where("rootId").is(mainTaskId).and("state").is(3).and("size").ne(0)), update, TaskFileImp.class);
        miningTask.setState(5);
        mongoTemplate.save(miningTask);
        log.info("开始重试主任务---");
        socketMessage(Constants.SocketType.TS_DRAW, miningTask.getTaskId(), miningTask.getSpaceName(), miningTask.getEmail());
        // Join the task queue to start processing
        SpaceQuery instance = SpaceQuery.getInstance();
        SpaceTaskUtils spaceTaskUtils = new SpaceTaskUtils(taskFileImpList, webSocketProcess);
        instance.addCache(userIdFromToken, miningTask, mongoTemplate, spaceTaskUtils);
        return ResultUtil.success();
    }

    /**
     * Subtask file failed retry
     */
    public ResponseResult<Object> subtasksRetry(String token, String mainTaskId, String[] subtaskIds) {
        log.info("调用子任务重试");
        if (StringUtils.isEmpty(mainTaskId) || StringUtils.isEmpty(mainTaskId.trim()) || null == subtaskIds || subtaskIds.length == 0) {
            return ResultUtil.errorInternational("PARAMETER_ERROR");
        }
        String userIdFromToken = jwtTokenUtils.getUserIdFromToken(token);
        Query query = new Query().addCriteria(Criteria.where("userId").is(userIdFromToken).and("taskId").is(mainTaskId));
        MiningTask miningTask = mongoTemplate.findOne(query, MiningTask.class);
        if (null == miningTask) {
            return ResultUtil.errorInternational("PARAMETER_ERROR");
        }
        // Verify space capacity
        if (SpaceSizeControl.validation(miningTask.getSpaceId())) {
            return ResultUtil.errorInternational("FILE_SIZE_FULL");
        }
        int state = miningTask.getState();
        if (state != 3 && state != 4) {
            return ResultUtil.success();
        }
        Query taskQuery = new Query().addCriteria(Criteria.where("rootId").is(mainTaskId).and("state").is(3));
        taskQuery.addCriteria(Criteria.where("taskId").in(subtaskIds));
        taskQuery.with(Sort.by(Sort.Direction.ASC, "sort"));
        List<TaskFileImp> taskFileImpList = mongoTemplate.find(taskQuery, TaskFileImp.class);
        if (taskFileImpList.isEmpty()) {
            return ResultUtil.success();
        }
        // Change Status
        Update update = new Update();
        update.set("state", 0);
        Query updateQuery = new Query().addCriteria(Criteria.where("rootId").is(mainTaskId).and("state").is(3).and("size").ne(0)).addCriteria(Criteria.where("taskId").in(subtaskIds));
        mongoTemplate.upsert(updateQuery, update, TaskFileImp.class);
        miningTask.setState(5);
        mongoTemplate.save(miningTask);
        socketMessage(Constants.SocketType.TS_DRAW, miningTask.getTaskId(), miningTask.getSpaceName(), miningTask.getEmail());
        log.info("开始子任务重试");
        // Join the task queue to start processing
        SpaceQuery instance = SpaceQuery.getInstance();
        SpaceTaskUtils spaceTaskUtils = new SpaceTaskUtils(taskFileImpList, webSocketProcess);
        instance.addCache(userIdFromToken, miningTask, mongoTemplate, spaceTaskUtils);
        return ResultUtil.success();
    }

    public ResponseResult<Object> taskCount(String token, String spaceId) {
        String userIdFromToken = jwtTokenUtils.getUserIdFromToken(token);
        Object taskCount = spaceStatistic.getIfPresent("task:" + userIdFromToken + spaceId);
        if (null == taskCount) {
            Query query = new Query().addCriteria(Criteria.where("userId").is(userIdFromToken));
            // 
            query.addCriteria(Criteria.where("spaceId").is(spaceId).and("state").ne(2));
            // query.addCriteria(new Criteria().orOperator(Criteria.where("state").is(0),Criteria.where("state").is(1),Criteria.where("state").is(5)));
            long count = mongoTemplate.count(query, MiningTask.class);
            spaceStatistic.put("task:" + userIdFromToken + spaceId, count);
            return ResultUtil.success(count);
        }
        return ResultUtil.success(taskCount);
    }

    public ResponseResult<Object> taskDel(String token, String[] taskIds) {
        if (null == taskIds || taskIds.length == 0) {
            return ResultUtil.errorInternational("PARAMETER_ERROR");
        }
        String userIdFromToken = jwtTokenUtils.getUserIdFromToken(token);
        Query query = new Query().addCriteria(Criteria.where("userId").is(userIdFromToken).and("taskId").in(taskIds));
        query.addCriteria(new Criteria().orOperator(Criteria.where("state").is(3), Criteria.where("state").is(4)));
        List<MiningTask> miningTasks = mongoTemplate.find(query, MiningTask.class);
        if (null == miningTasks || miningTasks.isEmpty()) {
            return ResultUtil.success();
        }
        mongoTemplate.remove(query, MiningTask.class);
        // Remove Subtask
        List<String> list = new ArrayList<>(miningTasks.size());
        for (MiningTask miningTask : miningTasks) {
            list.add(miningTask.getTaskId());
        }
        mongoTemplate.remove(new Query().addCriteria(Criteria.where("rootId").in(list)), TaskFileImp.class);
        return ResultUtil.success();
    }

    public ResponseResult<Object> levelDel(String token, String mainTaskId, String[] taskIds) {
        if (null == taskIds || taskIds.length == 0) {
            return ResultUtil.errorInternational("PARAMETER_ERROR");
        }
        String userIdFromToken = jwtTokenUtils.getUserIdFromToken(token);
        Query query = new Query().addCriteria(Criteria.where("userId").is(userIdFromToken).and("taskId").is(mainTaskId));
        query.addCriteria(new Criteria().orOperator(Criteria.where("state").is(3), Criteria.where("state").is(4)));
        MiningTask one = mongoTemplate.findOne(query, MiningTask.class);
        if (one == null) {
            return ResultUtil.success();
        }
        Query fileQuery = new Query().addCriteria(Criteria.where("rootId").is(mainTaskId).and("taskId").in(taskIds));
        mongoTemplate.remove(fileQuery, TaskFileImp.class);
        long fileCount = mongoTemplate.count(new Query().addCriteria(Criteria.where("rootId").is(mainTaskId)), TaskFileImp.class);
        long errorCount = mongoTemplate.count(new Query().addCriteria(Criteria.where("rootId").is(mainTaskId).and("state").is(3)), TaskFileImp.class);
        one.setFileCount(fileCount);
        if (errorCount == 0) {
            one.setSchedule(100);
            one.setState(2);
        } else {
            double showMain = fileCount > 0 ? ((new BigDecimal((float) errorCount / fileCount).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue()) * 100) : 0.0;
            one.setSchedule(showMain);
        }
        mongoTemplate.save(one);
        return ResultUtil.success();
    }

    /**
     * Password verification - httpCline
     */
    public ResponseResult<Object> spaceInfo(String linkId, String host) {
        HttpClient httpClient = new HttpClient();
        String result;
        try {
            host = host + "/api/harvest/isPwd";
            result = httpClient.doGetWayTwo(host + "?linkId=" + linkId);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultUtil.errorInternational("GENERAL_HTTPCLIENT");
        }
        return JSONObject.parseObject(result, ResponseResult.class);
    }

    public ResponseResult<Object> cmd(String linkId, String cmd, String target, String spaceId, String password, String host, Integer page, Integer size, String direction, String sort) {
        HttpClient httpClient = new HttpClient();
        host = host + "/api/harvest/file";
        String url = host + "?linkId=" + linkId + "&cmd=" + cmd + "&target=" + target + "&spaceId=" + spaceId + "&password=" + password + "&page=" + page + "&size=" + size + "&direction=" + direction + "&sort=" + sort;
        String result;
        try {
            result = httpClient.doGetWayTwo(url);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultUtil.errorInternational("GENERAL_HTTPCLIENT");
        }
        Map map = JSONObject.parseObject(result, Map.class);
        if ((int) map.get("code") == 0) {
            return ResultUtil.success(map.get("data"));
        } else {
            return ResultUtil.error(map.get("message").toString());
        }
    }

    public ResponseResult<Object> detail(String linkId, String password, String host) {
        HttpClient httpClient = new HttpClient();
        host = host + "/api/harvest/detail";
        String url = host + "?linkId=" + linkId + "&password=" + password;
        String result;
        try {
            result = httpClient.doGetWayTwo(url);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultUtil.errorInternational("GENERAL_HTTPCLIENT");
        }
        return JSONObject.parseObject(result, ResponseResult.class);
    }

    public ResponseResult<Object> fileList(String linkId, String password, String host) {
        HttpClient httpClient = new HttpClient();
        host = host + "/api/harvest/fileList";
        String url = host + "?linkId=" + linkId + "&password=" + password;
        String result;
        try {
            result = httpClient.doGetWayTwo(url);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultUtil.errorInternational("GENERAL_HTTPCLIENT");
        }
        return JSONObject.parseObject(result, ResponseResult.class);
    }
}
