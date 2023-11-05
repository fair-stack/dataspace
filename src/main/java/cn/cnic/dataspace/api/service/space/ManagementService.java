package cn.cnic.dataspace.api.service.space;

import cn.cnic.dataspace.api.asynchronous.AsyncDeal;
import cn.cnic.dataspace.api.cacheLoading.CacheLoading;
import cn.cnic.dataspace.api.config.space.SpaceControlConfig;
import cn.cnic.dataspace.api.exception.CommonException;
import cn.cnic.dataspace.api.model.CacheData;
import cn.cnic.dataspace.api.model.manage.*;
import cn.cnic.dataspace.api.model.Restrict;
import cn.cnic.dataspace.api.model.backup.BackupSpaceMain;
import cn.cnic.dataspace.api.model.backup.BackupSpaceSubtasks;
import cn.cnic.dataspace.api.model.space.*;
import cn.cnic.dataspace.api.model.space.child.AuthorizationPerson;
import cn.cnic.dataspace.api.model.space.child.Operator;
import cn.cnic.dataspace.api.model.space.child.Person;
import cn.cnic.dataspace.api.model.space.child.SimpleSpace;
import cn.cnic.dataspace.api.model.backup.FtpHost;
import cn.cnic.dataspace.api.model.email.SysEmail;
import cn.cnic.dataspace.api.model.email.ToEmail;
import cn.cnic.dataspace.api.model.file.FileMapping;
import cn.cnic.dataspace.api.model.open.ApiAuth;
import cn.cnic.dataspace.api.model.open.Application;
import cn.cnic.dataspace.api.model.open.OpenApi;
import cn.cnic.dataspace.api.model.statistics.SpaceDataStatistic;
import cn.cnic.dataspace.api.model.statistics.SpaceTypeStatistic;
import cn.cnic.dataspace.api.model.user.ConsumerDO;
import cn.cnic.dataspace.api.repository.*;
import cn.cnic.dataspace.api.service.impl.UserServiceImpl;
import cn.cnic.dataspace.api.util.*;
import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import sun.misc.BASE64Encoder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Pattern;
import static cn.cnic.dataspace.api.model.space.SvnControlSetting.CLOSED;
import static cn.cnic.dataspace.api.model.space.SvnControlSetting.OPEN;
import static cn.cnic.dataspace.api.service.space.SpaceService.*;
import static cn.cnic.dataspace.api.util.CommonUtils.*;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;

/**
 * Management Service
 *
 * @author wangCc
 * @date 2021-10-09 15:50
 */
@Service
@EnableAsync
public class ManagementService {

    @Autowired
    private JwtTokenUtils jwtTokenUtils;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private UserRepository userRepository;

    @Lazy
    @Autowired
    private AsyncDeal asyncDeal;

    @Autowired
    private SpaceUrl spaceUrl;

    @Autowired
    private ApproveSettingRepository approveSettingRepository;

    @Autowired
    private SvnSpaceLogRepository svnSpaceLogRepository;

    @Autowired
    private RecentViewRepository recentViewRepository;

    @Autowired
    private CacheLoading cacheLoading;

    @Autowired
    private SpaceControlConfig spaceControlConfig;

    private final Cache<String, String> check = CaffeineUtil.getCHECK();

    private final Cache<String, String> tokenCache = CaffeineUtil.getTokenCache();

    private final Cache<String, List<String>> emailToken = CaffeineUtil.getEmailToken();

    private final Cache<String, Object> config = CaffeineUtil.getConfig();

    private final BASE64Encoder encoder = new BASE64Encoder();

    /**
     * recent view space list
     */
    public ResponseResult<Object> recent(String token) {
        return ResultUtil.success(mongoTemplate.find(new Query().addCriteria(Criteria.where("email").is(jwtTokenUtils.getEmail(token))).with(PageRequest.of(0, 10)).with(Sort.by(Sort.Direction.DESC, "dateTime")), RecentView.class));
    }

    /**
     * search any where
     */
    public ResponseResult<Object> searchAnywhere(String token, String content) {
        String userId = jwtTokenUtils.getUserIdFromToken(token);
        // String userId = token;
        Map<String, Object> result = new HashMap<>(16);
        // if ((!StringUtils.equals(type, "space")) && StringUtils.isNotBlank(content)) {
        // //file and folder
        // result.putAll(spaceService.search(userId, content));
        // }
        result.put("spaces", spaceList(userId, content));
        Criteria criteria = new Criteria().orOperator(Criteria.where("isPublic").is(1), Criteria.where("authorizationList.userId").is(userId));
        List<SimpleSpace> spaces = mongoTemplate.find(new Query().addCriteria(Criteria.where("state").is("1").andOperator(criteria)), SimpleSpace.class);
        List<Map<String, Object>> fileList = new ArrayList<>();
        List<Map<String, Object>> folderList = new ArrayList<>();
        for (SimpleSpace space : spaces) {
            // file
            fileList.addAll(searchFileMapping(content, space, 0));
            // folder
            folderList.addAll(searchFileMapping(content, space, 1));
        }
        result.put("file", fileList);
        result.put("folder", folderList);
        return ResultUtil.success(result);
    }

    /**
     * File retrieval
     */
    private List<Map<String, Object>> searchFileMapping(String content, SimpleSpace space, int type) {
        List<Map<String, Object>> fileList = new ArrayList<>();
        Criteria name = Criteria.where("name").regex(compile("^.*" + content + ".*$", CASE_INSENSITIVE));
        name.and("type").is(type);
        List<FileMapping> fileMappingList = mongoTemplate.find(new Query().addCriteria(name), FileMapping.class, space.getSpaceId());
        if (!fileMappingList.isEmpty()) {
            Map<String, Map<String, Object>> distinctMap = new HashMap<>();
            for (FileMapping fileMapping : fileMappingList) {
                String fileName = fileMapping.getName();
                if (distinctMap.containsKey(fileName)) {
                    Map<String, Object> fileMap = distinctMap.get(fileName);
                    fileMap.get("count");
                    fileMap.put("count", (int) fileMap.get("count") + 1);
                    distinctMap.put(fileName, fileMap);
                } else {
                    Map<String, Object> fileMap = new HashMap<>(5);
                    fileMap.put("name", fileName);
                    fileMap.put("count", 1);
                    fileMap.put("spaceId", space.getSpaceId());
                    fileMap.put("spaceName", space.getSpaceName());
                    fileMap.put("public", space.getIsPublic());
                    fileMap.put("homeUrl", space.getHomeUrl());
                    distinctMap.put(fileName, fileMap);
                }
            }
            for (String key : distinctMap.keySet()) {
                fileList.add(distinctMap.get(key));
            }
        }
        return fileList;
    }

    /**
     * search by name and tag for space
     */
    private List<SimpleSpace> spaceList(String userId, String spaceName) {
        List<SimpleSpace> spaces = mongoTemplate.find(new Query().addCriteria(Criteria.where("state").is("1").andOperator(new Criteria().andOperator(new Criteria().orOperator(Criteria.where("spaceName").regex(Pattern.compile("^.*" + CommonUtils.escapeExprSpecialWord(spaceName) + ".*$", Pattern.CASE_INSENSITIVE)), Criteria.where("tag").regex(Pattern.compile("^.*" + CommonUtils.escapeExprSpecialWord(spaceName) + ".*$", Pattern.CASE_INSENSITIVE)), Criteria.where("markdown").regex(Pattern.compile("^.*" + CommonUtils.escapeExprSpecialWord(spaceName) + ".*$", Pattern.CASE_INSENSITIVE)), Criteria.where("description").regex(Pattern.compile("^.*" + CommonUtils.escapeExprSpecialWord(spaceName) + ".*$", Pattern.CASE_INSENSITIVE))), new Criteria().orOperator(Criteria.where("isPublic").is(1), Criteria.where("applyIs").is(1), Criteria.where("authorizationList.userId").is(userId))))), SimpleSpace.class);
        List<String> spaceIds = cacheLoading.getUserApplySpaces(userId);
        for (SimpleSpace space : spaces) {
            if (StringUtils.equals(userId, space.getUserId())) {
                space.setOwned("1");
                continue;
            }
            for (AuthorizationPerson authorizationPerson : space.getAuthorizationList()) {
                if (StringUtils.equals(userId, authorizationPerson.getUserId())) {
                    space.setOwned("1");
                    break;
                }
            }
            if (spaceIds.contains(space.getSpaceId())) {
                space.setHaveApply(1);
            }
        }
        return spaces;
    }

    /**
     * find spaceId by home index
     */
    public ResponseResult<Object> homeIndex(String index) {
        return spaceRepository.findByHomeUrl(index).<ResponseResult<Object>>map(space -> ResultUtil.success(space.getSpaceId())).orElseGet(() -> ResultUtil.errorInternational("INDEX_NOT_FOUND"));
    }

