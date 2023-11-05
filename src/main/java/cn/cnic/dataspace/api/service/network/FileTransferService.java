package cn.cnic.dataspace.api.service.network;

import cn.cnic.dataspace.api.cacheLoading.CacheLoading;
import cn.cnic.dataspace.api.config.space.SpaceControlConfig;
import cn.cnic.dataspace.api.elfinder.core.Target;
import cn.cnic.dataspace.api.elfinder.service.ElfinderStorage;
import cn.cnic.dataspace.api.elfinder.service.ElfinderStorageService;
import cn.cnic.dataspace.api.model.network.NetworkConf;
import cn.cnic.dataspace.api.model.space.Space;
import cn.cnic.dataspace.api.model.harvest.MiningTask;
import cn.cnic.dataspace.api.model.network.*;
import cn.cnic.dataspace.api.queue.SpaceQuery;
import cn.cnic.dataspace.api.queue.space.SpaceTaskUtils;
import cn.cnic.dataspace.api.util.*;
import cn.cnic.dataspace.api.websocket.WebSocketProcess;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static cn.cnic.dataspace.api.util.CommonUtils.messageInternational;

@Service
@Slf4j
@EnableAsync
public class FileTransferService {

    @Autowired
    private JwtTokenUtils jwtTokenUtils;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private SpaceUrl spaceUrl;

    @Autowired
    private ElfinderStorageService elfinderStorageService;

    @Autowired
    private WebSocketProcess webSocketProcess;

    @Autowired
    private SpaceControlConfig spaceControlConfig;

    private final Cache<String, String> network = CaffeineUtil.getThirdParty();

    private final Cache<String, Object> spaceStatistic = CaffeineUtil.getSpaceStatistic();

    public ResponseResult<Object> fileList(HttpServletRequest request, String path) {
        Token user = jwtTokenUtils.getToken(CommonUtils.getUser(request, Constants.TOKEN));
        String token = network.getIfPresent(Constants.LoginWay.NETWORK + user.getEmailAccounts());
        if (token == null) {
            return ResultUtil.error(4011, messageInternational("FILE_LOGIN_DISK"));
        }
        if (StringUtils.isEmpty(path)) {
            path = "/";
        }
        String fileUrl = NetworkUrl.getFileUrl(token, path);
        HttpClient httpClient = new HttpClient();
        String data = "";
        try {
            data = httpClient.doGetWayTwo(fileUrl);
        } catch (Exception e) {
            return ResultUtil.error(500, messageInternational("FILE_CONNECT"));
        }
        if (StringUtils.isEmpty(data)) {
            return ResultUtil.error(500, messageInternational("FILE_DISK_MSG"));
        }
        BaiduData baiduData = JSONObject.parseObject(data, BaiduData.class);
        if (baiduData == null) {
            return ResultUtil.error(500, messageInternational("FILE_PARSE_ERROR"));
        }
        if (baiduData.getErrno() != 0) {
            return ResultUtil.error(500, messageInternational("FILE_USER_ERROR") + baiduData.getGuid_info());
        }
        return ResultUtil.success(baiduData.getList());
    }

    public ResponseResult<Object> userinfo(HttpServletRequest request) {
        Token user = jwtTokenUtils.getToken(CommonUtils.getUser(request, Constants.TOKEN));
        String token = network.getIfPresent(Constants.LoginWay.NETWORK + user.getEmailAccounts());
        if (token == null) {
            return ResultUtil.error(4011, messageInternational("FILE_LOGIN_DISK"));
        }
        String userInfo = network.getIfPresent(Constants.LoginWay.NETWORK_USER + user.getEmailAccounts());
        HashMap hashMap = JSONObject.parseObject(userInfo, HashMap.class);
        if (hashMap != null && hashMap.containsKey("errno")) {
            int errno = (int) hashMap.get("errno");
            if (errno != 0) {
                return ResultUtil.error(500, messageInternational("FILE_USER_ERROR") + hashMap.get("errmsg"));
            }
            return ResultUtil.success(hashMap);
        } else {
            return ResultUtil.error(500, messageInternational("FILE_PARSE_ERROR"));
        }
    }

