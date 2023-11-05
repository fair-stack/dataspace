package cn.cnic.dataspace.api.service.open;

import cn.cnic.dataspace.api.cacheLoading.CacheLoading;
import cn.cnic.dataspace.api.config.CreateMongoIndex;
import cn.cnic.dataspace.api.config.FileProperties;
import cn.cnic.dataspace.api.datax.admin.tool.sql.CommonDBUtils;
import cn.cnic.dataspace.api.datax.admin.tool.sql.JdbcConnectionFactory;
import cn.cnic.dataspace.api.datax.admin.tool.sql.SqlUtils;
import cn.cnic.dataspace.api.exception.CommonException;
import cn.cnic.dataspace.api.model.open.*;
import cn.cnic.dataspace.api.model.space.Space;
import cn.cnic.dataspace.api.model.space.child.AuthorizationPerson;
import cn.cnic.dataspace.api.model.statistics.SpaceDataStatistic;
import cn.cnic.dataspace.api.model.user.ConsumerDO;
import cn.cnic.dataspace.api.model.user.UserShow;
import cn.cnic.dataspace.api.repository.SpaceRepository;
import cn.cnic.dataspace.api.service.impl.UserServiceImpl;
import cn.cnic.dataspace.api.service.space.SpaceService;
import cn.cnic.dataspace.api.util.*;
import cn.hutool.extra.cglib.CglibUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.*;
import static cn.cnic.dataspace.api.service.impl.UserServiceImpl.COLLECTION_NAME;
import static cn.cnic.dataspace.api.util.CommonUtils.*;