    /**
     * all space for data space administrator
     */
    public ResponseResult<Object> allSpace(String token, String pageOffset, String pageSize, String order, String spaceName, String member, String tag, String start, String end) {
        Query query = new Query();
        if (StringUtils.isNotBlank(spaceName)) {
            query.addCriteria(Criteria.where("spaceName").regex(compile("^.*" + spaceName + ".*$", CASE_INSENSITIVE)));
        }
        if (StringUtils.isNotBlank(member)) {
            query.addCriteria(Criteria.where("authorizationList.userName").regex(compile("^.*" + member + ".*$", CASE_INSENSITIVE)));
        }
        if (StringUtils.isNotBlank(tag)) {
            query.addCriteria(Criteria.where("tags").regex(compile("^.*" + tag + ".*$", CASE_INSENSITIVE)));
        }
        if (StringUtils.isNotEmpty(start) || StringUtils.isNotEmpty(end)) {
            Criteria criteria = new Criteria().and("createDateTime");
            if (StringUtils.isNotEmpty(start)) {
                criteria.gte(start);
            }
            if (StringUtils.isNotEmpty(end)) {
                criteria.lte(end);
            }
            query.addCriteria(criteria);
        }
        if (StringUtils.isNotBlank(order)) {
            query.with(Sort.by(StringUtils.equals(order, "desc") ? DESC : ASC, "createDateTime"));
        }
        Map<String, Object> result = new HashMap<>(16);
        result.put("count", mongoTemplate.count(query, Space.class));
        List<Map<String, Object>> mapList = new ArrayList<>();
        for (Space simpleSpace : mongoTemplate.find(query.with(PageRequest.of(Integer.parseInt(pageOffset), Integer.parseInt(pageSize))), Space.class)) {
            Map<String, Object> map = new HashMap<>(16);
            map.put("spaceId", simpleSpace.getSpaceId());
            map.put("spaceName", simpleSpace.getSpaceName());
            map.put("tags", simpleSpace.getTags());
            map.put("spaceLogo", simpleSpace.getSpaceLogo());
            map.put("createTime", simpleSpace.getCreateDateTime());
            map.put("members", simpleSpace.getAuthorizationList());
            map.put("state", simpleSpace.getState());
            map.put("size", simpleSpace.getSpaceSize());
            Optional<ConsumerDO> consumerDoOptional = userRepository.findById(simpleSpace.getUserId());
            map.put("owner", consumerDoOptional.isPresent() ? consumerDoOptional.get().getName() : "已注销");
            mapList.add(map);
        }
        result.put("content", mapList);
        return ResultUtil.success(result);
    }

    /**
     * rest password email notify
     */
    public ResponseResult<Object> pwdEmail(String email, String name, String verificationCode, HttpServletRequest request) {
        String token = jwtTokenUtils.getToken(request);
        if (StringUtils.isNotEmpty(token)) {
            String username = jwtTokenUtils.getEmail(token);
            if (username != null) {
                email = jwtTokenUtils.getEmail(token);
            }
        }
        if (StringUtils.isEmpty(token)) {
            // Verification code verification
            if (StringUtils.isEmpty(verificationCode) || StringUtils.isEmpty(verificationCode.trim())) {
                return ResultUtil.errorInternational("VERIFICATION_CODE_ERROR");
            }
            HttpSession session = request.getSession();
            Object attribute = session.getAttribute(Constants.EmailSendType.EMAIL_CODE);
            if (attribute == null) {
                return ResultUtil.errorInternational("VERIFICATION_CODE");
            }
            if (!attribute.toString().equals(verificationCode.toLowerCase())) {
                return ResultUtil.errorInternational("VERIFICATION_CODE_ERROR");
            }
        }
        String ipAddr = CommonUtils.getIpAddr(request);
        // Email verification
        if (!CommonUtils.isEmail(email)) {
            return ResultUtil.errorInternational("EMAIL_INCORRECT");
        }
        Query emailAccounts = new Query().addCriteria(Criteria.where("emailAccounts").is(email));
        ConsumerDO user = mongoTemplate.findOne(emailAccounts, ConsumerDO.class);
        if (user == null) {
            return ResultUtil.errorInternational("EMAIL_ERROR");
        }
        int type = 0;
        if (StringUtils.isNotEmpty(name)) {
            if (!user.getName().equals(name)) {
                // Forgot password
                return ResultUtil.errorInternational("EMAIL_ERROR");
            }
            type = 1;
        }
        if (user.getState() == 2) {
            return ResultUtil.errorInternational("AUTH_EMAIL_UNREGISTERED");
        }
        if (user.getState() == 0 || user.getDisable() == 1) {
            return ResultUtil.errorInternational("EMAIL_FORBIDDEN");
        }
        String current = CommonUtils.getCurrentDateTimeString();
        Restrict re = judgePawCount(ipAddr, current, type);
        if (re.getResult()) {
            return ResultUtil.errorInternational("RETRIEVE_LIMIT");
        } else {
            Restrict result = judgePawCount(email, current, type);
            if (result.getResult()) {
                return ResultUtil.errorInternational("RETRIEVE_LIMIT");
            } else {
                // Verify email sending interval time
                String key = Constants.EmailSendType.FORGET_PWD + emailAccounts;
                if (CaffeineUtil.checkEmailAging(key)) {
                    return ResultUtil.success(messageInternational("SEND_EMAIL_TIME"));
                }
                // IP+email limit times+1
                mongoTemplate.save(re);
                mongoTemplate.save(result);
            }
        }
        long stringTime = System.currentTimeMillis();
        String code = SMS4.Encryption("pwdBack&" + email + "&" + stringTime);
        Map<String, Object> param = new HashMap<>();
        param.put("name", user.getName());
        param.put("email", user.getEmailAccounts());
        param.put("url", spaceUrl.getCallHost() + "/api/ps.av?code=" + code);
        asyncDeal.send(param, EmailModel.EMAIL_PASS(), EmailModel.EMAIL_PASS().getType());
        return ResultUtil.success(messageInternational("SEND_EMAIL"));
    }

    /**
     * rest password
     */
    public ResponseResult<Object> restPwd(String pwd, String confirmPwd, HttpServletRequest request) {
        String id = request.getSession().getId();
        String ifPresent = check.getIfPresent(id);
        if (ifPresent == null) {
            return ResultUtil.errorInternational("LINK_ERROR");
        }
        String password = RSAEncrypt.decrypt(pwd);
        String conPwd = RSAEncrypt.decrypt(confirmPwd);
        if (StringUtils.isNotBlank(password) && StringUtils.isNotBlank(conPwd)) {
            if (StringUtils.isEmpty(password.trim()) || StringUtils.isEmpty(conPwd.trim()) || (!CommonUtils.passVerify(password))) {
                return ResultUtil.errorInternational("PWD_STRENGTH");
            }
            if (!StringUtils.equals(password, conPwd)) {
                return ResultUtil.errorInternational("PWD_NOT_MATCH");
            }
        } else {
            return ResultUtil.errorInternational("LINK_ERROR");
        }
        String[] split = ifPresent.split("&");
        Query query = new Query();
        query.addCriteria(Criteria.where("emailAccounts").is(split[0]));
        ConsumerDO user = mongoTemplate.findOne(query, ConsumerDO.class, UserServiceImpl.COLLECTION_NAME);
        if (null == user) {
            return ResultUtil.errorInternational("USER_UNREGISTERED");
        }
        Update update = new Update();
        update.set("password", pwd);
        mongoTemplate.upsert(query, update, UserServiceImpl.COLLECTION_NAME);
        // Clear account online users
        List<String> tokenList = emailToken.getIfPresent(user.getEmailAccounts());
        if (null != tokenList && tokenList.size() > 0) {
            for (String t : tokenList) {
                tokenCache.invalidate(t);
            }
            emailToken.invalidate(user.getEmailAccounts());
        }
        // Clear Activation
        check.put(split[0] + split[1], split[1]);
        check.invalidate(id);
        return ResultUtil.success();
    }

    /**
     * check operator whether space member
     */
    public boolean member(String token, String spaceId) {
        try {
            spaceControlConfig.spatialVerification(spaceId, jwtTokenUtils.getEmail(token), Constants.SpaceRole.LEVEL_OTHER);
        } catch (CommonException e) {
            return false;
        }
        return true;
    }

    /**
     * system admin enter space role check
     */
    public ResponseResult<Object> roleCheck(String token, String spaceId) {
        String userIdFromToken = jwtTokenUtils.getUserIdFromToken(token);
        final Optional<Space> spaceOptional = spaceRepository.findById(spaceId);
        Space space = spaceOptional.orElse(new Space());
        String role = "0";
        for (AuthorizationPerson authorizationPerson : Objects.requireNonNull(space).getAuthorizationList()) {
            if (StringUtils.equals(userIdFromToken, authorizationPerson.getUserId())) {
                String authorizationPersonRole = authorizationPerson.getRole();
                switch(authorizationPersonRole) {
                    case SPACE_OWNER:
                        role = "1";
                        break;
                    case SPACE_SENIOR:
                        role = "2";
                        break;
                    case SPACE_GENERAL:
                        role = "3";
                        break;
                    default:
                        role = "0";
                        break;
                }
            }
        }
        return ResultUtil.success(role);
    }