    public ResponseResult<Object> networkImport(HttpServletRequest request, FileImportRequest fileImportRequest) {
        Token user = jwtTokenUtils.getToken(CommonUtils.getUser(request, Constants.TOKEN));
        String token = network.getIfPresent(Constants.LoginWay.NETWORK + user.getEmailAccounts());
        String userInfo = network.getIfPresent(Constants.LoginWay.NETWORK_USER + user.getEmailAccounts());
        HashMap hashMap = JSONObject.parseObject(userInfo, HashMap.class);
        if (token == null) {
            return ResultUtil.error(4011, messageInternational("FILE_LOGIN_DISK"));
        }
        if (fileImportRequest == null || fileImportRequest.getFileIds() == null || StringUtils.isEmpty(fileImportRequest.getSpaceId())) {
            return ResultUtil.error(500, messageInternational("PARAMETER_ERROR"));
        }
        // Permission verification
        spaceControlConfig.spatialVerification(fileImportRequest.getSpaceId(), user.getEmailAccounts(), Constants.SpaceRole.LEVEL_OTHER);
        spaceControlConfig.validateSpacePermissions(user.getEmailAccounts(), fileImportRequest.getSpaceId(), SpaceRoleEnum.F_OTHER_BAIDU.getRole());
        // Verify spatial information
        Query query = new Query().addCriteria(Criteria.where("_id").is(fileImportRequest.getSpaceId()));
        Space space = mongoTemplate.findOne(query, Space.class);
        // Space Creator
        if (!space.getUserId().equals(user.getUserId())) {
            return ResultUtil.error(403, messageInternational("PERMISSION_DENIED"));
        }
        // Verify space capacity
        if (SpaceSizeControl.validation(space.getSpaceId())) {
            return ResultUtil.errorInternational("FILE_SIZE_FULL");
        }
        String spacePath;
        try {
            ElfinderStorage elfinderStorage = elfinderStorageService.getElfinderStorage(request, space.getSpaceId());
            Target target = elfinderStorage.fromHash(fileImportRequest.getHash());
            spacePath = target.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return ResultUtil.errorInternational(messageInternational("FILE_SPACE_ERROR"));
        }
        List<String> sourcePaths = new ArrayList<>(fileImportRequest.getFileIds().size());
        for (Long fileId : fileImportRequest.getFileIds()) {
            sourcePaths.add(String.valueOf(fileId));
        }
        MiningTask miningTask = new MiningTask();
        miningTask.setCreateTime(new Date());
        miningTask.setEmail(user.getEmailAccounts());
        miningTask.setSpaceName("网盘导入");
        miningTask.setTaskId(CommonUtils.generateUUID());
        miningTask.setSpaceId(space.getSpaceId());
        miningTask.setUserId(user.getUserId());
        miningTask.setType(Constants.TaskType.NET_IMP);
        miningTask.setTargetRootPath(spacePath);
        miningTask.setSourcePaths(sourcePaths);
        // Baidu Netdisk User ID
        miningTask.setParam(getUk(hashMap.get("uk")).toString());
        miningTask.setState(5);
        mongoTemplate.save(miningTask);
        socketMessage(Constants.SocketType.TS_DRAW, miningTask.getTaskId(), miningTask.getEmail());
        Object taskCount = spaceStatistic.getIfPresent("task:" + user.getUserId() + space.getSpaceId());
        if (null != taskCount) {
            spaceStatistic.put("task:" + user.getUserId() + space.getSpaceId(), (long) taskCount + 1);
        }
        // Join the task queue to start processing
        SpaceQuery instance = SpaceQuery.getInstance();
        SpaceTaskUtils spaceTaskUtils = new SpaceTaskUtils(new ArrayList<>(16), webSocketProcess);
        spaceTaskUtils.setRootId(miningTask.getTaskId());
        spaceTaskUtils.setUser(miningTask.getEmail());
        instance.addCache(user.getUserId(), miningTask, mongoTemplate, spaceTaskUtils);
        return ResultUtil.success();
    }

    private void socketMessage(String type, String taskId, String email) {
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("type", type);
        messageMap.put("taskId", taskId);
        messageMap.put("mark", "space");
        try {
            webSocketProcess.sendMessage(email, JSONObject.toJSONString(messageMap));
        } catch (Exception e) {
            log.error("空间导入任务消息通知发送失败: {} " + e.getMessage());
        }
    }