@Slf4j
@Service
public class OpenService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private SpaceService spaceService;

    @Autowired
    private SpaceUrl spaceUrl;

    @Lazy
    @Autowired
    private CacheLoading cacheLoading;

    @Lazy
    @Autowired
    private CreateMongoIndex createMongoIndex;

    @Autowired
    private FileProperties fileProperties;

    private static String[] spaceType = { "private", "limited", "public" };

    /**
     * Unified verification request interception
     */
    private ResponseResult<Object> intercept(String appId) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        Application application = cacheLoading.getApplication(appId);
        if (null == application) {
            return resultError(APICodeType.AUTH_APP_KEY);
        }
        // Verify permissions
        AppAuthApi appAuthPath = cacheLoading.getAppAuthPath(appId);
        List<String> pathList = appAuthPath.getPathList();
        if (pathList.isEmpty()) {
            return resultError(APICodeType.AUTH_ROLE);
        }
        String requestURI = request.getRequestURI();
        if (!pathList.contains(requestURI)) {
            return resultError(APICodeType.AUTH_ROLE);
        }
        return ResultUtil.success();
    }

    /**
     * Request parameter encryption+duplicate verification+submission time verification
     */
    private ResponseResult<Object> verify(Object object) {
        Map map = JSONObject.parseObject(JSONObject.toJSONString(object), Map.class);
        String appKey = (String) map.get("appId");
        ResponseResult<Object> intercept = intercept(appKey);
        if (intercept.getCode() != 0) {
            return intercept;
        }
        Application application = cacheLoading.getApplication(appKey);
        String sign = (String) map.get("sign");
        map.remove("sign");
        if (map.containsKey("spaceLogo")) {
            map.remove("spaceLogo");
        }
        String str = ASCLLSort(map) + application.getAppSecret();
        try {
            String md5 = MD5.getMD5(str);
            if (!md5.equals(sign)) {
                return resultError(APICodeType.AUTH_SIGN);
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return resultError(APICodeType.SYSTEM_ERROR);
        }
        // Validation submission time
        // String timestamp = (String)map.get("timestamp");
        return ResultUtil.success();
    }

    /**
     * User registration open interface
     */
    public ResponseResult<Object> openRegister(RequestRegister requestRegister) {
        if (requestRegister == null) {
            return resultError(APICodeType.MISSING_PARAMETERS);
        }
        String appKey = requestRegister.getAppId();
        List<String> validation = CommonUtils.validation(requestRegister);
        if (validation.size() > 0) {
            return resultError(APICodeType.MISSING_PARAMETERS.getCode(), "参数错误: {} " + validation.toString());
        }
        // validate
        ResponseResult<Object> verify = verify(requestRegister);
        if (verify.getCode() != 0) {
            return verify;
        }
        Application application = cacheLoading.getApplication(appKey);
        String emailDe = requestRegister.getEmailAccounts();
        if (StringUtils.isNotEmpty(emailDe) && StringUtils.isNotEmpty(emailDe.trim())) {
            if (!CommonUtils.isEmail(emailDe.trim())) {
                return resultError(APICodeType.EMAIL_FORMAT);
            }
        } else {
            return resultError(APICodeType.MISSING_PARAMETERS.getCode(), "参数错误: {} 请输入邮箱地址!");
        }
        // Email verification
        String name = requestRegister.getName();
        if (StringUtils.isNotEmpty(name)) {
            if (CommonUtils.isSpecialChar(name)) {
                return resultError(APICodeType.SPECIAL_CHARACTERS.getCode(), "真实姓名不能带有特殊字符:" + CommonUtils.takeOutChar(name));
            }
        } else {
            return resultError(APICodeType.MISSING_PARAMETERS.getCode(), "参数错误: {} 请输入姓名!");
        }
        if (CommonUtils.isSpecialChar(requestRegister.getOrg())) {
            return resultError(APICodeType.SPECIAL_CHARACTERS.getCode(), "所在单位不能带有特殊字符:" + CommonUtils.takeOutChar(requestRegister.getOrg()));
        }
        // password verifiers
        if (StringUtils.isNotEmpty(requestRegister.getPassword())) {
            if (!CommonUtils.passVerify(requestRegister.getPassword())) {
                return resultError(APICodeType.PASSWORD_STRENGTH);
            }
        }
        String role = requestRegister.getRole();
        if (!role.equals(Constants.GENERAL) && !role.equals(Constants.SENIOR)) {
            return resultError(APICodeType.MISSING_PARAMETERS.getCode(), "用户角色不符");
        }
        String emailAccounts = emailDe.trim();
        Query query = new Query();
        query.addCriteria(Criteria.where("emailAccounts").is(emailAccounts));
        String id = "";
        synchronized (this) {
            ConsumerDO user = mongoTemplate.findOne(query, ConsumerDO.class, COLLECTION_NAME);
            if (user != null && (user.getState() == 0 || user.getState() == 2)) {
                // User information coverage
            } else if (user == null) {
                user = new ConsumerDO();
            } else {
                // User exists and returns user ID - to be considered
                return resultSuccess(APICodeType.SUCCESS, user.getId());
            }
            CglibUtil.copy(requestRegister, user);
            user.setCreateTime(LocalDateTime.now());
            user.setOrgChineseName(requestRegister.getOrg());
            user.setState(1);
            user.setAppKey(appKey);
            user.setAddWay(application.getAppName() + "-用户注册");
            user.setRoles(new ArrayList<String>() {

                {
                    add(role);
                }
            });
            ConsumerDO insert = mongoTemplate.save(user);
            id = insert.getId();
        }
        return resultSuccess(APICodeType.SUCCESS, id);
    }

    /**
     * Space creation open interface
     */
    public ResponseResult<Object> spaceCreate(RequestSpace requestSpace) {
        if (requestSpace == null) {
            return resultError(APICodeType.MISSING_PARAMETERS);
        }
        List<String> validation = CommonUtils.validation(requestSpace);
        if (!validation.isEmpty()) {
            return resultError(APICodeType.MISSING_PARAMETERS.getCode(), "参数错误: {} " + validation.toString());
        }
        ResponseResult<Object> verify = verify(requestSpace);
        if (verify.getCode() != 0) {
            return verify;
        }
        Space space = new Space();
        String spaceId = generateSnowflake();
        space.setSpaceId(spaceId);
        String spaceName = requestSpace.getSpaceName().trim();
        if (isSpecialChar(spaceName)) {
            return resultError(APICodeType.SPECIAL_CHARACTERS.getCode(), "空间名不能包含特殊字符:" + CommonUtils.takeOutChar(spaceName));
        }
        if ((spaceName.length()) > 100) {
            return resultError(APICodeType.STRING_LENGTH.getCode(), "空间名请在100个文字以内!");
        }
        // Tags judgment
        List<String> tags = requestSpace.getTags();
        if (null == tags) {
            requestSpace.setTags(new ArrayList<>(0));
        }
        if ((requestSpace.getDescription().length()) > 300) {
            return resultError(APICodeType.STRING_LENGTH.getCode(), "空间描述不要超过300个字!");
        }
        // Space type judgment
        String type = requestSpace.getType();
        if (!Arrays.asList(spaceType).contains(type)) {
            return resultError(APICodeType.MISSING_PARAMETERS.getCode(), "空间类型错误!");
        }
        if (type.equals("public")) {
            space.setIsPublic(1);
            space.setApplyIs(1);
        } else if (type.equals("limited")) {
            space.setApplyIs(1);
            space.setIsPublic(0);
        } else {
            space.setApplyIs(0);
            space.setIsPublic(0);
        }
        String spaceCode = requestSpace.getSpaceCode();
        if (StringUtils.isNotEmpty(spaceCode)) {
            if (isSpecialChar(spaceCode)) {
                return resultError(APICodeType.SPECIAL_CHARACTERS.getCode(), "空间code码不能包含特殊符号:" + takeOutChar(spaceCode));
            }
            if (!spaceCode.matches("^[a-z0-9A-Z]*")) {
                return resultError(APICodeType.SPACE_LOGO.getCode(), "空间code码请输入英文和数字的组合!");
            }
        }
        String spaceLogo = space.getSpaceLogo();
        if (StringUtils.isNotEmpty(spaceLogo) && StringUtils.isNotEmpty(spaceLogo.trim())) {
            String head = String.valueOf(new Date().getTime());
            String imagePath = "/" + Constants.Image.image + "/" + Constants.Image.SPACE + "/" + space.getSpaceId() + "_" + head + ".jpg";
            try {
                CommonUtils.generateImage(spaceLogo, spaceUrl.getRootDir() + imagePath);
                space.setSpaceLogo(imagePath);
            } catch (Exception e) {
                space.setSpaceLogo(spaceLogo);
            }
        } else {
            space.setSpaceLogo(null);
        }
        synchronized (this) {
            return createSpace(space, requestSpace);
        }
    }

    private ResponseResult<Object> createSpace(Space space, RequestSpace requestSpace) {
        String homeUrl = requestSpace.getSpaceCode();
        String spaceName = requestSpace.getSpaceName();
        if (StringUtils.isNotEmpty(homeUrl)) {
            long count = mongoTemplate.count(new Query().addCriteria(Criteria.where("homeUrl").is(homeUrl)), Space.class);
            if (count > 0) {
                return resultError(APICodeType.SPACE_CODE);
            }
            space.setHomeUrl(homeUrl);
        }
        String userId = requestSpace.getUserId();
        ConsumerDO consumerDO = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(userId)), ConsumerDO.class);
        if (null == consumerDO) {
            return resultError(APICodeType.USER_NOT_EXIST);
        }
        if (StringUtils.isNotEmpty(consumerDO.getAppKey())) {
            if (StringUtils.isEmpty(consumerDO.getAppKey()) || !consumerDO.getAppKey().equals(requestSpace.getAppId())) {
                return resultError(APICodeType.AUTH_ROLE);
            }
        }
        if (spaceRepository.findByUserIdAndSpaceName(userId, spaceName) != null) {
            return resultError(APICodeType.SPACE_REPEAT);
        }
        // Create a corresponding database for the corresponding db space
        String dbName = SqlUtils.getUUID32();
        String createDBSql = SqlUtils.generateCreateDBSql(dbName);
        Connection connection = null;
        try {
            connection = new JdbcConnectionFactory().getConnection();
            CommonDBUtils.executeSql(connection, createDBSql);
        } catch (Exception e) {
            log.error("创建db失败");
            log.error(e.getMessage(), e);
            return resultError(APICodeType.SPACE_DB);
        } finally {
            CommonDBUtils.closeDBResources(connection);
        }
        space.setDbName(dbName);
        Long spaceSize = requestSpace.getSpaceSize();
        space.setSpaceSize((null == spaceSize ? 0 : spaceSize));
        CglibUtil.copy(requestSpace, space);
        spaceService.createSpaceImpl(space, consumerDO, "1");
        // Add spatial database mapping index
        createMongoIndex.createSpaceFileMappingIndex(space.getSpaceId());
        return resultSuccess(APICodeType.SUCCESS, space.getSpaceId());
    }

    /**
     * Verification
     */
    public ResponseResult<Object> validation(String appId, String userId, String spaceId, String version, String timestamp, String sign) {
        Map<String, Object> paramMap = new HashMap<>(6);
        paramMap.put("appId", appId);
        paramMap.put("userId", userId);
        paramMap.put("spaceId", spaceId);
        paramMap.put("version", version);
        paramMap.put("timestamp", timestamp);
        paramMap.put("sign", sign);
        List<String> validation = validationMap(paramMap);
        if (!validation.isEmpty()) {
            return resultError(APICodeType.MISSING_PARAMETERS.getCode(), "参数错误: {} " + validation.toString());
        }
        return verify(paramMap);
    }

    /**
     * Detailed acquisition of spatial data
     */
    public ResponseResult<Object> spaceDataList(String appId, String version, String timestamp, String sign) {
        Map<String, Object> paramMap = new HashMap<>(4);
        paramMap.put("appId", appId);
        paramMap.put("version", version);
        paramMap.put("timestamp", timestamp);
        paramMap.put("sign", sign);
        List<String> validation = validationMap(paramMap);
        if (!validation.isEmpty()) {
            return resultError(APICodeType.MISSING_PARAMETERS.getCode(), "参数错误: {} " + validation.toString());
        }
        ResponseResult<Object> verify = verify(paramMap);
        if (verify.getCode() != 0) {
            return verify;
        }
        Query query = new Query().addCriteria(Criteria.where("state").is("1"));
        List<Space> spaceList = mongoTemplate.find(query, Space.class);
        List<Map<String, Object>> spaceMapList = new ArrayList<>(spaceList.size());
        for (Space space : spaceList) {
            Map<String, Object> map = new HashMap<>(3);
            map.put("name", space.getSpaceName());
            map.put("desc", space.getDescription());
            Query query1 = new Query().addCriteria(Criteria.where("spaceId").is(space.getSpaceId()));
            SpaceDataStatistic one = mongoTemplate.findOne(query1, SpaceDataStatistic.class);
            FileUtils fileUtils = new FileUtils();
            map.put("size", fileUtils.formFileSizeToGB(one.getDataSize()));
            map.put("fileNumber", one.getFileNum());
            spaceMapList.add(map);
        }
        return resultSuccess(APICodeType.SUCCESS, spaceMapList);
    }

    /**
     * Obtain user information
     */
    public ResponseResult<Object> userInfo(String appId, String version, String timestamp, String sign, String email) {
        Map<String, Object> paramMap = new HashMap<>(5);
        paramMap.put("appId", appId);
        paramMap.put("version", version);
        paramMap.put("email", email);
        paramMap.put("timestamp", timestamp);
        paramMap.put("sign", sign);
        List<String> validation = validationMap(paramMap);
        if (!validation.isEmpty()) {
            return resultError(APICodeType.MISSING_PARAMETERS.getCode(), "参数错误: {} " + validation.toString());
        }
        ResponseResult<Object> verify = verify(paramMap);
        if (verify.getCode() != 0) {
            return verify;
        }
        if (!CommonUtils.isEmail(email)) {
            return resultError(APICodeType.EMAIL_FORMAT);
        }
        UserShow consumerDO = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("emailAccounts").is(email)), UserShow.class, COLLECTION_NAME);
        if (null == consumerDO) {
            return resultError(APICodeType.USER_NOT_EXIST);
        }
        if (consumerDO.getState() != 1) {
            return resultError(APICodeType.USER_ACTIVATE);
        }
        if (consumerDO.getDisable() == 1) {
            return resultError(APICodeType.USER_DIS);
        }
        return resultSuccess(APICodeType.SUCCESS, consumerDO);
    }

    /**
     * Obtain spatial information
     */
    public ResponseResult<Object> spaceInfo(String appId, String version, String timestamp, String sign, String spaceId) {
        Map<String, Object> paramMap = new HashMap<>(5);
        paramMap.put("appId", appId);
        paramMap.put("version", version);
        paramMap.put("spaceId", spaceId);
        paramMap.put("timestamp", timestamp);
        paramMap.put("sign", sign);
        List<String> validation = validationMap(paramMap);
        if (!validation.isEmpty()) {
            return resultError(APICodeType.MISSING_PARAMETERS.getCode(), "参数错误: {} " + validation.toString());
        }
        ResponseResult<Object> verify = verify(paramMap);
        if (verify.getCode() != 0) {
            return verify;
        }
        Space space = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(spaceId)), Space.class);
        if (null == space) {
            return resultError(APICodeType.SPACE_NOT_EXIST);
        }
        if (space.getState().equals("0")) {
            return resultError(APICodeType.SPACE_REVIEWED);
        }
        if (space.getState().equals("2")) {
            return resultError(APICodeType.SPACE_GO_OFFLINE);
        }
        SpaceInfo spaceInfo = new SpaceInfo();
        CglibUtil.copy(space, spaceInfo);
        spaceInfo.setId(space.getSpaceId());
        spaceInfo.setType((space.getIsPublic() == 1 ? space.getApplyIs() == 1 ? "受限空间" : "私有空间" : "公开空间"));
        spaceInfo.setSpaceCode(space.getHomeUrl());
        if (StringUtils.isNotEmpty(space.getSpaceLogo())) {
            String spaceLogo = fileToBase64(new File(spaceUrl.getRootDir(), space.getSpaceLogo()));
            spaceInfo.setSpaceLogo(spaceLogo);
        }
        String ftpHost = spaceUrl.getFtpHost();
        if (ftpHost.contains(":")) {
            ftpHost = ftpHost.substring(0, ftpHost.indexOf(":"));
        }
        if (!spaceUrl.getShow().equals("21")) {
            ftpHost = ftpHost + ":" + spaceUrl.getShow();
        }
        Map<String, String> uploadLink = new HashMap<>(2);
        uploadLink.put("webDavLink", spaceUrl.getCallHost() + fileProperties.getWebDavPrefix() + FILE_SPLIT + space.getSpaceShort());
        uploadLink.put("ftpLink", "ftp://" + ftpHost + "/" + space.getSpaceShort());
        spaceInfo.setUploadLink(uploadLink);
        return resultSuccess(APICodeType.SUCCESS, spaceInfo);
    }

    /**
     * Space invitation
     */
    public ResponseResult<Object> spaceInvite(String appId, String version, String timestamp, String sign, String operaId, String spaceId, String userId, String role) {
        Map<String, Object> paramMap = new HashMap<>(5);
        paramMap.put("appId", appId);
        paramMap.put("version", version);
        paramMap.put("timestamp", timestamp);
        paramMap.put("sign", sign);
        paramMap.put("operaId", operaId);
        paramMap.put("spaceId", spaceId);
        paramMap.put("userId", userId);
        paramMap.put("role", role);
        List<String> validation = validationMap(paramMap);
        if (!validation.isEmpty()) {
            return resultError(APICodeType.MISSING_PARAMETERS.getCode(), "参数错误: {} " + validation.toString());
        }
        if (!role.equals(Constants.SpaceRole.SENIOR) && !role.equals(Constants.SpaceRole.GENERAL)) {
            return resultError(APICodeType.MISSING_PARAMETERS.getCode(), "参数错误: {} role - >" + role);
        }
        ResponseResult<Object> verify = verify(paramMap);
        if (verify.getCode() != 0) {
            return verify;
        }
        ConsumerDO operaCon = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(operaId)), ConsumerDO.class);
        ResponseResult<Object> objectResponseResult = judgeUser(operaCon);
        if (objectResponseResult.getCode() != 0) {
            return objectResponseResult;
        }
        ConsumerDO userCon = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(userId)), ConsumerDO.class);
        ResponseResult<Object> responseResult = judgeUser(userCon);
        if (responseResult.getCode() != 0) {
            return responseResult;
        }
        Space space = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(spaceId)), Space.class);
        if (null == space) {
            return resultError(APICodeType.SPACE_NOT_EXIST);
        }
        if (space.getState().equals("0")) {
            return resultError(APICodeType.SPACE_REVIEWED);
        }
        if (space.getState().equals("2")) {
            return resultError(APICodeType.SPACE_GO_OFFLINE);
        }
        Set<AuthorizationPerson> authorizationList = space.getAuthorizationList();
        if (null == authorizationList) {
            return resultError(APICodeType.AUTH_ROLE);
        }
        Set<String> authList = new HashSet<>(authorizationList.size());
        for (AuthorizationPerson authorizationPerson : authorizationList) {
            authList.add(authorizationPerson.getUserId());
        }
        if (!authList.contains(operaId)) {
            return resultError(APICodeType.AUTH_ROLE);
        }
        if (!authList.contains(userId)) {
            // join
            AuthorizationPerson authorizationPerson = new AuthorizationPerson(userCon);
            authorizationPerson.setRole(role);
            authorizationList.add(authorizationPerson);
            space.setAuthorizationList(authorizationList);
            mongoTemplate.save(space);
        }
        return resultSuccess(APICodeType.SUCCESS, null);
    }

    private ResponseResult<Object> judgeUser(ConsumerDO consumerDO) {
        if (null == consumerDO) {
            return resultError(APICodeType.USER_NOT_EXIST);
        }
        if (consumerDO.getState() != 1) {
            return resultError(APICodeType.USER_ACTIVATE);
        }
        if (consumerDO.getDisable() == 1) {
            return resultError(APICodeType.USER_DIS);
        }
        return ResultUtil.success();
    }

    /*Result return*/
    private ResponseResult<Object> resultError(APICodeType apiCodeType) {
        return ResultUtil.error(apiCodeType.getCode(), apiCodeType.getMsg(), null);
    }

    private ResponseResult<Object> resultError(int code, String message) {
        return ResultUtil.error(code, message, null);
    }

    private ResponseResult<Object> resultSuccess(APICodeType apiCodeType, Object date) {
        return ResultUtil.success(apiCodeType.getCode(), apiCodeType.getMsg(), date);
    }
}