    /**
     * space admin enter space
     */
    public ResponseResult<Object> enterSpace(String token, String spaceId) {
        String userIdFromToken = jwtTokenUtils.getUserIdFromToken(token);
        Optional<Space> spaceOptional = spaceRepository.findById(spaceId);
        if (spaceOptional.isPresent()) {
            Space space = spaceOptional.get();
            space.getAuthorizationList().add(new AuthorizationPerson(userRepository.findById(userIdFromToken).get(), SPACE_SENIOR));
            spaceRepository.save(space);
            svnSpaceLogRepository.save(SpaceSvnLog.builder().spaceSvnId(generateSnowflake()).spaceId(spaceId).action(SpaceSvnLog.ACTION_MEMBER).description(jwtTokenUtils.getUsernameFromToken(token) + "（" + jwtTokenUtils.getEmail(token) + messageInternational("ENTER_SPACE")).version(-2).operatorId(userIdFromToken).operator(new Operator(userRepository.findById(userIdFromToken).get())).createTime(new Date()).build());
            return ResultUtil.success(messageInternational("ADDED_SUCCESSFULLY"));
        } else {
            return ResultUtil.errorInternational("RESOURCE_DOES_NOT_EXIST");
        }
    }

    /**
     * space online offline
     */
    public ResponseResult<Object> upDown(String token, String spaceId) {
        Optional<Space> spaceOptional = spaceRepository.findById(spaceId);
        if (spaceOptional.isPresent()) {
            Space space = spaceOptional.get();
            space.setState(StringUtils.equals(space.getState(), "1") ? "2" : "1");
            spaceRepository.save(space);
            if (StringUtils.equals(space.getState(), "2")) {
                recentViewRepository.deleteBySpaceId(spaceId);
                // Stop space backup task
                asyncDeal.deleteBackupTask(spaceId, false);
                cacheLoading.clearSpaceCaffeine(spaceId, space.getSpaceShort());
            }
            // Statistical status modification
            Query query = new Query().addCriteria(Criteria.where("spaceId").is(spaceId));
            Update update = new Update();
            update.set("state", Integer.valueOf(space.getState()));
            mongoTemplate.findAndModify(query, update, new FindAndModifyOptions().returnNew(true).upsert(true), SpaceDataStatistic.class);
            return ResultUtil.success(space.getState());
        } else {
            return ResultUtil.errorInternational("RESOURCE_DOES_NOT_EXIST");
        }
    }

    /**
     * Space capacity modification
     */
    public ResponseResult<Object> spaceCapacity(String token, String spaceId, String capacity) {
        if (StringUtils.isEmpty(spaceId) || StringUtils.isEmpty(capacity)) {
            return ResultUtil.errorInternational("GENERAL_PARAMETER_ERROR");
        }
        Query query = new Query().addCriteria(Criteria.where("_id").is(spaceId));
        Space spaceSimple = mongoTemplate.findOne(query, Space.class);
        if (null == spaceSimple) {
            return ResultUtil.errorInternational("RELEASE_SPACE_DELETE");
        }
        if (spaceSimple.getState().equals("0")) {
            return ResultUtil.errorInternational("SPACE_RE_APPLY");
        }
        if (spaceSimple.getState().equals("2")) {
            return ResultUtil.errorInternational("SPACE_OFFLINE_FORBIDDEN");
        }
        long totalSize = Long.valueOf(capacity) * SIZE_1G;
        Update update = new Update();
        update.set("spaceSize", (spaceSimple.getSpaceSize() + totalSize));
        synchronized (this) {
            mongoTemplate.upsert(query, update, Space.class);
            SpaceSizeControl.updateCapacity(spaceId, (spaceSimple.getSpaceSize() + totalSize));
        }
        return ResultUtil.success();
    }

    /**
     * get approve setting
     */
    public ResponseResult<Object> approve(String token) {
        return ResultUtil.success(approveSettingRepository.findAll().get(0));
    }

    /**
     * update approve setting
     * and update space service cache
     */
    public ResponseResult<Object> approved(String token, String approved, String gb) {
        ApproveSetting approveSetting = approveSettingRepository.findAll().get(0);
        if (StringUtils.isNotBlank(approved)) {
            approveSetting.setApproved(approved);
            // update space service cache
            SpaceService.SPACE_AUDIT.put("audit", approved);
        }
        if (StringUtils.isNotBlank(gb) && StringUtils.isNumeric(gb)) {
            approveSetting.setGb(Long.parseLong(gb));
            // update space service cache
            SpaceService.SPACE_AUDIT.put("spaceSize", gb);
        } else {
            return ResultUtil.errorInternational("DIGITAL_ERROR");
        }
        approveSettingRepository.save(approveSetting);
        return ResultUtil.success();
    }