    public void auth(HttpServletRequest request, HttpServletResponse response) {
        String user = CommonUtils.getUser(request, Constants.TOKEN);
        if (user == null) {
            return;
        }
        String netCallbackUrl = spaceUrl.getNetCallbackUrl();
        Object net = judgeNet(response);
        if (null == net) {
            return;
        }
        NetworkConf umtConf = (NetworkConf) net;
        try {
            String authUrl = NetworkUrl.getAuthUrl(umtConf.getAppKey(), umtConf.getHongPage() + netCallbackUrl, user);
            response.sendRedirect(authUrl);
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return;
    }

    private Object judgeNet(HttpServletResponse response) {
        CacheLoading cacheLoading = new CacheLoading(mongoTemplate);
        Object net = cacheLoading.loadingNet();
        if (net == null) {
            // Not added to the network disk configuration
            Map<String, Object> param = new HashMap<>();
            param.put("code", 500);
            param.put("message", messageInternational("FILE_DISK"));
            errorMsg(response, param);
        }
        return net;
    }

    private void errorMsg(HttpServletResponse response, Map param) {
        OutputStream out = null;
        try {
            response.addHeader("Content-Type", "application/json;charset=UTF-8");
            response.setCharacterEncoding("utf-8");
            response.setContentType("text/json");
            out = response.getOutputStream();
            out.write(JSON.toJSONString(param).getBytes(StandardCharsets.UTF_8));
            out.flush();
            response.flushBuffer();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return;
    }

    public void callback(HttpServletRequest request, HttpServletResponse response) {
        log.info("网盘回调： {} -------");
        String state = request.getParameter("state");
        String code = request.getParameter("code");
        if (StringUtils.isNotEmpty(code)) {
            Object net = judgeNet(response);
            if (net == null) {
                return;
            }
            NetworkConf networkConf = (NetworkConf) net;
            String netCallbackUrl = networkConf.getHongPage() + spaceUrl.getNetCallbackUrl();
            String tokenUrl = NetworkUrl.getTokenUrl(code, netCallbackUrl, networkConf.getAppKey(), networkConf.getSecretKey());
            HttpClient httpClient = new HttpClient();
            String data = null;
            try {
                data = httpClient.doGetWayTwo(tokenUrl);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            Auth auth = JSONObject.parseObject(data, Auth.class);
            String access_token = auth.getAccess_token();
            if (jwtTokenUtils.validateToken(state)) {
                Token token = jwtTokenUtils.getToken(state);
                network.put(Constants.LoginWay.NETWORK + token.getEmailAccounts(), access_token);
                // Update user information
                String userUrl = NetworkUrl.getUserUrl(access_token);
                try {
                    String resultInfo = httpClient.doGetWayTwo(userUrl);
                    HashMap hashMap = JSONObject.parseObject(resultInfo, HashMap.class);
                    hashMap.put("acc_token", access_token);
                    hashMap.put("ref_token", auth.getRefresh_token());
                    network.put(Constants.LoginWay.NETWORK_USER + token.getEmailAccounts(), resultInfo);
                    networkUpdate(hashMap);
                } catch (Exception e) {
                    network.invalidate(Constants.LoginWay.NETWORK + token.getEmailAccounts());
                    e.printStackTrace();
                    return;
                }
                // Message notification
                Map<String, String> message = new HashMap<>();
                message.put("mark", "network");
                message.put("type", "message");
                message.put("state", "success");
                try {
                    webSocketProcess.sendMessage(token.getEmailAccounts(), JSONObject.toJSONString(message));
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            } else {
                log.info("系统token过期");
                return;
            }
        }
        try {
            // response.sendRedirect(spaceUrl.getNetLoginSuccess());
            String html = "<script type='text/javascript'>location.href='" + spaceUrl.getNetLoginSuccess() + "';</script>";
            response.getWriter().print(html);
        } catch (IOException ioException) {
            ioException.printStackTrace();
            log.info("error: {} " + ioException.getMessage());
        }
    }

    private void networkUpdate(Map<String, Object> networkInfo) {
        Long uk = getUk(networkInfo.get("uk"));
        String avatar_url = (String) networkInfo.get("avatar_url");
        String baidu_name = (String) networkInfo.get("baidu_name");
        String netdisk_name = (String) networkInfo.get("netdisk_name");
        String acc_token = (String) networkInfo.get("acc_token");
        String ref_token = (String) networkInfo.get("ref_token");
        NetworkUser networkUser = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(uk.toString())), NetworkUser.class);
        if (null == networkUser) {
            NetworkUser netUser = new NetworkUser();
            netUser.setId(uk.toString());
            netUser.setAvatar_url(avatar_url);
            netUser.setBaidu_name(baidu_name);
            netUser.setNetdisk_name(netdisk_name);
            netUser.setAcc_token(acc_token);
            netUser.setRef_token(ref_token);
            netUser.setCreateTime(new Date());
            mongoTemplate.insert(netUser);
        } else {
            // update
            if (StringUtils.isEmpty(networkUser.getAvatar_url()) || !networkUser.getAvatar_url().equals(avatar_url)) {
                networkUser.setAvatar_url(avatar_url);
            }
            if (StringUtils.isEmpty(networkUser.getBaidu_name()) || !networkUser.getBaidu_name().equals(baidu_name)) {
                networkUser.setBaidu_name(baidu_name);
            }
            if (StringUtils.isEmpty(networkUser.getNetdisk_name()) || (StringUtils.isNotEmpty(netdisk_name) && !networkUser.getNetdisk_name().equals(netdisk_name))) {
                networkUser.setNetdisk_name(netdisk_name);
            }
            networkUser.setAcc_token(acc_token);
            networkUser.setRef_token(ref_token);
            networkUser.setLastUpdateTime(new Date());
            mongoTemplate.save(networkUser);
        }
        return;
    }

    private Long getUk(Object ukObject) {
        Long uk = 0L;
        try {
            uk = (long) (int) ukObject;
        } catch (Exception e) {
            try {
                uk = (long) ukObject;
            } catch (Exception e2) {
                uk = Long.valueOf((String) ukObject);
            }
        }
        return uk;
    }
}