    private Restrict judgePawCount(String main, String date, int type) {
        boolean result = false;
        Restrict restrict = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("main").is(main).and("date").is(date).and("type").is(type)), Restrict.class);
        if (restrict != null) {
            long count = restrict.getCount();
            if (count >= 3) {
                result = true;
            } else {
                restrict.setCount(restrict.getCount() + 1);
            }
        } else {
            restrict = new Restrict();
            restrict.setCount(1);
            restrict.setCreateTime(new Date());
            restrict.setMain(main);
            restrict.setType(type);
            restrict.setDate(date);
        }
        restrict.setResult(result);
        return restrict;
    }

    /**
     * svn control query
     */
    public ResponseResult<Object> svnControl(String token) {
        if (StringUtils.isBlank(token)) {
            return ResultUtil.errorInternational("NEED_TOKEN");
        }
        final List<SvnControlSetting> all = mongoTemplate.query(SvnControlSetting.class).all();
        return ResultUtil.success(all.size() > 0 ? all.get(0).getNeed() : CLOSED);
    }

    /**
     * update svn control setting
     */
    public ResponseResult<Object> updateSvn(String token) {
        final List<SvnControlSetting> all = mongoTemplate.query(SvnControlSetting.class).all();
        SvnControlSetting svnControlSetting = new SvnControlSetting();
        if (all.size() > 0) {
            svnControlSetting = all.get(0);
            svnControlSetting.setNeed(StringUtils.equals(svnControlSetting.getNeed(), CLOSED) ? OPEN : CLOSED);
        } else {
            svnControlSetting.setSvnId(generateSnowflake());
            svnControlSetting.setNeed(CLOSED);
        }
        mongoTemplate.save(svnControlSetting);
        return ResultUtil.success(svnControlSetting.getNeed());
    }

    /**
     * Obtain
     */
    public ResponseResult<Object> getFtp(String token) {
        List<FtpHost> all = mongoTemplate.findAll(FtpHost.class);
        return ResultUtil.success(all);
    }

    /**
     * Configure or modify
     */
    public ResponseResult<Object> setFtp(String token, FtpHost ftpHost) {
        Token user = jwtTokenUtils.getToken(token);
        if (null == ftpHost) {
            return ResultUtil.errorInternational("PARAMETER_ERROR");
        }
        String host = ftpHost.getHost();
        String port = ftpHost.getPort();
        if (StringUtils.isEmpty(host) || StringUtils.isEmpty(port)) {
            return ResultUtil.errorInternational("PARAMETER_ERROR");
        }
        ftpHost.setFounder(user.getEmailAccounts());
        if (ftpHost.getInvoke() == null) {
            ftpHost.setInvoke(true);
        }
        if (StringUtils.isNotEmpty(ftpHost.getId())) {
            FtpHost id = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(ftpHost.getId())), FtpHost.class);
            if (null == id) {
                return ResultUtil.errorInternational("RESOURCE_DOES_NOT_EXIST");
            }
            ftpHost.setCreateTime(id.getCreateTime());
            ftpHost.setLastUpdateTime(new Date());
        } else {
            ftpHost.setCreateTime(new Date());
            ftpHost.setLastUpdateTime(new Date());
        }
        mongoTemplate.save(ftpHost);
        return ResultUtil.success();
    }

    public ResponseResult<Object> getSysEmail(String token) {
        SystemConf type = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("type").is(Constants.CaffeType.SYS_EMAIL)), SystemConf.class);
        return ResultUtil.success(type);
    }

    public ResponseResult<Object> setSysEmail(String token, SysEmail sysEmail) {
        String email = jwtTokenUtils.getEmail(token);
        List<String> validation = validation(sysEmail);
        if (!validation.isEmpty()) {
            return ResultUtil.error(validation.toString());
        }
        String password = sysEmail.getPassword();
        String decrypt = RSAEncrypt.decrypt(password);
        if (StringUtils.isEmpty(decrypt)) {
            return ResultUtil.errorInternational("SYSTEM_ERROR");
        }
        sysEmail.setFrom(sysEmail.getUsername());
        SystemConf type = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("type").is(Constants.CaffeType.SYS_EMAIL)), SystemConf.class);
        if (null != type) {
            type.setConf(sysEmail);
            type.setLastModifier(email);
            type.setLastUpdateTime(new Date());
            mongoTemplate.save(type);
        } else {
            type = new SystemConf();
            type.setType(Constants.CaffeType.SYS_EMAIL);
            type.setConf(sysEmail);
            type.setLastModifier(email);
            type.setCreateTime(new Date());
            mongoTemplate.insert(type);
        }
        // Clear Cache - Clear Mail Sending Configuration Class
        config.invalidate(Constants.CaffeType.SYS_EMAIL);
        EmailUtils.initInstance();
        return ResultUtil.success();
    }

    public ResponseResult<Object> testSend(String email) {
        EmailModel emailType = EmailModel.EMAIL_TEST_SEND();
        Map<String, Object> map = new HashMap<>(16);
        map.put("email", email);
        ToEmail toEmail = new ToEmail();
        try {
            toEmail.setTos(new String[] { (String) map.get("email") });
        } catch (Exception e) {
            toEmail.setTos((String[]) map.get("email"));
        }
        Map<String, Object> objectMap = (Map) cacheLoading.loadingConfig();
        String dataSpaceName = objectMap.get("dataSpaceName").toString();
        emailType.setSubject("【" + dataSpaceName + emailType.getSubject());
        toEmail.setSubject(emailType.getSubject());
        map.put("org", dataSpaceName);
        // Encapsulation template information
        map.put("title", emailType.getTitle());
        map.put("call", emailType.getCall());
        map.put("message", emailType.getMessage());
        map.put("end", emailType.getEnd());
        map.put("copyright", objectMap.get("copyright").toString());
        boolean b = asyncDeal.emailSend(toEmail, map, emailType);
        if (b) {
            return ResultUtil.success();
        }
        return ResultUtil.error("邮件发送失败...！");
    }

    public ResponseResult<Object> setSpaceTotal(String token, String total) {
        if (null == total) {
            return ResultUtil.errorInternational("SYSTEM_ERROR");
        }
        ApproveSetting approveSetting = approveSettingRepository.findAll().get(0);
        long totalSize = Long.valueOf(total) * SIZE_1G;
        approveSetting.setStorage(totalSize);
        SpaceService.SPACE_AUDIT.put("storage", totalSize);
        approveSettingRepository.save(approveSetting);
        return ResultUtil.success();
    }

    /**
     * Using Storage Details
     */
    public ResponseResult<Object> sysDet(String token, boolean type) {
        Map<String, Object> resultMap = new HashMap<>(4);
        // Actual storage capacity of the system
        CacheData cacheData = mongoTemplate.findOne(new Query(), CacheData.class);
        String dataSize = cacheData.getFileSize();
        if (type) {
            resultMap.put("actual", dataSize == null ? "0L" : dataSize);
        } else {
            resultMap.put("actual", (dataSize == null ? 0L : cacheData.getFileSizeLong()));
        }
        // System allocated storage capacity
        List<AggregationOperation> aggList = new ArrayList<>(3);
        aggList.add(Aggregation.match(Criteria.where("state").is("1")));
        aggList.add(Aggregation.group().sum("spaceSize").as("size"));
        Aggregation aggregation = Aggregation.newAggregation(aggList);
        AggregationResults<Document> storageDocument = mongoTemplate.aggregate(aggregation, "space", Document.class);
        long size = 0L;
        for (Document document : storageDocument) {
            size = (Long) document.get("size");
        }
        if (type) {
            resultMap.put("allocated", FileUtils.formFileSize(size));
        } else {
            resultMap.put("allocated", size);
        }
        // Total storage capacity
        Object storage = SPACE_AUDIT.get("storage");
        long storageTotal = 0L;
        if (storage == null) {
            ApproveSetting approveSetting = approveSettingRepository.findAll().get(0);
            Long storage1 = approveSetting.getStorage();
            if (null != storage1) {
                storageTotal = approveSetting.getStorage();
            }
        } else {
            storageTotal = (long) storage;
        }
        resultMap.put("storage", storageTotal);
        // Unallocated
        if (type) {
            resultMap.put("unabsorbed", FileUtils.formFileSize(storageTotal == 0L ? 0L : storageTotal - size));
        } else {
            resultMap.put("unabsorbed", storageTotal == 0L ? 0L : storageTotal - size);
        }
        return ResultUtil.success(resultMap);
    }

    /**
     * Disaster recovery list
     */
    public ResponseResult<Object> recoveryList(String token, Integer page, Integer size, String spaceName) {
        Query query = new Query();
        if (StringUtils.isNotEmpty(spaceName)) {
            Pattern pattern = Pattern.compile("^.*" + CommonUtils.escapeExprSpecialWord(spaceName) + ".*$", Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where("spaceName").regex(pattern));
        }
        long count = mongoTemplate.count(query, BackupSpaceMain.class);
        List<BackupSpaceMain> resultList = null;
        if (count > 0) {
            query.with(Sort.by(DESC, "createTime"));
            query.with(PageRequest.of(page - 1, size));
            List<BackupSpaceMain> backupSpaceMains = mongoTemplate.find(query, BackupSpaceMain.class);
            for (BackupSpaceMain backupSpaceMain : backupSpaceMains) {
                backupSpaceMain.setSpacePath(null);
            }
            resultList = backupSpaceMains;
        }
        Map<String, Object> resultMap = new HashMap<>(2);
        resultMap.put("count", count);
        resultMap.put("data", resultList);
        return ResultUtil.success(resultMap);
    }

    public ResponseResult<Object> subtaskList(String token, Integer page, Integer size, String jobId, String state, String startTime, String endTime) {
        if (StringUtils.isEmpty(jobId) || StringUtils.isEmpty(state)) {
            return ResultUtil.errorInternational("SYSTEM_ERROR");
        }
        if (!state.equals("all") && !state.equals("1") && !state.equals("2") && !state.equals("3")) {
            return ResultUtil.errorInternational("SYSTEM_ERROR");
        }
        Criteria criteria = Criteria.where("jobId").is(jobId);
        if (!state.equals("all")) {
            criteria.and("state").is(Integer.valueOf(state));
        }
        if (StringUtils.isNotEmpty(startTime) && StringUtils.isNotEmpty(endTime)) {
            criteria.and("startTime").gte(CommonUtils.conversionDate(startTime, "start")).lte(CommonUtils.conversionDate(endTime, "end"));
        }
        Query query = new Query().addCriteria(criteria);
        long count = mongoTemplate.count(query, BackupSpaceSubtasks.class);
        List<BackupSpaceSubtasks> resultList = new ArrayList<>();
        if (count > 0) {
            query.with(Sort.by(DESC, "startTime"));
            query.with(PageRequest.of(page - 1, size));
            resultList = mongoTemplate.find(query, BackupSpaceSubtasks.class);
        }
        Map<String, Object> resultMap = new HashMap<>(2);
        resultMap.put("count", count);
        resultMap.put("data", resultList);
        return ResultUtil.success(resultMap);
    }

    public ResponseResult<Object> setApplication(String token, Application application) {
        if (null == application) {
            return ResultUtil.errorInternational("GENERAL_PARAMETER_ERROR");
        }
        List<String> validation = validation(application);
        if (validation.size() > 0) {
            return ResultUtil.error(application.toString());
        }
        Token token1 = jwtTokenUtils.getToken(token);
        Person person = new Person(token1);
        if (StringUtils.isNotEmpty(application.getId())) {
            Query query = new Query().addCriteria(Criteria.where("_id").is(application.getId()));
            Application app = mongoTemplate.findOne(query, Application.class);
            if (null == app) {
                return ResultUtil.errorInternational("GENERAL_NOT_EXIST");
            }
            if (app.getState() != 0) {
                return ResultUtil.errorInternational("GENERAL_DISABLE");
            }
            application.setPerson(person);
            application.setCreateTime(app.getCreateTime());
            application.setAppKey(app.getAppKey());
            application.setAppSecret(app.getAppSecret());
            application.setUpdateTime(new Date());
        } else {
            Query query = new Query().addCriteria(Criteria.where("appName").is(application.getAppName()));
            Application one = mongoTemplate.findOne(query, Application.class);
            if (null != one) {
                return ResultUtil.errorInternational("GENERAL_DUPLICATION");
            }
            // Generate appKey
            Long appKey = getAppKey();
            application.setAppKey(appKey);
            application.setPerson(person);
            // Generate appSecret
            String appSecret = encoder.encode(CommonUtils.generateUUID().getBytes(StandardCharsets.UTF_8));
            application.setAppSecret(appSecret);
            application.setCreateTime(new Date());
        }
        mongoTemplate.save(application);
        cacheLoading.upDateApplication(application);
        return ResultUtil.success();
    }

    public ResponseResult<Object> appList(String token, int page, int size, int state, String name, String secret) {
        Criteria criteria = new Criteria();
        if (state != 2) {
            criteria.and("state").is(state);
        }
        if (StringUtils.isNotEmpty(name)) {
            Pattern pattern = Pattern.compile("^.*" + CommonUtils.escapeExprSpecialWord(name.trim()) + ".*$", Pattern.CASE_INSENSITIVE);
            criteria.and("appName").regex(pattern);
        }
        if (StringUtils.isNotEmpty(secret)) {
            Pattern pattern = Pattern.compile("^.*" + CommonUtils.escapeExprSpecialWord(secret.trim()) + ".*$", Pattern.CASE_INSENSITIVE);
            criteria.and("appSecret").regex(pattern);
        }
        Query query = new Query().addCriteria(criteria);
        long count = mongoTemplate.count(query, Application.class);
        List<Application> applicationList = new ArrayList<>(0);
        if (count > 0) {
            query.with(PageRequest.of(page - 1, size));
            query.with(Sort.by(Sort.Order.desc("createTime")));
            applicationList = mongoTemplate.find(query, Application.class);
        }
        Map<String, Object> resultMap = new HashMap<>(2);
        resultMap.put("count", count);
        resultMap.put("data", applicationList);
        return ResultUtil.success(resultMap);
    }

    public ResponseResult<Object> appDelete(String token, String id) {
        if (StringUtils.isNotEmpty(id) && StringUtils.isNotEmpty(id.trim())) {
            Query query = new Query().addCriteria(Criteria.where("_id").is(id));
            Application one = mongoTemplate.findOne(query, Application.class);
            if (one != null) {
                mongoTemplate.remove(query, Application.class);
                CacheLoading cacheLoading = new CacheLoading();
                cacheLoading.delApplication(String.valueOf(one.getAppKey()));
            }
        }
        return ResultUtil.success();
    }

    public ResponseResult<Object> appDis(String token, String id, Boolean dis) {
        if (StringUtils.isNotEmpty(id) && StringUtils.isNotEmpty(id.trim()) && null != dis) {
            Query query = new Query().addCriteria(Criteria.where("_id").is(id));
            Application one = mongoTemplate.findOne(query, Application.class);
            if (dis) {
                one.setState(1);
            } else {
                one.setState(0);
            }
            mongoTemplate.save(one);
            CacheLoading cacheLoading = new CacheLoading();
            cacheLoading.delApplication(String.valueOf(one.getAppKey()));
        }
        return ResultUtil.success();
    }

    /**
     * Statistical Preview - Spatial Statistics
     */
    public ResponseResult<Object> statisticsPreview(String token) {
        Map<String, Object> resultMap = new HashMap<>();
        // space statistics
        long total = mongoTemplate.count(new Query().addCriteria(Criteria.where("state").is("1")), Space.class);
        long pr = mongoTemplate.count(new Query().addCriteria(Criteria.where("state").is("1").and("applyIs").is(0).and("isPublic").ne(1)), Space.class);
        long lim = mongoTemplate.count(new Query().addCriteria(Criteria.where("state").is("1").and("applyIs").is(1).and("isPublic").ne(1)), Space.class);
        long pu = mongoTemplate.count(new Query().addCriteria(Criteria.where("state").is("1").and("isPublic").is(1)), Space.class);
        long pen = mongoTemplate.count(new Query().addCriteria(Criteria.where("state").is("0")), Space.class);
        int num = 0;
        int year = getCurrentYearTo();
        int month = getCurrentMonth();
        SpaceTypeStatistic one = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("year").is(year).and("month").is(month)), SpaceTypeStatistic.class);
        if (null != one) {
            num = one.getLim() + one.getPri() + one.getPub();
        }
        resultMap.put("total", total);
        resultMap.put("private", pr);
        resultMap.put("limited", lim);
        resultMap.put("public", pu);
        resultMap.put("mouthAdd", num);
        resultMap.put("pending", pen);
        return ResultUtil.success(resultMap);
    }

    /**
     * Statistics Preview - User Statistics
     */
    public ResponseResult<Object> statisticsPreviewUser(String token) {
        Map<String, Object> resultMap = new HashMap<>();
        // User statistics
        long total = mongoTemplate.count(new Query().addCriteria(Criteria.where("state").is(1)), ConsumerDO.class);
        long general = mongoTemplate.count(new Query().addCriteria(Criteria.where("state").is(1).and("roles").is(Constants.GENERAL)), ConsumerDO.class);
        long senior = mongoTemplate.count(new Query().addCriteria(Criteria.where("state").is(1).and("roles").is(Constants.SENIOR)), ConsumerDO.class);
        long admin = mongoTemplate.count(new Query().addCriteria(Criteria.where("state").is(1).and("roles").is(Constants.ADMIN)), ConsumerDO.class);
        long wait = mongoTemplate.count(new Query().addCriteria(Criteria.where("state").ne(1)), ConsumerDO.class);
        Criteria criteria = Criteria.where("state").is(1);
        criteria.and("createTime").gte(CommonUtils.conversionDate(getMonthFirstDay(), "start")).lte(CommonUtils.conversionDate(getMonthLastDay(), "end"));
        long mouthAdd = mongoTemplate.count(new Query().addCriteria(criteria), ConsumerDO.class);
        resultMap.put("total", total);
        resultMap.put("general", general);
        resultMap.put("senior", senior);
        resultMap.put("admin", admin);
        resultMap.put("mouthAdd", mouthAdd);
        resultMap.put("wait", wait);
        return ResultUtil.success(resultMap);
    }

    /**
     * Statistical Preview - Data Volume Statistics
     */
    public ResponseResult<Object> statisticsPreviewData(String token) {
        Map<String, Object> resultMap = new HashMap<>();
        // Data usage statistics
        ResponseResult<Object> objectResponseResult = sysDet(token, false);
        if (objectResponseResult.getCode() != 0) {
            return objectResponseResult;
        }
        // Data usage
        Map data = (Map) objectResponseResult.getData();
        // Data authenticity statistics
        Map<String, Object> realMap = new HashMap<>(2);
        Object actual = data.get("actual");
        data.remove("actual");
        data.put("storage", data.get("storage"));
        // Allocated storage for this month
        List<AggregationOperation> aggList = new ArrayList<>(3);
        aggList.add(Aggregation.match(Criteria.where("state").ne(0)));
        aggList.add(Aggregation.match(Criteria.where("createTime").gte(CommonUtils.conversionDate(getMonthFirstDay(), "start")).lte(CommonUtils.conversionDate(getMonthLastDay(), "end"))));
        aggList.add(Aggregation.group().sum("capacity").as("size"));
        Aggregation aggregation = Aggregation.newAggregation(aggList);
        AggregationResults<Document> storageDocument = mongoTemplate.aggregate(aggregation, "space_data_statistic", Document.class);
        long size = 0L;
        for (Document document : storageDocument) {
            size = (Long) document.get("size");
        }
        data.put("monthAllot", size);
        realMap.put("actual", actual);
        // Statistics of new data volume this month
        long space_data_in_statistic = dataInOutMonth("space_data_in_statistic");
        realMap.put("monthAdd", space_data_in_statistic);
        // Inflow statistics
        Map<String, Object> inMap = dataInOutStatistic("space_data_in_statistic", "in");
        inMap.put("monthAdd", space_data_in_statistic);
        // Outflow statistics
        Map<String, Object> outMap = dataInOutStatistic("space_data_out_statistic", "out");
        long space_data_out_statistic = dataInOutMonth("space_data_out_statistic");
        outMap.put("monthAdd", space_data_out_statistic);
        resultMap.put("subsidiary", data);
        resultMap.put("data", realMap);
        resultMap.put("inData", inMap);
        resultMap.put("outData", outMap);
        return ResultUtil.success(resultMap);
    }

    /**
     * Space ranking
     */
    public ResponseResult<Object> statisticsPreviewTop(String token, String sort) {
        Query query = new Query().addCriteria(Criteria.where("state").is(1));
        query.with(Sort.by(DESC, sort));
        query.with(PageRequest.of(0, 10));
        List<SpaceDataStatistic> spaceDataStatistics = mongoTemplate.find(query, SpaceDataStatistic.class);
        return ResultUtil.success(spaceDataStatistics);
    }

    /**
     * Inflow and outflow statistics - all
     */
    private Map<String, Object> dataInOutStatistic(String collect, String type) {
        Map<String, Object> resultMap = new HashMap<>(8);
        long webData = 0L;
        long fairLinkData = 0L;
        long ftpData = 0L;
        long webDavData = 0L;
        long total = 0L;
        long released = 0L;
        long count = mongoTemplate.count(new Query(), collect);
        if (count > 0) {
            List<AggregationOperation> inList = new ArrayList<>(5);
            GroupOperation as = null;
            if (type.equals("out")) {
                as = Aggregation.group().sum("webData").as("webData").sum("fairLinkData").as("fairLinkData").sum("ftpData").as("ftpData").sum("webDavData").as("webDavData").sum("totalData").as("total").sum("releasedData").as("released");
            } else {
                as = Aggregation.group().sum("webData").as("webData").sum("fairLinkData").as("fairLinkData").sum("ftpData").as("ftpData").sum("webDavData").as("webDavData").sum("totalData").as("total");
            }
            inList.add(as);
            Aggregation inAggregation = Aggregation.newAggregation(inList);
            AggregationResults<Document> inStorageDocument = mongoTemplate.aggregate(inAggregation, collect, Document.class);
            for (Document document : inStorageDocument) {
                webData = (Long) document.get("webData");
                fairLinkData = (Long) document.get("fairLinkData");
                ftpData = (Long) document.get("ftpData");
                webDavData = (Long) document.get("webDavData");
                total = (Long) document.get("total");
                if (type.equals("out") && document.containsKey("released")) {
                    released = (Long) document.get("released");
                }
            }
        }
        resultMap.put("total", total);
        resultMap.put("webData", webData);
        resultMap.put("fairLinkData", fairLinkData);
        resultMap.put("ftpData", ftpData);
        resultMap.put("webDavData", webDavData);
        if (type.equals("out")) {
            resultMap.put("releasedData", released);
        }
        return resultMap;
    }

    /**
     * Inflow and outflow statistics - current month
     */
    private long dataInOutMonth(String collect) {
        long totalData = 0L;
        long count = mongoTemplate.count(new Query(), collect);
        if (count > 0) {
            List<AggregationOperation> addList = new ArrayList<>(3);
            addList.add(Aggregation.match(Criteria.where("year").is(CommonUtils.getCurrentYearTo())));
            addList.add(Aggregation.match(Criteria.where("month").is(CommonUtils.getCurrentMonth())));
            addList.add(Aggregation.group().sum("totalData").as("totalData"));
            Aggregation addAggregation = Aggregation.newAggregation(addList);
            AggregationResults<Document> addStorageDocument = mongoTemplate.aggregate(addAggregation, collect, Document.class);
            for (Document document : addStorageDocument) {
                totalData = (Long) document.get("totalData");
            }
        }
        return totalData;
    }

    public ResponseResult<Object> statisticsPreviewGrowth(String token, String startTime, String endTime, int type) {
        int start = 0;
        int end = 0;
        if (StringUtils.isEmpty(startTime) || StringUtils.isEmpty(endTime)) {
            // Convert to half a year
            end = getYearMonth(new Date());
            if (type == 0) {
                start = getYearMonth(getPastMonth(6));
            } else {
                start = getYearMonth(getPastMonth(12));
            }
        } else {
            try {
                start = getYearMonth(getStringDate(startTime));
                end = getYearMonth(getStringDate(endTime));
            } catch (ParseException e) {
                // Convert to half a year
                end = getYearMonth(new Date());
                start = getYearMonth(getPastMonth(6));
            }
        }
        int startYear = Integer.valueOf(String.valueOf(start).substring(0, 4));
        int endYear = Integer.valueOf(String.valueOf(end).substring(0, 4));
        List<Integer> xAxis = getXAxis(start, end, startYear, endYear);
        // Data Query
        Query query = new Query().addCriteria(Criteria.where("sort").gte(start).lte(end));
        List<SpaceTypeStatistic> spaceTypeStatistics = mongoTemplate.find(query, SpaceTypeStatistic.class);
        Map<Integer, Map<String, Integer>> object = new HashMap<>(spaceTypeStatistics.size());
        for (SpaceTypeStatistic spaceTypeStatistic : spaceTypeStatistics) {
            Map<String, Integer> typeMap = new HashMap<>(3);
            typeMap.put("lim", spaceTypeStatistic.getLim());
            typeMap.put("pri", spaceTypeStatistic.getPri());
            typeMap.put("pub", spaceTypeStatistic.getPub());
            object.put(spaceTypeStatistic.getSort(), typeMap);
        }
        List<String> xAxisList = new ArrayList<>(xAxis.size());
        List<Integer> limList = new ArrayList<>(xAxis.size());
        List<Integer> priList = new ArrayList<>(xAxis.size());
        List<Integer> pubList = new ArrayList<>(xAxis.size());
        for (Integer xAxi : xAxis) {
            String year = String.valueOf(xAxi).substring(0, 4);
            String month = String.valueOf(xAxi).substring(4);
            xAxisList.add(year + "年-" + month + "月");
            if (object.containsKey(xAxi)) {
                Map<String, Integer> stringIntegerMap = object.get(xAxi);
                limList.add(stringIntegerMap.get("lim"));
                priList.add(stringIntegerMap.get("pri"));
                pubList.add(stringIntegerMap.get("pub"));
            } else {
                limList.add(0);
                priList.add(0);
                pubList.add(0);
            }
        }
        Map<String, Object> resultMap = new HashMap<>(2);
        resultMap.put("xAxis", xAxisList);
        List<Map<String, Object>> series = new ArrayList<>(3);
        Map<String, Object> limMap = new HashMap<>(2);
        limMap.put("name", "受限");
        limMap.put("data", limList);
        Map<String, Object> priMap = new HashMap<>(2);
        priMap.put("name", "私有");
        priMap.put("data", priList);
        Map<String, Object> pubMap = new HashMap<>(2);
        pubMap.put("name", "公开");
        pubMap.put("data", pubList);
        series.add(pubMap);
        series.add(limMap);
        series.add(priMap);
        resultMap.put("series", series);
        return ResultUtil.success(resultMap);
    }

    private List<Integer> getXAxis(int start, int end, int startYear, int endYear) {
        Integer current = Integer.valueOf(endYear + "00");
        List<Integer> xAxis = new ArrayList<>(12);
        if (start > current) {
            for (int i = start; i <= end; i++) {
                xAxis.add(i);
            }
        } else {
            Integer currentEnd = Integer.valueOf(startYear + "12");
            Integer currentStart = Integer.valueOf(endYear + "01");
            for (int i = start; i <= currentEnd; i++) {
                xAxis.add(i);
            }
            for (int i = currentStart; i <= end; i++) {
                xAxis.add(i);
            }
        }
        return xAxis;
    }

    private int getYearMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        // Obtain the current year
        int year = calendar.get(Calendar.YEAR);
        // Get Current Month
        int month = calendar.get(Calendar.MONTH) + 1;
        String s = ("" + month).length() == 1 ? "0" + month : "" + month;
        return Integer.valueOf(year + s);
    }

    /**
     * API List Retrieval
     */
    public ResponseResult<Object> apiList(String token, int page, int size, String state, String apiName, String app) {
        Criteria criteria = new Criteria();
        if (!state.equals("all") && (state.equals(Constants.OpenApiState.offline) || state.equals(Constants.OpenApiState.online))) {
            criteria.and("state").is(state);
        } else if (!state.equals("all")) {
            return ResultUtil.errorInternational("GENERAL_PARAMETER_ERROR");
        }
        if (StringUtils.isNotEmpty(apiName)) {
            Pattern pattern = Pattern.compile("^.*" + CommonUtils.escapeExprSpecialWord(apiName.trim()) + ".*$", Pattern.CASE_INSENSITIVE);
            criteria.and("name").regex(pattern);
        }
        if (StringUtils.isNotEmpty(app)) {
            Pattern pattern = Pattern.compile("^.*" + CommonUtils.escapeExprSpecialWord(app.trim()) + ".*$", Pattern.CASE_INSENSITIVE);
            Criteria or = new Criteria();
            or.orOperator(Criteria.where("authApp.appKey").regex(pattern), Criteria.where("authApp.appName").regex(pattern));
            criteria.andOperator(or);
        }
        Query query = new Query().addCriteria(criteria);
        long count = mongoTemplate.count(query, OpenApi.class);
        List<OpenApi> applicationList = new ArrayList<>(0);
        if (count > 0) {
            query.with(PageRequest.of(page - 1, size));
            query.with(Sort.by(Sort.Order.desc("publicTime")));
            applicationList = mongoTemplate.find(query, OpenApi.class);
        }
        Map<String, Object> resultMap = new HashMap<>(2);
        resultMap.put("count", count);
        resultMap.put("data", applicationList);
        return ResultUtil.success(resultMap);
    }

    /**
     * Configure API authorization information
     */
    public ResponseResult<Object> setApiAuth(String token, String apiId, String authType, String authTime, String appId, String appName) {
        Token user = jwtTokenUtils.getToken(token);
        Map<String, Object> paramMap = new HashMap<>(3);
        paramMap.put("apiId", apiId);
        paramMap.put("authType", authType);
        paramMap.put("appId", appId);
        paramMap.put("appName", appName);
        List<String> strings = CommonUtils.validationMap(paramMap);
        if (!strings.isEmpty()) {
            return ResultUtil.error("参数错误：{} " + strings.toString());
        }
        OpenApi openApi = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(apiId)), OpenApi.class);
        if (null == openApi) {
            return ResultUtil.errorInternational("GENERAL_NOT_EXIST");
        }
        List<ApiAuth> authApp = openApi.getAuthApp();
        authApp = null == authApp ? new ArrayList<>(1) : authApp;
        boolean add = true;
        for (ApiAuth apiAuth : authApp) {
            if (apiAuth.getAppKey().equals(appId)) {
                apiAuth.setAuthType(authType);
                apiAuth.setAuthTime(authTime);
                apiAuth.setExpire(false);
                apiAuth.setAppName(appName);
                apiAuth.setAuthorizer(user.getName());
                add = false;
            }
        }
        if (add) {
            ApiAuth apiAuth = new ApiAuth();
            apiAuth.setAppKey(appId);
            apiAuth.setAppName(appName);
            apiAuth.setAuthType(authType);
            apiAuth.setAuthTime(authTime);
            apiAuth.setExpire(false);
            apiAuth.setAuthorizer(user.getName());
            authApp.add(apiAuth);
        }
        openApi.setAuthApp(authApp);
        openApi.setAuthNum(authApp.size());
        mongoTemplate.save(openApi);
        // Clear cache
        CacheLoading cacheLoading = new CacheLoading();
        cacheLoading.clearAppAuthPath(appId);
        return ResultUtil.success();
    }

    /**
     * Dropdown Box Data
     */
    public ResponseResult<Object> appSimple(String token, String app) {
        Criteria criteria = new Criteria();
        // Enable
        criteria.and("state").is(0);
        if (StringUtils.isNotEmpty(app)) {
            Pattern pattern = Pattern.compile("^.*" + app.trim() + ".*$", Pattern.CASE_INSENSITIVE);
            criteria.and("appName").regex(pattern);
        }
        Query query = new Query().addCriteria(criteria);
        List<Application> applicationList = mongoTemplate.find(query, Application.class);
        List<Map<String, Object>> resultList = new ArrayList<>(applicationList.size());
        for (Application application : applicationList) {
            Map<String, Object> map = new HashMap<>(2);
            map.put("appId", application.getAppKey());
            map.put("appName", application.getAppName());
            resultList.add(map);
        }
        return ResultUtil.success(resultList);
    }

    /**
     * OpenApi online and offline
     */
    public ResponseResult<Object> setApiState(String token, String apiId, String state) {
        if (StringUtils.isEmpty(apiId) || StringUtils.isEmpty(apiId.trim())) {
            return ResultUtil.errorInternational("GENERAL_PARAMETER_ERROR");
        }
        if (StringUtils.isEmpty(state) || StringUtils.isEmpty(state.trim())) {
            return ResultUtil.errorInternational("GENERAL_PARAMETER_ERROR");
        }
        if (!state.equals(Constants.OpenApiState.online) && !state.equals(Constants.OpenApiState.offline)) {
            return ResultUtil.errorInternational("GENERAL_PARAMETER_ERROR");
        }
        Query query = new Query().addCriteria(Criteria.where("_id").is(apiId));
        OpenApi one = mongoTemplate.findOne(query, OpenApi.class);
        if (null == one) {
            return ResultUtil.errorInternational("GENERAL_NOT_EXIST");
        }
        if (one.getState().equals(state)) {
            return ResultUtil.success();
        }
        one.setState(state);
        mongoTemplate.save(one);
        if (state.equals(Constants.OpenApiState.offline)) {
            List<ApiAuth> authApp = one.getAuthApp();
            CacheLoading cacheLoading = new CacheLoading();
            for (ApiAuth apiAuth : authApp) {
                // Clear all involved application caches
                cacheLoading.clearAppAuthPath(apiAuth.getAppKey());
            }
        }
        return ResultUtil.success();
    }

    /**
     * Installed market components
     */
    public ResponseResult<Object> installList(String token, Integer page, Integer size, String category, String name) {
        Criteria criteria = new Criteria();
        if (StringUtils.isNotEmpty(category) && StringUtils.isNotEmpty(category.trim())) {
            criteria.and("category").is(category.trim());
        }
        if (StringUtils.isNotEmpty(name) && StringUtils.isNotEmpty(name.trim())) {
            Pattern pattern = Pattern.compile("^.*" + CommonUtils.escapeExprSpecialWord(name.trim()) + ".*$", Pattern.CASE_INSENSITIVE);
            criteria.and("name").regex(pattern);
        }
        long count = mongoTemplate.count(new Query().addCriteria(criteria), Component.class);
        List<ComponentShow> componentList = null;
        if (count > 0) {
            Query query = new Query().addCriteria(criteria);
            query.with(PageRequest.of(page - 1, size));
            query.with(Sort.by(DESC, "installTime"));
            componentList = mongoTemplate.find(query, ComponentShow.class, "component");
            // Return parameters to map
            for (ComponentShow componentShow : componentList) {
                List<Map<String, Object>> parameters = componentShow.getParameters();
                Map<String, Object> parametersMap = new HashMap<>();
                if (parameters != null) {
                    for (Map<String, Object> parameter : parameters) {
                        Object key = parameter.get("key");
                        if (key != null) {
                            parametersMap.put(key.toString(), parameter.get("value"));
                        }
                    }
                }
                componentShow.setParameterMap(parametersMap);
                componentShow.setParameters(null);
            }
        }
        HashMap<Object, Object> result = new HashMap<>(2);
        result.put("total", count);
        result.put("data", componentList);
        return ResultUtil.success(result);
    }

    /**
     * Market Component Interface
     */
    public ResponseResult<Object> component(String token, Integer page, Integer size, Integer sort, String category, String name) {
        HttpClient httpClient = new HttpClient();
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("page", String.valueOf(page)));
        params.add(new BasicNameValuePair("pageSize", String.valueOf(size)));
        params.add(new BasicNameValuePair("sort", String.valueOf(sort)));
        if (StringUtils.isNotEmpty(category)) {
            params.add(new BasicNameValuePair("category", category));
        }
        if (StringUtils.isNotEmpty(name)) {
            params.add(new BasicNameValuePair("name", name));
        }
        params.add(new BasicNameValuePair("software", "DataSpace"));
        Map<String, String> header = new HashMap<>(1);
        header.put("accessKey", "8c6daabc-7253-11ed-96b5-305a3ac88b53");
        String fairManMarketUrl = spaceUrl.getFairManMarketUrl();
        String result = "";
        try {
            result = httpClient.doGetWayTwo(params, fairManMarketUrl, header);
        } catch (Exception e) {
            return ResultUtil.error("获取市场组件时发生错误，请稍后重试!");
        }
        Map map = JSONObject.parseObject(result, Map.class);
        if (!map.containsKey("code") || ((int) map.get("code")) != 0) {
            return ResultUtil.error("获取市场组件数据无法解析，请稍后重试!");
        }
        Map<String, Object> dataMap = (Map<String, Object>) map.get("data");
        List<Map<String, Object>> list = (List<Map<String, Object>>) dataMap.get("list");
        for (Map<String, Object> m : list) {
            String downloadId = m.get("downloadId").toString();
            String bundle = m.get("bundle").toString();
            // Verify if it has been installed
            boolean exists = mongoTemplate.exists(new Query().addCriteria(Criteria.where("componentId").is(downloadId).and("bundle").is(bundle)), Component.class);
            m.put("isInstall", exists);
            // Change the parameter List type of component parameters to directly return a Map
            List<Map<String, Object>> parameters = (List<Map<String, Object>>) m.get("parameters");
            Map<String, Object> parametersMap = new HashMap<>();
            if (parameters != null) {
                parameters.stream().forEach(var -> {
                    Object key = var.get("key");
                    if (key != null) {
                        parametersMap.put(key.toString(), var.get("value"));
                    }
                });
            }
            m.put("parameters", parametersMap);
        }
        dataMap.put("list", list);
        return ResultUtil.success(dataMap);
    }

    public ResponseResult<Object> aggData(String token) {
        HttpClient httpClient = new HttpClient();
        Map<String, String> header = new HashMap<>(1);
        header.put("accessKey", "8c6daabc-7253-11ed-96b5-305a3ac88b53");
        String fairManMarketUrl = spaceUrl.getFairManMarketUrl() + "/source?softwareName=DataSpace";
        String result = "";
        try {
            result = httpClient.doGet(fairManMarketUrl, header);
        } catch (Exception e) {
            return ResultUtil.error("获取市场组件聚合数据时发生错误，请稍后重试!");
        }
        Map map = JSONObject.parseObject(result, Map.class);
        if (!map.containsKey("code") || ((int) map.get("code")) != 0) {
            return ResultUtil.error("获取市场组件聚合数据无法解析，请稍后重试!");
        }
        Map<String, Object> dataMap = (Map<String, Object>) map.get("data");
        return ResultUtil.success(dataMap);
    }

    /**
     * Install market components
     */
    public ResponseResult<Object> componentInstall(String token, Component component) {
        List<String> validation = validation(component);
        if (!validation.isEmpty()) {
            return ResultUtil.error("参数错误: {} " + validation.toString());
        }
        String bucket = component.getComponentId();
        String bundle = component.getBundle();
        // Verify if it has been installed
        boolean exists = mongoTemplate.exists(new Query().addCriteria(Criteria.where("bundle").is(bundle)), Component.class);
        if (exists) {
            return ResultUtil.error("组件已安装！");
        }
        // if(!key.contains(".zip")){
        // Return ResultUtil. error ("The component type is not recognized and cannot be installed!");
        // }
        String fairManMarketUrl = spaceUrl.getFairManMarketUrl() + "/download";
        fairManMarketUrl = fairManMarketUrl + "?id=" + bucket + "&bundle=" + bundle;
        String installComSource = spaceUrl.getInstallComSource();
        File sourceFile = new File(installComSource, bucket);
        if (!sourceFile.exists()) {
            sourceFile.mkdirs();
        }
        boolean res = installDownloadFile(fairManMarketUrl, sourceFile.getPath());
        if (!res) {
            return ResultUtil.error("安装失败，请重新安装!");
        }
        File[] files = sourceFile.listFiles();
        if (files.length == 0) {
            return ResultUtil.error("安装失败，请重新安装!");
        }
        // File decompression
        try {
            ZipStream.unzip(files[0].getPath(), sourceFile.getPath());
        } catch (Exception e) {
            return ResultUtil.error("组件解压失败，无法安装!");
        }
        String fileName = "";
        for (File file2 : sourceFile.listFiles()) {
            if (file2.isDirectory()) {
                fileName = file2.getName();
                break;
            }
        }
        // Installation Data Record
        List<Map<String, Object>> parameters = component.getParameters();
        List<String> fileTypes = new ArrayList<>();
        if (null != parameters) {
            for (Map<String, Object> parameter : parameters) {
                if (parameter.containsValue("suffix")) {
                    String value = parameter.get("value").toString();
                    String[] split = value.contains(",") ? value.split(",") : value.split("，");
                    fileTypes.addAll(Arrays.asList(split));
                }
            }
        }
        component.setFileTypes(fileTypes);
        component.setSourcePath(sourceFile.getPath());
        component.setWebPath(spaceUrl.getInstallComWeb() + "/" + bucket + "/" + fileName);
        component.setInstallTime(new Date());
        mongoTemplate.insert(component);
        return ResultUtil.success();
    }

    public ResponseResult<Object> componentEdit(String token, ComponentUpdate component) {
        Update update = new Update();
        update.set("parameters", component.getParameters());
        mongoTemplate.upsert(new Query().addCriteria(Criteria.where("_id").is(component.getId())), update, Component.class);
        return ResultUtil.success();
    }

    private boolean installDownloadFile(String remoteFileUrl, String localPath) {
        CloseableHttpResponse response = null;
        InputStream input = null;
        FileOutputStream output = null;
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            HttpGet httpget = new HttpGet(remoteFileUrl);
            httpget.setHeader("accessKey", "");
            response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                return false;
            }
            String fileName = "";
            Header[] headers = response.getHeaders("Content-Disposition");
            for (Header header : headers) {
                String value = header.getValue();
                if (value.contains("filename")) {
                    fileName = value.substring(value.indexOf("=") + 1);
                }
            }
            if (StringUtils.isEmpty(fileName)) {
                return false;
            }
            File localFile = new File(localPath, fileName);
            input = entity.getContent();
            output = new FileOutputStream(localFile);
            // Create handling tools
            byte[] datas = new byte[1024];
            int len = 0;
            while ((len = input.read(datas)) != -1) {
                // Loop reading data
                output.write(datas, 0, len);
            }
            output.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            // Turn off low-level flow.
            if (input != null) {
                try {
                    input.close();
                } catch (Exception e) {
                }
            }
            if (output != null) {
                try {
                    output.close();
                } catch (Exception e) {
                }
            }
            if (response != null) {
                try {
                    response.close();
                } catch (Exception e) {
                }
            }
            if (httpclient != null) {
                try {
                    httpclient.close();
                } catch (Exception e) {
                }
            }
        }
    }

    public ResponseResult<Object> componentRemove(String token, String id) {
        Component component = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(id)), Component.class);
        if (null == component) {
            return ResultUtil.success();
        }
        String sourcePath = component.getSourcePath();
        // String webPath = component.getWebPath();
        // String webUrl = spaceUrl.getInstallComSource()+webPath.replaceAll(spaceUrl.getInstallComWeb(),"");
        // remove file
        try {
            // Files.delete(new File(sourcePath).toPath());
            File file = new File(sourcePath);
            if (file.isDirectory()) {
                Files.walk(file.toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            } else {
                file.delete();
            }
        } catch (IOException io) {
            return ResultUtil.error("操作失败，请稍后重试");
        }
        mongoTemplate.remove(component);
        return ResultUtil.success();
    }

    /**
     * Obtain space default permission configuration
     */
    public ResponseResult<Object> defaultSpaceRole(String token) {
        Map<String, Object> resultMap = new HashMap<>(2);
        List<SpaceMenu> spaceMenuList = mongoTemplate.findAll(SpaceMenu.class);
        SystemConf systemConf = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("type").is(Constants.CaffeType.SYS_DEF_SPACE_ROLE)), SystemConf.class);
        if (null == systemConf) {
            resultMap.put("普通", spaceMenuList);
            resultMap.put("高级", spaceMenuList);
        } else {
            Object conf = systemConf.getConf();
            Map<String, List<String>> spaceRole = JSONObject.parseObject(JSONObject.toJSONString(conf), Map.class);
            for (String key : spaceRole.keySet()) {
                List<String> menus = spaceRole.get(key);
                List<SpaceMenu> spaceMenus = new ArrayList<>(spaceMenuList.size());
                spaceMenus.addAll(spaceMenuList);
                if (null != menus && !menus.isEmpty()) {
                    for (SpaceMenu spaceMenu : spaceMenus) {
                        List<SpaceMenu.Action> actionList = spaceMenu.getActionList();
                        for (SpaceMenu.Action action : actionList) {
                            for (SpaceMenu.Role role : action.getRoleList()) {
                                role.setDisable(menus.contains(role.getRoleKey()));
                            }
                        }
                    }
                }
                resultMap.put(key, JSONObject.parseObject(JSONObject.toJSONString(spaceMenuList), List.class));
            }
        }
        return ResultUtil.success(resultMap);
    }

    /**
     * Configure space default permission configuration
     */
    public ResponseResult<Object> setSpaceRole(String token, List<SpaceRoleRequest> spaceRoleRequests) {
        List<String> validation = validation(spaceRoleRequests);
        if (!validation.isEmpty()) {
            return ResultUtil.error("参数错误: {} " + validation.toString());
        }
        Map<String, Object> setMap = new HashMap<>(2);
        for (SpaceRoleRequest spaceRoleRequest : spaceRoleRequests) {
            List<String> roles = spaceRoleRequest.getRoles();
            String roleName = spaceRoleRequest.getRoleName();
            if (null == roles) {
                roles = new ArrayList<>(0);
            } else {
                if (roles.size() > 30) {
                    return ResultUtil.errorInternational("GENERAL_PARAMETER_ERROR");
                }
            }
            setMap.put(roleName, roles);
        }
        // change
        synchronized (this) {
            SystemConf systemConf = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("type").is(Constants.CaffeType.SYS_DEF_SPACE_ROLE)), SystemConf.class);
            if (null == systemConf) {
                systemConf = new SystemConf();
                systemConf.setType(Constants.CaffeType.SYS_DEF_SPACE_ROLE);
                systemConf.setCreateTime(new Date());
                systemConf.setLastUpdateTime(new Date());
            }
            systemConf.setConf(setMap);
            mongoTemplate.save(systemConf);
        }
        return ResultUtil.success();
    }

    public ResponseResult<Object> getSpaceRatio(String spaceId) {
        Map<String, Long> result = new HashMap<>(2);
        SpaceDataStatistic statistic = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("spaceId").is(spaceId)), SpaceDataStatistic.class);
        result.put("capacity", statistic.getCapacity());
        result.put("dataSize", statistic.getDataSize());
        return ResultUtil.success(result);
    }
}
