package cn.cnic.dataspace.api.service.space;

import cn.cnic.dataspace.api.asynchronous.AsyncDeal;
import cn.cnic.dataspace.api.cacheLoading.CacheLoading;
import cn.cnic.dataspace.api.config.CreateMongoIndex;
import cn.cnic.dataspace.api.config.FileProperties;
import cn.cnic.dataspace.api.config.VersionUpdate;
import cn.cnic.dataspace.api.config.space.*;
import cn.cnic.dataspace.api.datax.admin.tool.sql.CommonDBUtils;
import cn.cnic.dataspace.api.datax.admin.tool.sql.JdbcConnectionFactory;
import cn.cnic.dataspace.api.datax.admin.tool.sql.SqlUtils;
import cn.cnic.dataspace.api.elfinder.core.Target;
import cn.cnic.dataspace.api.elfinder.service.ElfinderStorage;
import cn.cnic.dataspace.api.elfinder.service.ElfinderStorageService;
import cn.cnic.dataspace.api.exception.CommonException;
import cn.cnic.dataspace.api.model.apply.Apply;
import cn.cnic.dataspace.api.model.apply.SpaceApply;
import cn.cnic.dataspace.api.model.file.FileMapping;
import cn.cnic.dataspace.api.model.manage.ApproveSetting;
import cn.cnic.dataspace.api.model.manage.Component;
import cn.cnic.dataspace.api.model.manage.ComponentShow;
import cn.cnic.dataspace.api.model.release.RequestLn;
import cn.cnic.dataspace.api.model.space.SpaceSimple;
import cn.cnic.dataspace.api.model.manage.SystemConf;
import cn.cnic.dataspace.api.model.backup.BackupSpaceMain;
import cn.cnic.dataspace.api.model.backup.FtpHost;
import cn.cnic.dataspace.api.model.backup.RequestAdd;
import cn.cnic.dataspace.api.model.space.*;
import cn.cnic.dataspace.api.model.space.child.AuthorizationPerson;
import cn.cnic.dataspace.api.model.space.child.Operator;
import cn.cnic.dataspace.api.model.space.child.Person;
import cn.cnic.dataspace.api.model.space.child.SimpleSpace;
import cn.cnic.dataspace.api.model.email.EmailRole;
import cn.cnic.dataspace.api.model.harvest.MiningTask;
import cn.cnic.dataspace.api.model.release.ResourceShow;
import cn.cnic.dataspace.api.model.release.ResourceV2;
import cn.cnic.dataspace.api.model.statistics.SpaceDataStatistic;
import cn.cnic.dataspace.api.model.user.ConsumerDO;
import cn.cnic.dataspace.api.quartz.MyJob;
import cn.cnic.dataspace.api.quartz.QuartzManager;
import cn.cnic.dataspace.api.queue.SpaceQuery;
import cn.cnic.dataspace.api.quartz.BackupUtils;
import cn.cnic.dataspace.api.queue.space.SpaceTaskUtils;
import cn.cnic.dataspace.api.repository.*;
import cn.cnic.dataspace.api.service.SendEmailService;
import cn.cnic.dataspace.api.service.WOPIService;
import cn.cnic.dataspace.api.service.impl.ReleaseServiceImpl;
import cn.cnic.dataspace.api.util.*;
import cn.cnic.dataspace.api.websocket.WebSocketProcess;
import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import static cn.cnic.dataspace.api.model.apply.Apply.*;
import static cn.cnic.dataspace.api.model.user.Message.*;
import static cn.cnic.dataspace.api.service.impl.WOPIServiceImpl.ACTION_URLS;
import static cn.cnic.dataspace.api.util.CommonUtils.*;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;

/**
 * SpaceService
 *
 * @author wangCc
 * @date 2021-03-23 10:15
 */
@Slf4j
@Service
public class SpaceService {

    static final Long SIZE_1G = (long) 1024 * 1024 * 1024;

    public static final Map<String, Object> SPACE_AUDIT = new HashMap<>();

    /**
     * Spatial role
     */
    public static final String SPACE_OWNER = "拥有者";

    public static final String SPACE_SENIOR = "高级";

    public static final String SPACE_GENERAL = "普通";

    public static final Map<String, AtomicInteger> limitMap = new ConcurrentHashMap<>(256);

    private final Cache<String, String> privateLink = CaffeineUtil.getPrivateLink();

    private final Cache<String, Object> spaceStatistic = CaffeineUtil.getSpaceStatistic();

    private final Cache<String, String> tokenCache = CaffeineUtil.getTokenCache();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileProperties fileProperties;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private SendEmailService sendEmailService;

    @Autowired
    private JwtTokenUtils jwtTokenUtils;

    @Autowired
    private ElfinderStorageService elfinderStorageService;

    @Autowired
    private SpaceUrl spaceUrl;

    @Autowired
    private MsgUtil msgUtil;

    @Autowired
    private AsyncMethod asyncMethod;

    @Autowired
    private ManagementService managementService;

    @Autowired
    private ApproveSettingRepository approveSettingRepository;

    @Autowired
    private ApplyRepository applyRepository;

    @Autowired
    private SettingService settingService;

    @Resource
    private WOPIService wopiService;

    @Lazy
    @Autowired
    private MessageService messageService;

    @Lazy
    @Autowired
    private AsyncDeal asyncDeal;

    @Lazy
    @Autowired
    private SpaceControlConfig spaceControlConfig;

    @Lazy
    @Autowired
    private FileMappingManage fileMappingManage;

    @Lazy
    @Autowired
    private VersionUpdate versionUpdate;

    @Autowired
    private WebSocketProcess webSocketProcess;

    @Lazy
    @Autowired
    private CacheLoading cacheLoading;

    @Lazy
    @Autowired
    private CreateMongoIndex createMongoIndex;

    /**
     * get create space audit
     * 0:not
     * 1:need
     */
    private String createSpaceAudit() {
        Object audit = SPACE_AUDIT.get("audit");
        if (Objects.isNull(audit)) {
            String approved = approveSettingRepository.findAll().get(0).getApproved();
            SPACE_AUDIT.put("audit", approved);
            return approved;
        } else {
            return audit.toString();
        }
    }

    /**
     * default space size
     */
    public Long spaceSizeFromApproveSetting() {
        Object size = SPACE_AUDIT.get("spaceSize");
        if (Objects.isNull(size)) {
            long gb = approveSettingRepository.findAll().get(0).getGb();
            SPACE_AUDIT.put("spaceSize", gb);
            return gb;
        } else {
            if (StringUtils.isNumeric(size.toString())) {
                return Long.parseLong(size.toString());
            } else {
                final long gb = approveSettingRepository.findAll().get(0).getGb();
                SPACE_AUDIT.put("spaceSize", gb);
                return gb;
            }
        }
    }

    /**
     * space view count
     */
    public ResponseResult<Object> viewCount(String spaceId) {
        // SpaceStatistic spaceStatistic = spaceStatisticConfig.getSpaceStatistic(spaceId);
        // List<Statistic> statistics = mongoTemplate.find(new Query().addCriteria(new Criteria().orOperator(
        // Criteria.where("spaceId").is(spaceId), Criteria.where("homeUrl").is(spaceId)).and("type").is(Statistic.TYPE_VIEW)), Statistic.class);
        // return ResultUtil.success(statistics.size() > 0 ? ((int) statistics.get(0).getCount()) + 1 : 0);
        return ResultUtil.success(0);
    }

    /**
     * space view count
     */
    public ResponseResult<Object> downloadCount(String spaceId) {
        // List<Statistic> statistics = mongoTemplate.find(new Query().addCriteria(new Criteria().orOperator(
        // Criteria.where("spaceId").is(spaceId), Criteria.where("homeUrl").is(spaceId)).and("type").is(Statistic.TYPE_DOWNLOAD)), Statistic.class);
        // return ResultUtil.success(statistics.size() > 0 ? statistics.get(0).getCount() * 2 : 0);
        // SpaceStatistic spaceStatistic = spaceStatisticConfig.getSpaceStatistic(spaceId);
        return ResultUtil.success(0);
    }

    public ResponseResult<Object> transit(String code) {
        Criteria criteria = new Criteria();
        criteria.orOperator(Criteria.where("_id").is(code), Criteria.where("homeUrl").is(code));
        Query query = new Query().addCriteria(criteria);
        Space one = mongoTemplate.findOne(query, Space.class);
        if (null == one) {
            return ResultUtil.errorInternational("GENERAL_NOT_EXIST");
        }
        return ResultUtil.success(one.getSpaceId());
    }

    /**
     * space usage size
     */
    public ResponseResult<Long> usageSize(String token, String spaceId) {
        if (StringUtils.isBlank(token)) {
            return ResultUtil.errorInternational("NEED_TOKEN");
        }
        Token userToken = jwtTokenUtils.getToken(token);
        if (!userToken.getRoles().contains(Constants.ADMIN)) {
            spaceControlConfig.spatialVerification(spaceId, userToken.getEmailAccounts(), Constants.SpaceRole.LEVEL_OTHER);
        }
        SpaceDataStatistic spaceDataStatistic = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("spaceId").is(spaceId)), SpaceDataStatistic.class);
        if (null == spaceDataStatistic) {
            return ResultUtil.success(0l);
        }
        return ResultUtil.success(spaceDataStatistic.getDataSize());
    }

    /**
     * space detail info
     */
    @View
    public ResponseResult<SimpleSpace> detail(String token, String spaceId) {
        Token userToken = jwtTokenUtils.getToken(token);
        spaceControlConfig.spatialVerification(spaceId, userToken.getEmailAccounts(), Constants.SpaceRole.LEVEL_OTHER);
        SimpleSpace simpleSpace = new SimpleSpace();
        final Space space = mongoTemplate.findOne(new Query().addCriteria(new Criteria().orOperator(Criteria.where("spaceId").is(spaceId), Criteria.where("homeUrl").is(spaceId))), Space.class);
        BeanUtils.copyProperties(space, simpleSpace);
        simpleSpace.setSpaceSize(space.getSpaceSize());
        for (AuthorizationPerson authorizationPerson : simpleSpace.getAuthorizationList()) {
            if (StringUtils.equals(authorizationPerson.getUserId(), userToken.getUserId())) {
                simpleSpace.setSpaceRole(authorizationPerson.getRole());
                break;
            }
        }
        simpleSpace.setAuthorizationList(new HashSet<>());
        if (StringUtils.isEmpty(simpleSpace.getFileRole())) {
            simpleSpace.setFileRole(Constants.SpaceRole.ALL);
        }
        spaceControlConfig.spaceStat(spaceId, "viewCount", 1L);
        return ResultUtil.success(simpleSpace);
    }

    /**
     * space list created and admin by myself
     */
    public ResponseResult<Map<String, Object>> spaceList(String token, String spaceName, String pageOffset, String pageSize, String order) {
        String userId = jwtTokenUtils.getUserIdFromToken(token);
        return ResultUtil.success(spaceResultList(pageOffset, pageSize, order, userId, new Query().addCriteria(Criteria.where("authorizationList.userId").is(userId).and("spaceName").regex(compile("^.*" + spaceName + ".*$", CASE_INSENSITIVE)).and("state").ne("2"))));
    }

    /**
     * public space list
     */
    public ResponseResult<Map<String, Object>> publicSpaceList(String token, String pageOffset, String pageSize, String order) {
        String userIdFromToken = jwtTokenUtils.getUserIdFromToken(token);
        Criteria criteria = new Criteria();
        // Exclude the remaining public and restricted access spaces from the spaces I participate in
        criteria.orOperator(Criteria.where("isPublic").is(1), Criteria.where("applyIs").is(1));
        Map<String, Object> objectMap = spaceResultList(pageOffset, pageSize, order, null, new Query().addCriteria(Criteria.where("state").is("1").and("authorizationList.userId").ne(userIdFromToken)).addCriteria(criteria));
        List<String> spaceIds = cacheLoading.getUserApplySpaces(userIdFromToken);
        List<Space> content = (List<Space>) objectMap.get("content");
        for (Space space : content) {
            if (space.getApplyIs() == 1 || space.getIsPublic() == 1) {
                // if(judgeMember(space.getAuthorizationList(),userIdFromToken)){
                // space.setHaveApply(2);
                // }else {
                // 
                // }
                space.setHaveApply((spaceIds.contains(space.getSpaceId()) ? 1 : 0));
            }
        }
        objectMap.put("content", content);
        return ResultUtil.success(objectMap);
    }

    /**
     * space query result list
     */
    private Map<String, Object> spaceResultList(String pageOffset, String pageSize, String order, String userId, Query query) {
        Map<String, Object> content = new HashMap<>(16);
        content.put("count", mongoTemplate.count(query, Space.class));
        List<Space> simpleSpaces = mongoTemplate.find(query.with(PageRequest.of(Integer.parseInt(pageOffset), Integer.parseInt(pageSize))).with(Sort.by(StringUtils.equals(order, "desc") ? DESC : ASC, "createDateTime")), Space.class);
        for (Space simpleSpace : simpleSpaces) {
            Optional<ConsumerDO> byId = userRepository.findById(simpleSpace.getUserId());
            if (byId.isPresent()) {
                ConsumerDO consumerDO = byId.get();
                simpleSpace.setUserName(consumerDO.getName());
                simpleSpace.setUserAvatar(consumerDO.getAvatar());
            } else {
                simpleSpace.setUserName(messageInternational("OWNER_NOT_FOUND"));
            }
            simpleSpace.setMemberCount(simpleSpace.getAuthorizationList().size());
            simpleSpace.setDataSetCount((int) mongoTemplate.count(new Query().addCriteria(Criteria.where("spaceId").is(simpleSpace.getSpaceId()).and("type").is(Constants.PUBLISHED)), ResourceV2.class));
            simpleSpace.setFilePath(null);
            simpleSpace.setOwned(StringUtils.isNotBlank(userId) ? StringUtils.equals(simpleSpace.getUserId(), userId) ? "1" : "0" : "0");
            // simpleSpace.setAuthorizationList(new HashSet<>());
            // final List<Statistic> statistics = mongoTemplate.find(new Query(Criteria.where("type").is("download").orOperator(Criteria.where("spaceId").
            // is(simpleSpace.getSpaceId()), Criteria.where("spaceId").is(simpleSpace.getHomeUrl()))), Statistic.class);
            // simpleSpace.setDownload(statistics.size() > 0 ? statistics.get(0).getCount() * 2 : 0d);
        }
        content.put("content", simpleSpaces);
        return content;
    }

    /**
     * account initialization
     */
    private String userRootDir() {
        String userRootDir = fileProperties.getRootDir() + FILE_SPLIT;
        File file = new File(fileProperties.getRootDir());
        if (!file.exists()) {
            file.mkdir();
        }
        return userRootDir;
    }

    /**
     * user authentication check
     */
    boolean authenticCheck(String token, String spaceId) {
        String userId = jwtTokenUtils.getUserIdFromToken(token);
        return !(StringUtils.isNotBlank(token) && (isSpaceSeniorAdmin(spaceId, userId)));
    }

    /**
     * create space
     * institutional email suffix does not need to be verified
     */
    public ResponseResult<Object> createSpace(String token, Space space) {
        Token user = jwtTokenUtils.getToken(token);
        if (Objects.isNull(user)) {
            return ResultUtil.errorInternational("NEED_TOKEN");
        }
        String userId = user.getUserId();
        String email = user.getEmailAccounts();
        String spaceId = generateSnowflake();
        String spaceName = space.getSpaceName().trim();
        // param check
        if (StringUtils.isBlank(spaceName)) {
            return ResultUtil.errorInternational("SPACE_NAME_NULL");
        }
        if (isSpecialChar(spaceName)) {
            return ResultUtil.error(messageInternational("SPACE_NAME_CHAR") + takeOutChar(spaceName));
        }
        if ((spaceName.length()) > 100) {
            return ResultUtil.errorInternational("SPACE_NAME_LENGTH");
        }
        if (null == space.getTags()) {
            space.setTags(new ArrayList<>(0));
        }
        if ((space.getDescription().length()) > 300) {
            return ResultUtil.errorInternational("SPACE_DESC_LENGTH");
        }
        // Space Unique Identification
        String homeUrl = space.getHomeUrl();
        if (StringUtils.isBlank(homeUrl)) {
            return ResultUtil.errorInternational("SPACE_HOME_ADDR");
        }
        if (isSpecialChar(homeUrl)) {
            return ResultUtil.error(messageInternational("SPACE_HOME_CHAR") + " " + takeOutChar(homeUrl));
        }
        if (!homeUrl.matches("^[a-z0-9A-Z]*")) {
            return ResultUtil.errorInternational("SPACE_ADDR_COMBINATION");
        }
        // Space type judgment
        int applyIs = space.getApplyIs();
        if (applyIs != 0 && applyIs != 1) {
            return ResultUtil.errorInternational("PERMISSION_DENIED");
        }
        // create space limit
        if (limitMap.containsKey(email)) {
            AtomicInteger count = limitMap.get(email);
            if (count.get() >= 50) {
                return ResultUtil.errorInternational("SPACE_CREATE_LIMIT");
            }
            limitMap.put(email, new AtomicInteger(count.incrementAndGet()));
        } else {
            limitMap.put(email, new AtomicInteger(1));
        }
        space.setSpaceId(spaceId);
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
            return ResultUtil.errorInternational("create_db_failed");
        } finally {
            CommonDBUtils.closeDBResources(connection);
        }
        // create space
        space.setDbName(dbName);
        // 0 pending approval 1 normal 2 offline
        String spaceState = "1";
        ConsumerDO consumerDO = null;
        synchronized (this) {
            if (urlCheck(spaceId, homeUrl)) {
                return ResultUtil.errorInternational("SPACE_ADDR_COMBINATION");
            }
            consumerDO = userRepository.findById(userId).get();
            if (spaceRepository.findByUserIdAndSpaceName(userId, spaceName) != null) {
                return ResultUtil.errorInternational("SPACE_EXISTED");
            }
            // system admin could create space directly without checking
            // senior whether need audit depending on approve setting
            if (!user.getRoles().contains(Constants.ADMIN)) {
                if (StringUtils.equals(createSpaceAudit(), ApproveSetting.NEED_APPROVED)) {
                    spaceState = "0";
                }
            }
            createSpaceImpl(space, consumerDO, spaceState);
        }
        // create svn repository

        if ("0".equals(spaceState)) {
            String msgContent = messageInternational("SPACE_USER") + consumerDO.getName() + "（" + email + messageInternational("SPACE_OPEN") + spaceName + messageInternational("NEED_APPROVE");
            // message record
            messageService.sendToAdmin(TITLE_PENDING, msgContent, consumerDO, spaceUrl.getApplyUrl());
            // space system admin need not approve and send email
            // apply and approve record
            String content = messageInternational("SPACE_USER") + consumerDO.getName() + "（" + email + messageInternational("SPACE_OPEN") + spaceName + "）";
            applyRepository.save(Apply.builder().applyId(generateSnowflake()).applicant(new Person(consumerDO)).content(content).submitDate(getCurrentDateTimeString()).type(TYPE_SPACE_APPLY).approvedStates(APPROVED_NOT).spaceId(spaceId).spaceName(spaceName).spaceDescription(space.getDescription()).spaceLogo("").spaceTag(space.getTags()).description(content).build());
            // websocket msg notify
            Map<String, Object> map = new HashMap<>();
            map.put("title", messageInternational("PENDING_APPROVAL_REMINDER"));
            map.put("content", msgContent);
            msgUtil.sendAdminMsg(msgUtil.mapToString(map));
            if (!user.getRoles().contains(Constants.ADMIN)) {
                try {
                    // send email to all admin
                    // TODO tentative email sending failure does not affect space creation
                    settingService.sendAdminEmail(messageInternational("SPACE_APPLICATION"), spaceName, consumerDO);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            // Add spatial database mapping index
            createMongoIndex.createSpaceFileMappingIndex(spaceId);
        }
        return ResultUtil.success(spaceId);
    }

    /**
     * create a new space
     */
    public String createSpaceImpl(Space space, ConsumerDO consumerDO, String spaceState) {
        String userId = consumerDO.getId();
        String spaceShort = spaceControlConfig.getSpaceShort();
        String filePath = userRootDir() + spaceShort;
        File filePathFile = new File(filePath);
        filePathFile.mkdirs();
        space.setFilePath(filePath);
        space.setSpaceShort(spaceShort);
        space.setCreateDateTime(getCurrentDateTimeString());
        space.setUserId(userId);
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
        Integer isPublic = space.getIsPublic();
        if (isPublic != null && isPublic == 1) {
            space.setApplyIs(1);
        } else {
            space.setIsPublic(0);
        }
        if (null == space.getSpaceSize() || space.getSpaceSize() == 0) {
            space.setSpaceSize(spaceSizeFromApproveSetting() * SIZE_1G);
        } else {
            space.setSpaceSize(space.getSpaceSize() * SIZE_1G);
        }
        space.setTopic("light");
        space.setState(spaceState);
        Set<AuthorizationPerson> set = new HashSet<>();
        set.add(AuthorizationPerson.builder().userId(userId).userName(consumerDO.getName()).email(consumerDO.getEmailAccounts()).role(SPACE_OWNER).build());
        space.setAuthorizationList(set);
        space.setFileRole(Constants.SpaceRole.ALL);
        spaceRepository.save(space);
        SpaceDataStatistic spaceDataStatistic = new SpaceDataStatistic();
        spaceDataStatistic.setSpaceId(space.getSpaceId());
        spaceDataStatistic.setSpaceName(space.getSpaceName());
        spaceDataStatistic.setState(Integer.valueOf(space.getState()));
        spaceDataStatistic.setCapacity(space.getSpaceSize());
        spaceDataStatistic.setDataSize(0L);
        spaceDataStatistic.setCreateTime(new Date());
        mongoTemplate.insert(spaceDataStatistic);
        // Initialize the number of members
        spaceControlConfig.spaceStat(space.getSpaceId(), "memberCount", 1L);
        // log record
        spaceControlConfig.spaceLogSave(space.getSpaceId(), "创建空间", userId, new Operator(consumerDO), SpaceSvnLog.ACTION_VERSION);
        // Sync permissions
        SystemConf systemConf = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("type").is(Constants.CaffeType.SYS_DEF_SPACE_ROLE)), SystemConf.class);
        if (null != systemConf) {
            Object conf = systemConf.getConf();
            Map<String, List<String>> spaceRole = JSONObject.parseObject(JSONObject.toJSONString(conf), Map.class);
            for (String key : spaceRole.keySet()) {
                List<String> menus = spaceRole.get(key);
                SpaceRole spaceUserRole = new SpaceRole();
                spaceUserRole.setSpaceId(space.getSpaceId());
                spaceUserRole.setRoleName(key);
                spaceUserRole.setMenus(menus);
                spaceUserRole.setCreateTime(new Date());
                spaceUserRole.setUpdateTime(new Date());
                mongoTemplate.insert(spaceUserRole);
            }
        }
        if (space.getState().equals("1")) {
            // Space type statistics
            versionUpdate.setSpaceType(space);
            SpaceSizeControl.addSpace(space.getSpaceId(), space.getSpaceSize(), 0L);
        }
        // Add ftp space identifier
        updateFtpAuth(consumerDO.getEmailAccounts(), space.getSpaceShort(), space.getSpaceId(), "add");
        return filePath;
    }

    /**
     * space update
     * parameter --> middle
     * old --> parameter
     * middle --> parameter
     */
    public ResponseResult<Object> updateSpace(String token, Space space) {
        Token userToken = jwtTokenUtils.getToken(token);
        String spaceId = space.getSpaceId();
        // Permission verification
        String email = userToken.getEmailAccounts();
        spaceControlConfig.spatialVerification(spaceId, email, Constants.SpaceRole.LEVEL_OTHER);
        spaceControlConfig.validateSpacePermissions(email, spaceId, SpaceRoleEnum.S_CONF_INFO.getRole());
        Space spaceMiddle = new Space();
        // parameter --> middle
        BeanUtils.copyProperties(space, spaceMiddle);
        Space spaceOriginalEntity = spaceRepository.findById(spaceId).get();
        // old --> parameter
        BeanUtils.copyProperties(spaceOriginalEntity, space);
        // old
        Integer publicOriginal = spaceOriginalEntity.getIsPublic();
        String descriptionOriginal = spaceOriginalEntity.getDescription();
        String homeUrlOriginal = spaceOriginalEntity.getHomeUrl();
        List<String> tagsOriginal = spaceOriginalEntity.getTags();
        String spaceOriginal = spaceOriginalEntity.getSpaceName();
        String topicOriginal = spaceOriginalEntity.getTopic();
        String logoOriginal = spaceOriginalEntity.getSpaceLogo();
        String markdownOriginal = spaceOriginalEntity.getMarkdown();
        // new
        String description = spaceMiddle.getDescription();
        List<String> tags = spaceMiddle.getTags();
        Integer isPublic = spaceMiddle.getIsPublic();
        String spaceName = spaceMiddle.getSpaceName();
        String homeUrl = spaceMiddle.getHomeUrl();
        String topic = spaceMiddle.getTopic();
        String logo = spaceMiddle.getSpaceLogo();
        String markdown = spaceMiddle.getMarkdown();
        String fileRole = spaceMiddle.getFileRole();
        Integer applyIs = spaceMiddle.getApplyIs();
        String updateName = null;
        String updateDes = null;
        List<String> updateTags = null;
        if (StringUtils.isBlank(spaceName)) {
            return ResultUtil.errorInternational("SPACE_NAME_NULL");
        }
        if (spaceName.length() > 100) {
            return ResultUtil.errorInternational("SPACE_NAME_LENGTH");
        }
        if (isSpecialChar(spaceName)) {
            return ResultUtil.error(messageInternational("SPACE_NAME_CHAR") + takeOutChar(spaceName));
        }
        if (StringUtils.isNoneBlank(space.getDescription()) && space.getDescription().length() > 300) {
            return ResultUtil.errorInternational("SPACE_DESC_LENGTH");
        }
        if (StringUtils.isNoneBlank(space.getMarkdown()) && space.getMarkdown().length() > 300) {
            return ResultUtil.errorInternational("SPACE_INSTRUCTION_LENGTH");
        }
        // middle --> parameter
        ConsumerDO consumerDO = userRepository.findById(userToken.getUserId()).get();
        boolean flag = false;
        boolean buckUpdate = false;
        String content = consumerDO.getName() + " " + messageInternational("SPACE_MODIFY_INFO");
        if (StringUtils.isNotBlank(description) && !description.equals(descriptionOriginal)) {
            space.setDescription(description);
            updateDes = description;
            content += messageInternational("SPACE_DESCRIPTION") + " ";
            flag = true;
        }
        if ((!Objects.isNull(tags)) && (!Objects.equals(tags, tagsOriginal))) {
            space.setTags(tags);
            updateTags = tags;
            content += messageInternational("SPACE_TAGS") + " ";
            flag = true;
        }
        // update public authorization
        if (!Objects.isNull(isPublic) && (!isPublic.equals(publicOriginal))) {
            space.setIsPublic(isPublic);
            content += messageInternational("SPACE_PUBLIC") + " ";
            flag = true;
        }
        if (StringUtils.isNotBlank(spaceName) && (!spaceName.equals(spaceOriginal))) {
            space.setSpaceName(spaceName);
            updateName = spaceName;
            content += messageInternational("SPACE_NAME") + " ";
            flag = true;
            buckUpdate = true;
        }
        if (StringUtils.isNotBlank(homeUrl) && (!StringUtils.equals(homeUrlOriginal, homeUrl))) {
            if (urlCheck(spaceId, homeUrl)) {
                return ResultUtil.errorInternational("SPACE_REPEAT");
            }
            if (isSpecialChar(homeUrl)) {
                return ResultUtil.error(messageInternational("SPACE_HOME_CHAR") + " " + takeOutChar(homeUrl));
            }
            if (!homeUrl.matches("^[a-z0-9A-Z]*")) {
                return ResultUtil.errorInternational("SPACE_ADDR_COMBINATION");
            }
            space.setHomeUrl(homeUrl);
            content += messageInternational("SPACE_HOME") + " ";
            flag = true;
        }
        if (!Objects.isNull(topic) && (!topic.equals(topicOriginal))) {
            space.setTopic(topic);
            content += messageInternational("SPACE_TOPIC") + " ";
            flag = true;
        }
        if (!Objects.isNull(logo) && (!logo.equals(logoOriginal))) {
            String head = String.valueOf(new Date().getTime());
            String imagePath = "/" + Constants.Image.image + "/" + Constants.Image.SPACE + "/" + space.getSpaceId() + "_" + head + ".jpg";
            try {
                CommonUtils.generateImage(logo, spaceUrl.getRootDir() + imagePath);
                if (StringUtils.isNotEmpty(spaceOriginalEntity.getSpaceLogo())) {
                    Files.delete(new File(spaceUrl.getRootDir(), spaceOriginalEntity.getSpaceLogo()).toPath());
                }
                space.setSpaceLogo(imagePath);
            } catch (Exception e) {
                space.setSpaceLogo(logo);
            }
            content += messageInternational("SPACE_LOGO") + " ";
            flag = true;
        }
        if (!Objects.isNull(markdown) && (!markdown.equals(markdownOriginal))) {
            space.setMarkdown(markdown);
            content += messageInternational("SPACE_INTRODUCTION") + " ";
            flag = true;
        }
        if (StringUtils.isNotEmpty(fileRole)) {
            if (!fileRole.equals(Constants.SpaceRole.ALL) && !fileRole.equals(Constants.SpaceRole.MANAGE)) {
                return ResultUtil.errorInternational("PERMISSION_DENIED");
            }
            space.setFileRole(fileRole);
            content += messageInternational("SPACE_FILE_ROLE") + " ";
            flag = true;
        } else {
            space.setFileRole(Constants.SpaceRole.ALL);
        }
        if (null != applyIs) {
            if (applyIs != 0 && applyIs != 1) {
                return ResultUtil.errorInternational("PERMISSION_DENIED");
            }
            space.setApplyIs(applyIs);
            content += messageInternational("SPACE_APPLY") + " ";
            flag = true;
        }
        space.setUpdateDateTime(getCurrentDateTimeString());
        spaceRepository.save(space);
        if (flag) {
            spaceControlConfig.spaceLogSave(spaceId, content, userToken.getUserId(), new Operator(consumerDO), SpaceSvnLog.ACTION_OTHER);
        }
        if (buckUpdate) {
            // Sync Space Easy Information
            cacheLoading.clearSimple(spaceId);
        }
        asyncDeal.spaceInfoUpdate(spaceId, updateName, updateDes, updateTags);
        return ResultUtil.successMsg("ok");
    }

    /**
     * public home url check
     * true: used
     * false: not used
     */
    public boolean urlCheck(String spaceId, String url) {
        // exclude itself if there is a spaceId
        long spaces = mongoTemplate.count(new Query().addCriteria(Criteria.where("homeUrl").is(url)), Space.class);
        if (spaces == 0) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * publish new version authentication check
     */
    public ResponseResult<Object> publish(String token, String spaceId) {
        String email = jwtTokenUtils.getEmail(token);
        spaceControlConfig.spatialVerification(spaceId, email, Constants.SpaceRole.LEVEL_SENIOR);
        return ResultUtil.success();
    }

    /**
     * delete space
     */
    @SneakyThrows
    public ResponseResult<Object> deleteSpace(String token, String spaceId) {
        Token userToken = jwtTokenUtils.getToken(token);
        if (!userToken.getRoles().contains(Constants.ADMIN)) {
            spaceControlConfig.spatialVerification(spaceId, userToken.getEmailAccounts(), Constants.SpaceRole.LEVEL_ADMIN);
        }
        // if (!(isOwner(spaceId, userId) || managementService.isSystemAdmin(userId))) {
        // return ResultUtil.errorInternational("PERMISSION_DENIED");
        // }
        Space space = spaceRepository.findById(spaceId).get();
        asyncMethod.deleteSpace(space, spaceUrl);
        // Delete space backup task
        asyncDeal.deleteBackupTask(spaceId, false);
        // Clear space cache
        cacheLoading.clearSpaceCaffeine(space.getSpaceShort(), space.getSpaceShort());
        return ResultUtil.success();
    }

    /**
     * Users are divided into registered, activated, and new users
     */
    public ResponseResult<Object> spaceInvite(String token, String spaceId, String userId, String role) {
        String userIdFromToken = jwtTokenUtils.getUserIdFromToken(token);
        // Permission verification
        String email = jwtTokenUtils.getEmail(token);
        spaceControlConfig.spatialVerification(spaceId, email, Constants.SpaceRole.LEVEL_OTHER);
        spaceControlConfig.validateSpacePermissions(email, spaceId, SpaceRoleEnum.M_ADD.getRole());
        if (!role.equals(SPACE_SENIOR) && !role.equals(SPACE_GENERAL)) {
            return ResultUtil.errorInternational("PERMISSION_DENIED");
        }
        // Invited
        Optional<ConsumerDO> user = userRepository.findById(userId);
        // Not empty and in an activated state -->add directly
        if (user.isPresent() && user.get().getState() == 1) {
            ConsumerDO consumerDO = user.get();
            AuthorizationPerson authorizationPerson = new AuthorizationPerson(consumerDO);
            authorizationPerson.setRole(role);
            Space space = spaceRepository.findById(spaceId).get();
            Set<AuthorizationPerson> authorizationList = space.getAuthorizationList();
            if (authorizationList.add(authorizationPerson)) {
                String operatorId = jwtTokenUtils.getUserIdFromToken(token);
                ConsumerDO operator = userRepository.findById(operatorId).get();
                space.setAuthorizationList(authorizationList);
                spaceRepository.save(space);
                spaceControlConfig.spaceStat(spaceId, "memberCount", 1L);
                // log record
                String des = operator.getName() + " " + messageInternational("USER_INVITE") + " " + consumerDO.getName() + " " + messageInternational("ENTER_SPACE").substring(1);
                spaceControlConfig.spaceLogSave(spaceId, des, userIdFromToken, new Operator(operator), SpaceSvnLog.ACTION_MEMBER);
                // websocket notify
                Map<String, Object> msgMap = new HashMap<>(16);
                String content = operator.getName() + "（" + operator.getEmailAccounts() + messageInternational("SPACE_INVITED_ENTERED") + space.getSpaceName() + "）";
                msgMap.put("title", messageInternational("SPACE_INVITED"));
                msgMap.put("content", content);
                msgUtil.sendMsg(consumerDO.getEmailAccounts(), msgUtil.mapToString(msgMap));
                // message record
                String url = spaceUrl.getSpaceDetailUrl().replaceAll("spaceId", spaceId);
                String webUrl = spaceUrl.getWebUrl();
                String linkUrl = url.replaceAll(webUrl, "");
                if (!linkUrl.substring(0, 1).equals("/")) {
                    linkUrl = "/" + linkUrl;
                }
                messageService.sendToApplicant(TITLE_INVITED, content, new Person(operator), new Person(consumerDO), linkUrl);
                cacheLoading.updateSpaceAuth(spaceId, userId);
                updateFtpAuth(consumerDO.getEmailAccounts(), space.getSpaceShort(), space.getSpaceId(), "add");
            } else {
                return ResultUtil.errorInternational("USER_ADDED");
            }
            // email
            // sendEmailService.sendInviteEmail(token, userId, spaceId,role);
            return ResultUtil.successMsg("ok");
        } else if (user.isPresent() && user.get().getState() == 0) {
            // Status is not activated - send an email
            sendEmailService.sendInviteEmail(token, userId, spaceId, role);
            return ResultUtil.successMsg(messageInternational("USER_STATE_UNACTIVATED"));
        } else {
            // new user
            sendEmailService.sendInviteEmail(token, userId, spaceId, role);
            return ResultUtil.successMsg(messageInternational("USER_UNREGISTERED"));
        }
    }

    /**
     * space invited by email and joined in space as general member
     */
    public ResponseResult<Object> spaceInviteByEmail(String spaceId, String userId, String sourUserId, String spaceRole) {
        ConsumerDO consumerDO = userRepository.findById(userId).get();
        ConsumerDO operator = userRepository.findById(sourUserId).get();
        Space space = spaceRepository.findById(spaceId).get();
        AuthorizationPerson person = new AuthorizationPerson(consumerDO);
        if (StringUtils.isNotBlank(spaceRole)) {
            person.setRole(spaceRole);
        }
        if (space.getAuthorizationList().add(person)) {
            spaceRepository.save(space);
            spaceControlConfig.spaceStat(spaceId, "memberCount", 1L);
            String des = operator.getName() + " " + messageInternational("USER_INVITE") + " " + consumerDO.getName() + " " + messageInternational("ENTER_SPACE").substring(1);
            spaceControlConfig.spaceLogSave(spaceId, des, sourUserId, new Operator(operator), SpaceSvnLog.ACTION_MEMBER);
            new CacheLoading().updateSpaceAuth(spaceId, userId);
            updateFtpAuth(consumerDO.getEmailAccounts(), space.getSpaceShort(), space.getSpaceId(), "add");
            return ResultUtil.successMsg("ok");
        } else {
            return ResultUtil.errorInternational("USER_ADDED");
        }
    }

    /**
     * List of users invited by the space
     */
    public ResponseResult<List<Map<String, String>>> userList(String spaceId, String pageOffset, String pageSize) {
        Space space = spaceRepository.findById(spaceId).get();
        String owner = space.getUserId();
        Page<ConsumerDO> userPage = userRepository.findAll(PageRequest.of(Integer.parseInt(pageOffset), Integer.parseInt(pageSize)));
        Set<AuthorizationPerson> authorizationList = space.getAuthorizationList();
        List<Map<String, String>> invited = new ArrayList<>();
        Set<String> set = new HashSet<>();
        for (AuthorizationPerson authorizationPerson : authorizationList) {
            set.add(authorizationPerson.getUserId());
        }
        for (ConsumerDO consumerDO : userPage) {
            Map<String, String> map = new HashMap<>(16);
            String userId = consumerDO.getId();
            map.put("userId", userId);
            map.put("email", consumerDO.getEmailAccounts());
            map.put("userName", consumerDO.getName());
            map.put("states", set.contains(userId) ? messageInternational("USER_JOINED") : consumerDO.getState() == 1 ? messageInternational("USER_INVITE") : consumerDO.getState() == 0 ? messageInternational("USER_ACTIVATED") : messageInternational("USER_UNACTIVATED"));
            if (!StringUtils.equals(owner, userId)) {
                invited.add(map);
            }
        }
        return ResultUtil.success(invited);
    }

    /**
     * space owner
     */
    private boolean isOwner(String spaceId, String userId) {
        return StringUtils.equals(userId, spaceRepository.findById(spaceId).get().getUserId());
    }

    /**
     * data space senior admin including space owner
     * senior admins have many authorities excluding delete space
     */
    public boolean isSpaceSeniorAdmin(String spaceId, String userId) {
        boolean flag = false;
        Optional<Space> spaceOptional = spaceRepository.findById(spaceId);
        if (spaceOptional.isPresent()) {
            Space space = spaceOptional.get();
            for (AuthorizationPerson authorizationPerson : space.getAuthorizationList()) {
                if (StringUtils.equals(authorizationPerson.getUserId(), userId) && StringUtils.equals(authorizationPerson.getRole(), SPACE_SENIOR)) {
                    flag = true;
                    break;
                }
            }
        }
        return flag || isOwner(spaceId, userId);
    }

    /**
     * space general member
     */
    // public boolean isSpaceMember(String spaceId, String operatorId) {
    // boolean flag = false;
    // List<Space> spaceList = mongoTemplate.find(new Query().addCriteria(new Criteria().orOperator(Criteria.where("spaceId").is(spaceId),
    // Criteria.where("homeUrl").is(spaceId))), Space.class);
    // if (spaceList.size() == 0) {
    // throw new CommonException(500, messageInternational("RESOURCE_DOES_NOT_EXIST"));
    // }
    // Space space = spaceList.get(0);
    // if (StringUtils.equals(operatorId, space.getUserId())) {
    // return true;
    // }
    // Set<AuthorizationPerson> authorizationList = space.getAuthorizationList();
    // if (!Objects.isNull(authorizationList)) {
    // for (AuthorizationPerson authorizationPerson : authorizationList) {
    // if (StringUtils.equals(authorizationPerson.getUserId(), operatorId)) {
    // flag = true;
    // break;
    // }
    // }
    // }
    // return flag;
    // }
    /**
     * publish version authorization
     */
    public ResponseResult<Object> publishAuth(String token, String spaceId) {
        String email = jwtTokenUtils.getEmail(token);
        spaceControlConfig.spatialVerification(spaceId, email, Constants.SpaceRole.LEVEL_OTHER);
        return ResultUtil.success();
    }

    /**
     * space member
     */
    public ResponseResult<Set<AuthorizationPerson>> member(String token, String spaceId) {
        String email = jwtTokenUtils.getEmail(token);
        spaceControlConfig.spatialVerification(spaceId, email, Constants.SpaceRole.LEVEL_OTHER);
        final Space space = mongoTemplate.findOne(new Query().addCriteria(new Criteria().orOperator(Criteria.where("spaceId").is(spaceId), Criteria.where("homeUrl").is(spaceId))), Space.class);
        return ResultUtil.success(space.getAuthorizationList());
    }

    /**
     * delete space member
     * cannot delete yourself and higher-level member
     */
    public ResponseResult<Object> deleteSpaceMember(String token, String spaceId, String userId, String role) {
        String operatorId = jwtTokenUtils.getUserIdFromToken(token);
        // Permission verification
        String email = jwtTokenUtils.getEmail(token);
        spaceControlConfig.spatialVerification(spaceId, email, Constants.SpaceRole.LEVEL_OTHER);
        spaceControlConfig.validateSpacePermissions(email, spaceId, SpaceRoleEnum.M_DEL.getRole());
        // cannot delete yourself and higher-level member
        if (StringUtils.equals(operatorId, userId)) {
            return ResultUtil.errorInternational("DEL_ERROR");
        }
        ConsumerDO deletedMember = userRepository.findById(userId).get();
        Space space = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(spaceId)), Space.class);
        if (StringUtils.equals(space.getUserId(), userId)) {
            return ResultUtil.errorInternational("DEL_OWNER_ERROR");
        }
        Set<AuthorizationPerson> authorizationList = space.getAuthorizationList();
        Iterator<AuthorizationPerson> iterator = authorizationList.iterator();
        while (iterator.hasNext()) {
            AuthorizationPerson next = iterator.next();
            if (next.getUserId().equals(userId)) {
                iterator.remove();
                break;
            }
        }
        // authorizationList.remove(new AuthorizationPerson(deletedMember, role));
        space.setAuthorizationList(authorizationList);
        spaceRepository.save(space);
        // Remove number of members
        spaceControlConfig.spaceDeleteUser(spaceId);
        // Remove ftp permissions
        this.updateFtpAuth(deletedMember.getEmailAccounts(), space.getSpaceShort(), spaceId, "delete");
        ConsumerDO operator = userRepository.findById(operatorId).get();
        String des = operator.getName() + " " + messageInternational("MODIFY_MEMBER_REMOVE") + " " + deletedMember.getName();
        spaceControlConfig.spaceLogSave(spaceId, des, operatorId, new Operator(operator), SpaceSvnLog.ACTION_MEMBER);
        cacheLoading.deleteSpaceAuth(spaceId, deletedMember.getId());
        // Clear space user permission cache
        cacheLoading.clearSpaceUserRole(spaceId, deletedMember.getEmailAccounts());
        cacheLoading.clearSpaceMenRole(spaceId);
        return ResultUtil.success();
    }

    /**
     * change space role
     */
    public ResponseResult<Object> updateSpaceRole(String token, String spaceId, String userId, String role) {
        Token user = jwtTokenUtils.getToken(token);
        String operatorId = user.getUserId();
        if (!role.equals(SPACE_OWNER) && !role.equals(SPACE_SENIOR) && !role.equals(SPACE_GENERAL)) {
            return ResultUtil.errorInternational("PERMISSION_DENIED");
        }
        // Permission verification
        spaceControlConfig.spatialVerification(spaceId, user.getEmailAccounts(), Constants.SpaceRole.LEVEL_OTHER);
        spaceControlConfig.validateSpacePermissions(user.getEmailAccounts(), spaceId, SpaceRoleEnum.M_EDIT.getRole());
        Space space = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(spaceId).and("state").is("1")), Space.class);
        if (space == null) {
            return ResultUtil.errorInternational("SPACE_REVIEW");
        }
        if (StringUtils.equals(space.getUserId(), userId)) {
            return ResultUtil.errorInternational("SPACE_OWNER_MODIFY");
        }
        if (StringUtils.equals(operatorId, userId)) {
            return ResultUtil.errorInternational("USER_MODIFY");
        }
        Set<AuthorizationPerson> authorizationList = space.getAuthorizationList();
        String des = "";
        ConsumerDO consumerDO = userRepository.findById(userId).get();
        if (!role.equals(SPACE_OWNER)) {
            for (AuthorizationPerson authorizationPerson : authorizationList) {
                if (StringUtils.equals(authorizationPerson.getUserId(), userId)) {
                    authorizationPerson.setRole(role);
                    break;
                }
            }
            spaceRepository.save(space);
            des = user.getName() + " " + messageInternational("MODIFY_MEMBER") + consumerDO.getName() + messageInternational("MODIFY_MEMBER_ROLE") + " " + role;
        } else {
            if (!operatorId.equals(space.getUserId())) {
                return ResultUtil.errorInternational("PERMISSION_DENIED");
            }
            Set<AuthorizationPerson> authorizationPeople = new LinkedHashSet<>(authorizationList.size());
            AuthorizationPerson personTow = null;
            Iterator<AuthorizationPerson> iterator = authorizationList.iterator();
            while (iterator.hasNext()) {
                if (authorizationPeople.size() > 0 && null != personTow) {
                    break;
                }
                AuthorizationPerson next = iterator.next();
                if (next.getUserId().equals(operatorId)) {
                    next.setRole(SPACE_SENIOR);
                    personTow = next;
                    iterator.remove();
                    continue;
                }
                if (next.getUserId().equals(userId)) {
                    next.setRole(SPACE_OWNER);
                    authorizationPeople.add(next);
                    iterator.remove();
                    continue;
                }
            }
            authorizationPeople.add(personTow);
            authorizationPeople.addAll(authorizationList);
            space.setUserId(userId);
            space.setAuthorizationList(authorizationPeople);
            spaceRepository.save(space);
            des = consumerDO.getName() + "成为空间拥有者";
            // Synchronize update of backup space owners
            AuthorizationPerson person = new AuthorizationPerson(consumerDO);
            asyncDeal.updateBackupTask(person, space.getSpaceId());
            // Clear space user permission cache
            cacheLoading.clearSpaceUserRole(spaceId, user.getEmailAccounts());
        }
        spaceControlConfig.spaceLogSave(spaceId, des, operatorId, new Operator(user), SpaceSvnLog.ACTION_MEMBER);
        // Clear space user permission cache
        cacheLoading.clearSpaceUserRole(spaceId, consumerDO.getEmailAccounts());
        cacheLoading.clearSpaceMenRole(spaceId);
        return ResultUtil.success();
    }

    /**
     * get webDAV url
     */
    public ResponseResult<Object> spaceWeb(String token, String spaceId) {
        // Permission verification
        String email = jwtTokenUtils.getEmail(token);
        spaceControlConfig.spatialVerification(spaceId, email, Constants.SpaceRole.LEVEL_OTHER);
        spaceControlConfig.validateSpacePermissions(email, spaceId, SpaceRoleEnum.F_OTHER_WEBDAV.getRole());
        final Optional<Space> spaceOptional = spaceRepository.findById(spaceId);
        return ResultUtil.success(spaceUrl.getCallHost() + fileProperties.getWebDavPrefix() + FILE_SPLIT + spaceOptional.get().getSpaceShort());
    }

    /**
     * space log
     */
    public ResponseResult<Object> spaceLog(String token, String spaceId, String actionType, String pageOffset, String pageSize, String contentSearch) {
        String email = jwtTokenUtils.getEmail(token);
        spaceControlConfig.spatialVerification(spaceId, email, Constants.SpaceRole.LEVEL_OTHER);
        String spaceUserRole = cacheLoading.getSpaceUserRole(spaceId, email);
        Query query = new Query().addCriteria(Criteria.where("spaceId").is(spaceId));
        if (StringUtils.isNotBlank(actionType)) {
            query.addCriteria(Criteria.where("action").is(actionType));
        }
        if (StringUtils.isNotBlank(contentSearch)) {
            query.addCriteria(Criteria.where("description").regex(compile("^.*" + CommonUtils.escapeExprSpecialWord(contentSearch.trim()) + ".*$", CASE_INSENSITIVE)));
        }
        Map<String, Object> content = new HashMap<>(16);
        content.put("count", mongoTemplate.count(query, SpaceSvnLog.class));
        List<SpaceSvnLog> createTime = mongoTemplate.find(query.with(PageRequest.of(Integer.parseInt(pageOffset), Integer.parseInt(pageSize))).with(Sort.by(DESC, "createTime")), SpaceSvnLog.class);
        for (SpaceSvnLog spaceSvnLog : createTime) {
            spaceSvnLog.setUpdateDateTime(CommonUtils.getDateTimeString(spaceSvnLog.getCreateTime()));
        }
        content.put("content", createTime);
        return ResultUtil.success(spaceUserRole, content);
    }

    /**
     * active members in the space
     */
    public ResponseResult<Object> activeMember(String token, String spaceId) {
        String email = jwtTokenUtils.getEmail(token);
        spaceControlConfig.spatialVerification(spaceId, email, Constants.SpaceRole.LEVEL_OTHER);
        List<SpaceSvnLog> spaceSvnLogList = mongoTemplate.find(new Query().addCriteria(Criteria.where("spaceId").is(spaceId)), SpaceSvnLog.class);
        Map<String, Long> collect = new HashMap<>();
        if (spaceSvnLogList.size() > 0) {
            collect = spaceSvnLogList.stream().collect(Collectors.groupingBy(SpaceSvnLog::getOperatorId, Collectors.counting()));
        }
        List<Map<String, Object>> nodes = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Long> operatorLongEntry : collect.entrySet()) {
            if (i <= 5) {
                Map<String, Object> map = new HashMap<>(16);
                String operatorId = operatorLongEntry.getKey();
                final Optional<ConsumerDO> byId = userRepository.findById(operatorId);
                if (byId.isPresent()) {
                    ConsumerDO consumerDO = byId.get();
                    map.put("id", consumerDO.getId());
                    map.put("name", consumerDO.getName());
                    map.put("email", consumerDO.getEmailAccounts());
                    map.put("avatar", consumerDO.getAvatar());
                    map.put("size", operatorLongEntry.getValue());
                    nodes.add(map);
                    i++;
                }
            } else {
                break;
            }
        }
        return ResultUtil.success(nodes);
    }

    /**
     * recent publish resource
     */
    public ResponseResult<Object> recentPublish(String token, String pageOffset, String pageSize, String releaseName, String spaceId) {
        spaceControlConfig.spatialVerification(spaceId, jwtTokenUtils.getEmail(token), Constants.SpaceRole.LEVEL_OTHER);
        Map<String, Object> resultMap = new HashMap<>(16);
        Query query = new Query().addCriteria(Criteria.where("spaceId").is(spaceId));
        if (StringUtils.isNotEmpty(releaseName)) {
            Pattern pattern = Pattern.compile("^.*" + CommonUtils.escapeExprSpecialWord(releaseName.trim()) + ".*$", Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where("titleCH").regex(pattern));
        }
        long count = mongoTemplate.count(query, ResourceV2.class);
        List<ResourceShow> nodes = new ArrayList<>();
        if (count > 0) {
            query.with(Sort.by(Sort.Order.desc("createTime"))).with(PageRequest.of(Integer.valueOf(pageOffset) - 1, Integer.valueOf(pageSize)));
            nodes = mongoTemplate.find(query, ResourceShow.class, ReleaseServiceImpl.COLLECTION_NAME);
        }
        resultMap.put("total", count);
        resultMap.put("result", nodes);
        return ResultUtil.success(resultMap);
    }

    /**
     * get ftp url
     */
    public ResponseResult<Object> ftpUrl(String token, String spaceId) {
        Token user = jwtTokenUtils.getToken(token);
        // Permission verification
        spaceControlConfig.spatialVerification(spaceId, user.getEmailAccounts(), Constants.SpaceRole.LEVEL_OTHER);
        spaceControlConfig.validateSpacePermissions(user.getEmailAccounts(), spaceId, SpaceRoleEnum.F_OTHER_FTP.getRole());
        final Optional<Space> spaceOptional = spaceRepository.findById(spaceId);
        final Space space = spaceOptional.get();
        String ftpHost = spaceUrl.getFtpHost();
        String port = spaceUrl.getShow();
        if (ftpHost.contains(":")) {
            ftpHost = ftpHost.substring(0, ftpHost.indexOf(":"));
        }
        if (!port.equals("21")) {
            ftpHost = ftpHost + ":" + port;
        }
        String shortChain = space.getSpaceShort();
        return ResultUtil.success("ftp://" + ftpHost + "/" + shortChain);
    }

    /**
     * Cross spatial replication - soft links
     */
    public ResponseResult<Object> cpLn(String token, RequestLn requestLn, HttpServletRequest request) {
        Token user = jwtTokenUtils.getToken(token);
        if (null == user) {
            return ResultUtil.errorInternational("NEED_TOKEN");
        }
        // Parameter verification
        List<String> validation = validation(requestLn);
        if (validation.size() > 0) {
            return ResultUtil.error("error: {}" + validation.toString());
        }
        List<String> hashList = requestLn.getHashList();
        if (hashList == null || hashList.size() == 0) {
            return ResultUtil.success();
        }
        String sourceSpaceId = requestLn.getSourceSpaceId();
        String targetSpaceId = requestLn.getTargetSpaceId();
        // Permission verification
        spaceControlConfig.spatialVerification(sourceSpaceId, user.getEmailAccounts(), Constants.SpaceRole.LEVEL_OTHER);
        spaceControlConfig.spatialVerification(targetSpaceId, user.getEmailAccounts(), Constants.SpaceRole.LEVEL_OTHER);
        spaceControlConfig.validateSpacePermissions(user.getEmailAccounts(), targetSpaceId, SpaceRoleEnum.F_OTHER_IM.getRole());
        Space targetSpace = spaceRepository.findById(targetSpaceId).get();
        Space sourceSpace = spaceRepository.findById(sourceSpaceId).get();
        // Verify space capacity
        if (SpaceSizeControl.validation(targetSpace.getSpaceId())) {
            return ResultUtil.errorInternational("FILE_SIZE_FULL");
        }
        // Path to read files
        ElfinderStorage elfinderStorage = elfinderStorageService.getElfinderStorage(request, targetSpaceId);
        String rootPath = elfinderStorage.fromHash(requestLn.getSpaceHash()).toString();
        ElfinderStorage elfinderStorageSource = elfinderStorageService.getElfinderStorage(request, sourceSpaceId);
        if (requestLn.getType().equals("cp")) {
            List<String> pathList = new ArrayList<>(hashList.size());
            for (String hash : hashList) {
                String sourcePath = elfinderStorageSource.fromHash(hash).toString();
                pathList.add(sourcePath);
            }
            MiningTask miningTask = new MiningTask();
            miningTask.setCreateTime(new Date());
            miningTask.setEmail(user.getEmailAccounts());
            miningTask.setSpaceName("空间导入：从空间【" + sourceSpace.getSpaceName() + "】导入");
            miningTask.setTaskId(CommonUtils.generateUUID());
            miningTask.setSpaceId(targetSpaceId);
            miningTask.setUserId(user.getUserId());
            miningTask.setType(Constants.TaskType.SYS_IMP);
            miningTask.setShowPath("/" + sourceSpace.getSpaceName());
            miningTask.setSourceRootPath(sourceSpace.getFilePath());
            miningTask.setTargetRootPath(rootPath);
            miningTask.setSourcePaths(pathList);
            miningTask.setState(5);
            mongoTemplate.save(miningTask);
            socketMessage(Constants.SocketType.TS_DRAW, miningTask.getTaskId(), miningTask.getEmail());
            Object taskCount = spaceStatistic.getIfPresent("task:" + user.getUserId() + targetSpaceId);
            if (null != taskCount) {
                spaceStatistic.put("task:" + user.getUserId() + targetSpaceId, (long) taskCount + 1);
            }
            // Join the task queue to start processing
            SpaceQuery instance = SpaceQuery.getInstance();
            SpaceTaskUtils spaceTaskUtils = new SpaceTaskUtils(new ArrayList<>(16), webSocketProcess);
            spaceTaskUtils.setRootId(miningTask.getTaskId());
            spaceTaskUtils.setUser(miningTask.getEmail());
            instance.addCache(user.getUserId(), miningTask, mongoTemplate, spaceTaskUtils);
            // PushTheFile. copyNeo (pathList, spaceUrl. getReleaseStored(), rootPath, userId, user. getName())// Asynchronous copy
        }
        // else if (requestLn.getType().equals("ln")) {
        // for (String hash : hashList) {
        // String sourcePath = elfinderStorage.fromHash(hash).toString();
        // if (StringUtils.isNotEmpty(sourcePath) && sourcePath.contains("/")) {
        // ln(sourcePath, rootPath, error);
        // } else {
        // Error. add ("Source file not found, copy failed!");
        // }
        // }
        // }
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

    public ResponseResult<Object> previewUrl(String spaceId, String homeUrl, String hash, HttpServletRequest request) {
        if (StringUtils.isEmpty(hash.trim())) {
            return ResultUtil.errorInternational("CHOSE_FILE");
        }
        String id = "";
        if (StringUtils.isNotEmpty(spaceId) && !spaceId.trim().equals("")) {
            id = spaceId;
        } else if (StringUtils.isNotEmpty(homeUrl) && !homeUrl.trim().equals("")) {
            Query query = new Query().addCriteria(Criteria.where("homeUrl").is(homeUrl).and("state").is("1"));
            Space one = mongoTemplate.findOne(query, Space.class);
            if (one == null) {
                return ResultUtil.errorInternational("RESOURCE_DOES_NOT_EXIST");
            }
            id = one.getSpaceId();
        } else {
            return ResultUtil.errorInternational("SPACE_FILE");
        }
        ElfinderStorage elfinderStorage = elfinderStorageService.getElfinderStorage(request, id);
        Target target = elfinderStorage.fromHash(hash);
        String path = target.toString();
        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            return ResultUtil.errorInternational("FILE_NOT_FOUND");
        }
        String name = file.getName();
        privateLink.put(hash + "_" + name, path);
        return ResultUtil.success(spaceUrl.getCallHost() + "/api/dwn/" + hash + "_" + name);
    }

    /**
     * Soft connection
     */
    private void ln(String source, String target, List<String> error) {
        Path sourcePath = null;
        try {
            sourcePath = FileSystems.getDefault().getPath(source);
            Path targetPath = FileSystems.getDefault().getPath(target + "[其他空间->软链接]" + sourcePath.getFileName());
            Files.createSymbolicLink(targetPath, sourcePath);
        } catch (IOException ioException) {
            ioException.printStackTrace();
            error.add(sourcePath.getFileName().toString() + " " + messageInternational("REPLICATION_FAILED"));
        }
    }

    // Change ftp user permissions
    // private void updateFtpAuth(String email, String userId, String spaceId, String type,String spacePath) {
    // Map<String, String> authoritiesCache = CaffeineUtil.getShortChain(email);
    // if (authoritiesCache != null) {
    // if (type.equals("add")) {
    // authoritiesCache.put(userId, spaceId);
    // authoritiesCache.put(userId+"-spacePath",spacePath);
    // } else if (type.equals("delete")) {
    // Query query = new Query().addCriteria(Criteria.where("userId").is(userId).and("spaceId").is(spaceId));
    // FTPShort one = mongoTemplate.findOne(query, FTPShort.class);
    // if(null != one) {
    // String shortChain = one.getShortChain();
    // authoritiesCache.remove(shortChain);
    // authoritiesCache.remove(shortChain+"-spacePath");
    // }
    // } else {
    // return;
    // }
    // CaffeineUtil.setShortChain(email, authoritiesCache);
    // }
    // }
    private void updateFtpAuth(String email, String shortChain, String spaceId, String type) {
        Map<String, String> authoritiesCache = CaffeineUtil.getShortChain(email);
        if (authoritiesCache != null) {
            if (type.equals("add")) {
                authoritiesCache.put(shortChain, spaceId);
            } else if (type.equals("delete")) {
                authoritiesCache.remove(shortChain);
            } else {
                return;
            }
            CaffeineUtil.setShortChain(email, authoritiesCache);
        }
    }

    public ResponseResult<Object> importSpaceList(String token, String spaceId) {
        String userIdFromToken = jwtTokenUtils.getUserIdFromToken(token);
        if (StringUtils.isEmpty(spaceId.trim())) {
            return ResultUtil.errorInternational("PARAMETER_ERROR");
        }
        // Permission verification
        String email = jwtTokenUtils.getEmail(token);
        spaceControlConfig.spatialVerification(spaceId, email, Constants.SpaceRole.LEVEL_OTHER);
        spaceControlConfig.validateSpacePermissions(email, spaceId, SpaceRoleEnum.F_OTHER_IM.getRole());
        Query query = new Query().addCriteria(Criteria.where("userId").is(userIdFromToken).and("state").is("1"));
        List<Space> spaces = mongoTemplate.find(query, Space.class);
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (Space space : spaces) {
            Map<String, Object> map = new HashMap<>(16);
            if (space.getSpaceId().equals(spaceId)) {
                continue;
            }
            map.put("spaceId", space.getSpaceId());
            map.put("spaceLogo", space.getSpaceLogo());
            map.put("spaceName", space.getSpaceName());
            resultList.add(map);
        }
        return ResultUtil.success(resultList);
    }

    /**
     * Space application information acquisition
     */
    public ResponseResult<Object> applyInfo(String token, String spaceId) {
        String userId = jwtTokenUtils.getUserIdFromToken(token);
        ConsumerDO consumerDO = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(userId)), ConsumerDO.class);
        if (null == consumerDO) {
            return ResultUtil.errorInternational("AUTH_USER_NOT_FOUND");
        }
        Criteria criteria = new Criteria();
        criteria.orOperator(Criteria.where("_id").is(spaceId), Criteria.where("homeUrl").is(spaceId));
        Query spaceQuery = new Query().addCriteria(Criteria.where("state").is("1")).addCriteria(criteria);
        Space space = mongoTemplate.findOne(spaceQuery, Space.class);
        if (null == space) {
            return ResultUtil.errorInternational("SPACE_APPLY_INFO");
        }
        if (space.getIsPublic() != 1 && space.getApplyIs() != 1) {
            return ResultUtil.errorInternational("SPACE_APPLY_INFO");
        }
        Map<String, Object> resultMap = new HashMap<>(3);
        resultMap.put("spaceName", space.getSpaceName());
        resultMap.put("spaceId", space.getSpaceId());
        resultMap.put("username", consumerDO.getName());
        resultMap.put("email", consumerDO.getEmailAccounts());
        resultMap.put("work", consumerDO.getOrgChineseName());
        Set<AuthorizationPerson> authorizationList = space.getAuthorizationList();
        List<AuthorizationPerson> personList = new ArrayList<>(authorizationList.size());
        for (AuthorizationPerson authorizationPerson : authorizationList) {
            if (authorizationPerson.getUserId().equals(userId)) {
                return ResultUtil.errorInternational("SPACE_APPLY_DIST");
            }
            if (authorizationPerson.getUserId().equals(space.getUserId()) || authorizationPerson.getRole().equals(SPACE_SENIOR)) {
                personList.add(authorizationPerson);
            }
        }
        resultMap.put("audit", personList);
        return ResultUtil.success(resultMap);
    }

    public ResponseResult<Object> applyJoin(String token, String spaceId, String reason) {
        Token user = jwtTokenUtils.getToken(token);
        Criteria criteria = new Criteria();
        criteria.orOperator(Criteria.where("_id").is(spaceId), Criteria.where("homeUrl").is(spaceId));
        Query spaceQuery = new Query().addCriteria(Criteria.where("state").is("1")).addCriteria(criteria);
        Space space = mongoTemplate.findOne(spaceQuery, Space.class);
        if (null == space) {
            return ResultUtil.errorInternational("SPACE_APPLY_INFO");
        }
        if (space.getIsPublic() != 1 && space.getApplyIs() != 1) {
            return ResultUtil.errorInternational("SPACE_APPLY_INFO");
        }
        ConsumerDO userDo = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(user.getUserId())), ConsumerDO.class);
        SpaceApply spaceApply = new SpaceApply();
        String applyId = generateSnowflake();
        spaceApply.setSpaceId(spaceId);
        spaceApply.setApplyId(applyId);
        spaceApply.setUserId(user.getUserId());
        spaceApply.setReason(reason);
        AuthorizationPerson authorizationPerson1 = new AuthorizationPerson(user);
        authorizationPerson1.setRole(user.getRoles().get(0));
        spaceApply.setApplicant(authorizationPerson1);
        spaceApply.setWork(userDo.getOrgChineseName());
        spaceApply.setTitle(user.getName() + "（" + user.getEmailAccounts() + "）申请加入空间");
        spaceApply.setCreateTime(new Date());
        mongoTemplate.insert(spaceApply);
        cacheLoading.upUserApplySpaces(user.getUserId(), spaceId);
        // Create user application records
        String content = messageInternational("SPACE_USER") + userDo.getName() + "（" + userDo.getEmailAccounts() + messageInternational("SPACE_JOIN") + space.getSpaceName() + "）";
        applyRepository.save(Apply.builder().applyId(applyId).applicant(new Person(userDo)).content(content).submitDate(getCurrentDateTimeString()).type(TYPE_SPACE_JOIN).approvedStates(APPROVED_NOT).spaceId(spaceId).spaceName(space.getSpaceName()).spaceDescription("").spaceLogo("").spaceTag(null).description(reason).build());
        // Email sending
        ArrayList<String> list = new ArrayList<>(8);
        ArrayList<Person> personArrayList = new ArrayList<>(8);
        for (AuthorizationPerson authorizationPerson : space.getAuthorizationList()) {
            if (space.getUserId().equals(authorizationPerson.getUserId()) || authorizationPerson.getRole().equals(SPACE_SENIOR)) {
                list.add(authorizationPerson.getEmail() + "~" + authorizationPerson.getUserName());
                Person person = new Person();
                person.setPersonId(authorizationPerson.getUserId());
                person.setPersonName(authorizationPerson.getUserName());
                person.setEmail(authorizationPerson.getEmail());
                person.setAvatar(authorizationPerson.getAvatar());
                personArrayList.add(person);
            }
        }
        EmailModel space_apply = EmailModel.SPACE_APPLY();
        String message = space_apply.getMessage().replaceAll("username", user.getName()).replaceAll("spaceName", space.getSpaceName()).replaceAll("email", user.getEmailAccounts());
        space_apply.setMessage(message);
        // send message
        String url = spaceUrl.getSpaceApplyUrl().replaceAll("spaceId", spaceId);
        String webUrl = spaceUrl.getWebUrl();
        String linkUrl = url.replaceAll(webUrl, "");
        if (!linkUrl.substring(0, 1).equals("/")) {
            linkUrl = "/" + linkUrl;
        }
        Map<String, Object> msgMap = new HashMap<>(2);
        msgMap.put("title", TITLE_APPLY);
        String emailAccounts = user.getEmailAccounts();
        String name = user.getName();
        String msgTitle = name + "（" + emailAccounts + "）申请加入空间（" + space.getSpaceName() + "）";
        msgMap.put("content", msgTitle);
        for (String info : list) {
            String[] split = info.split("~");
            Map<String, Object> param = new HashMap<>(16);
            param.put("name", split[1]);
            param.put("url", url);
            param.put("email", split[0]);
            asyncDeal.send(param, space_apply, EmailRole.SPACE_JOIN_APPLY);
            msgUtil.sendMsg(split[0], msgUtil.mapToString(msgMap));
            messageService.sendToApplicant(TITLE_PENDING, msgTitle, personArrayList.get(list.indexOf(info)), 1, linkUrl);
        }
        return ResultUtil.success();
    }

    public ResponseResult<Object> applyList(String token, String spaceId, Integer state, Integer page, Integer size, String applicant, String content) {
        // Permission verification
        String email = jwtTokenUtils.getEmail(token);
        spaceControlConfig.spatialVerification(spaceId, email, Constants.SpaceRole.LEVEL_OTHER);
        spaceControlConfig.validateSpacePermissions(email, spaceId, SpaceRoleEnum.S_AUDIT.getRole());
        Query query = new Query().addCriteria(Criteria.where("spaceId").is(spaceId));
        long num = 0L;
        if (state != 0) {
            query.addCriteria(Criteria.where("state").ne(0));
            num = mongoTemplate.count(new Query().addCriteria(Criteria.where("spaceId").is(spaceId).and("state").is(0)), SpaceApply.class);
        } else {
            query.addCriteria(Criteria.where("state").is(state));
            num = mongoTemplate.count(new Query().addCriteria(Criteria.where("spaceId").is(spaceId).and("state").ne(state)), SpaceApply.class);
        }
        if (StringUtils.isNotEmpty(applicant)) {
            Pattern pattern = Pattern.compile("^.*" + CommonUtils.escapeExprSpecialWord(applicant.trim()) + ".*$", Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where("applicant").regex(pattern));
        }
        if (StringUtils.isNotEmpty(content)) {
            Pattern pattern = Pattern.compile("^.*" + CommonUtils.escapeExprSpecialWord(content.trim()) + ".*$", Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where("title").regex(pattern));
        }
        Map<String, Object> resultMap = new HashMap<>(2);
        long count = mongoTemplate.count(query, SpaceApply.class);
        List<SpaceApply> spaceApplies = null;
        if (count > 0) {
            query.with(PageRequest.of(page - 1, size));
            query.with(Sort.by(Sort.Order.desc("createTime")));
            spaceApplies = mongoTemplate.find(query, SpaceApply.class);
        }
        if (state == 0) {
            resultMap.put("auditNum", count);
            resultMap.put("recordNum", num);
        } else {
            resultMap.put("auditNum", num);
            resultMap.put("recordNum", count);
        }
        resultMap.put("data", spaceApplies);
        return ResultUtil.success(resultMap);
    }

    public ResponseResult<Object> approve(String token, String applyId, String role, String result, String reason) {
        Token user = jwtTokenUtils.getToken(token);
        if (result.equals("reject")) {
            if (StringUtils.isEmpty(reason)) {
                return ResultUtil.errorInternational("GENERAL_PARAMETER_ERROR");
            }
        } else if (!result.equals("pass")) {
            return ResultUtil.errorInternational("GENERAL_PARAMETER_ERROR");
        }
        SpaceApply spaceApply = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(applyId)), SpaceApply.class);
        if (null == spaceApply) {
            return ResultUtil.errorInternational("GENERAL_NOT_EXIST");
        }
        // Permission verification
        spaceControlConfig.spatialVerification(spaceApply.getSpaceId(), user.getEmailAccounts(), Constants.SpaceRole.LEVEL_OTHER);
        spaceControlConfig.validateSpacePermissions(user.getEmailAccounts(), spaceApply.getSpaceId(), SpaceRoleEnum.S_AUDIT.getRole());
        String spaceApplyId = spaceApply.getApplyId();
        Space space = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(spaceApply.getSpaceId())), Space.class);
        if (null == space) {
            return ResultUtil.errorInternational("GENERAL_NOT_EXIST");
        }
        Set<AuthorizationPerson> authorizationList = space.getAuthorizationList();
        AuthorizationPerson approver = spaceApply.getApplicant();
        // ConsumerDO consumerDO = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(spaceApply.getUserId())), ConsumerDO.class);
        String msgTitle = "您申请的加入空间（" + space.getSpaceName() + "）";
        String title = "";
        EmailModel space_apply;
        // Personal application records
        Apply apply = null;
        if (StringUtils.isEmpty(spaceApplyId)) {
            String content = messageInternational("SPACE_USER") + approver.getUserName() + "（" + approver.getEmail() + messageInternational("SPACE_JOIN") + space.getSpaceName() + "）";
            apply = Apply.builder().applyId(applyId).applicant(new Person(approver)).content(content).submitDate(getCurrentDateTimeString()).type(TYPE_SPACE_JOIN).approvedStates(APPROVED_NOT).spaceId(space.getSpaceId()).spaceName(space.getSpaceName()).spaceDescription("").spaceLogo("").spaceTag(null).description(reason).build();
        } else {
            apply = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(spaceApplyId)), Apply.class);
        }
        if (result.equals("reject")) {
            msgTitle += "未通过审批！驳回原因：" + reason;
            title = TITLE_NOT_PASSED;
            space_apply = EmailModel.APPLY_RESULT_REJECT();
            space_apply.setAlert(reason);
            spaceApply.setRejectReason(reason);
            spaceApply.setState(2);
            apply.setReject(reason);
            apply.setApprovedResult(Apply.RESULT_NOT_PASS);
        } else if (result.equals("pass")) {
            if (!role.equals(SPACE_SENIOR) && !role.equals(SPACE_GENERAL)) {
                return ResultUtil.errorInternational("SYSTEM_ERROR");
            }
            msgTitle += "已通过审批！";
            title = TITLE_PASSED;
            space_apply = EmailModel.APPLY_RESULT_PASS();
            approver.setRole(role);
            authorizationList.add(approver);
            space.setAuthorizationList(authorizationList);
            spaceApply.setState(1);
            mongoTemplate.save(space);
            spaceControlConfig.spaceStat(space.getSpaceId(), "memberCount", 1L);
            // log
            String content = "申请加入：" + approver.getUserName() + " 通过审核加入空间";
            spaceControlConfig.spaceLogSave(space.getSpaceId(), content, user.getUserId(), new Operator(user), SpaceSvnLog.ACTION_MEMBER);
            apply.setApprovedResult(Apply.RESULT_PASS);
            new CacheLoading().updateSpaceAuth(space.getSpaceId(), spaceApply.getUserId());
            updateFtpAuth(approver.getEmail(), space.getSpaceShort(), space.getSpaceId(), "add");
        } else {
            return ResultUtil.errorInternational("SYSTEM_ERROR");
        }
        AuthorizationPerson authorizationPerson = new AuthorizationPerson();
        authorizationPerson.setAvatar(user.getAvatar());
        authorizationPerson.setUserName(user.getName());
        authorizationPerson.setEmail(user.getEmailAccounts());
        authorizationPerson.setRole(user.getRoles().get(0));
        authorizationPerson.setUserId(user.getUserId());
        spaceApply.setApprover(authorizationPerson);
        mongoTemplate.save(spaceApply);
        apply.setApprover(new Person(user));
        apply.setCompletedDate(getCurrentDateTimeString());
        apply.setApprovedStates(Apply.APPROVED_PASS);
        applyRepository.save(apply);
        // Clear application records
        cacheLoading.deleteUserApply(spaceApply.getUserId(), space.getSpaceId());
        // Email sending
        String message = space_apply.getMessage().replaceAll("spaceName", space.getSpaceName());
        space_apply.setMessage(message);
        Map<String, Object> param = new HashMap<>(16);
        param.put("name", approver.getUserName());
        param.put("url", spaceUrl.getSpaceDetailUrl().replaceAll("spaceId", space.getSpaceId()));
        param.put("email", approver.getEmail());
        asyncDeal.send(param, space_apply, EmailRole.SPACE_JOIN_APPLY);
        // send message
        Map<String, Object> msgMap = new HashMap<>(2);
        msgMap.put("title", title);
        msgMap.put("content", msgTitle);
        String emailAccounts = approver.getEmail();
        msgUtil.sendMsg(emailAccounts, msgUtil.mapToString(msgMap));
        messageService.sendToApplicant(title, msgTitle, new Person(approver), 1);
        return ResultUtil.success();
    }

    /**
     * Task Disaster Recovery - Space
     */
    public ResponseResult<Object> recovery(String token, RequestAdd requestAdd) {
        String status = requestAdd.getStatus();
        String strategy = requestAdd.getStrategy();
        String executionCycle = requestAdd.getExecutionCycle();
        String time = requestAdd.getTime();
        String value = requestAdd.getValue();
        String spaceId = requestAdd.getSpaceId();
        Token user = jwtTokenUtils.getToken(token);
        // Permission verification
        spaceControlConfig.spatialVerification(spaceId, user.getEmailAccounts(), Constants.SpaceRole.LEVEL_OTHER);
        spaceControlConfig.validateSpacePermissions(user.getEmailAccounts(), spaceId, SpaceRoleEnum.S_CONF_BAK.getRole());
        if (StringUtils.isEmpty(spaceId) || StringUtils.isEmpty(status)) {
            return ResultUtil.errorInternational("SYSTEM_ERROR");
        }
        if (!status.equals(Constants.Backup.START) && !status.equals(Constants.Backup.STOP)) {
            return ResultUtil.errorInternational("SYSTEM_ERROR");
        }
        if (status.equals(Constants.Backup.START)) {
            if (StringUtils.isEmpty(strategy) || StringUtils.isEmpty(executionCycle) || StringUtils.isEmpty(time)) {
                return ResultUtil.errorInternational("SYSTEM_ERROR");
            }
            if (!strategy.equals(Constants.Backup.ALL) && !strategy.equals(Constants.Backup.THREE) && !strategy.equals(Constants.Backup.NEW)) {
                return ResultUtil.errorInternational("SYSTEM_ERROR");
            }
            if (!executionCycle.equals(Constants.Backup.DAY) && !executionCycle.equals(Constants.Backup.WEEK) && !executionCycle.equals(Constants.Backup.MONTH)) {
                return ResultUtil.errorInternational("SYSTEM_ERROR");
            }
            if (StringUtils.isEmpty(time) || !judgeTime(time)) {
                return ResultUtil.errorInternational("SYSTEM_ERROR");
            }
            if (executionCycle.equals(Constants.Backup.WEEK) || executionCycle.equals(Constants.Backup.MONTH)) {
                if (StringUtils.isEmpty(value) || !judgeValue(value, executionCycle)) {
                    return ResultUtil.errorInternational("SYSTEM_ERROR");
                }
            }
        } else {
            if (StringUtils.isEmpty(requestAdd.getId())) {
                return ResultUtil.errorInternational("SYSTEM_ERROR");
            }
        }
        // processing logic
        Space space = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(spaceId).and("state").is("1")), Space.class);
        if (null == space) {
            return ResultUtil.errorInternational("RESOURCE_DOES_NOT_EXIST");
        }
        if (status.equals(Constants.Backup.STOP)) {
            // Stop Task
            BackupSpaceMain backupSpaceMain = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(requestAdd.getId())), BackupSpaceMain.class);
            if (backupSpaceMain != null && backupSpaceMain.getStatus().equals(Constants.Backup.START)) {
                // Stop scheduled tasks
                String triggerState = QuartzManager.getTriggerState(backupSpaceMain.getJobId(), spaceId);
                if (!triggerState.equals("NONE")) {
                    // suspend
                    QuartzManager.pauseJob(backupSpaceMain.getJobId(), spaceId);
                    // remove
                    QuartzManager.removeJob(backupSpaceMain.getJobId(), spaceId);
                    triggerState = QuartzManager.getTriggerState(backupSpaceMain.getJobId(), spaceId);
                }
                backupSpaceMain.setJobStatus(triggerState);
                backupSpaceMain.setStatus(status);
                mongoTemplate.save(backupSpaceMain);
            }
        } else {
            // Add or initiate tasks
            Date executionTime = getExecutionTime(time, executionCycle);
            Date nextTime = getNextTime(executionTime, executionCycle);
            FtpHost ftpHost = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("invoke").is(true)), FtpHost.class);
            if (null == ftpHost) {
                return ResultUtil.errorInternational("SPACE_BACK_FTP");
            }
            if (StringUtils.isNotBlank(requestAdd.getId())) {
                // Update+Restart
                BackupSpaceMain backupSpaceMain = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(requestAdd.getId())), BackupSpaceMain.class);
                if (null != backupSpaceMain) {
                    backupSpaceMain.setLastUpdateTime(new Date());
                    backupSpaceMain.setStatus(status);
                    if (!strategy.equals(backupSpaceMain.getStrategy())) {
                        backupSpaceMain.setStrategy(strategy);
                    }
                    // Determine if the task time has changed
                    boolean judge = false;
                    if (!executionCycle.equals(backupSpaceMain.getExecutionCycle())) {
                        backupSpaceMain.setExecutionCycle(executionCycle);
                        judge = true;
                    }
                    if (!time.equals(backupSpaceMain.getExecutionTime())) {
                        backupSpaceMain.setExecutionTime(time);
                        judge = true;
                    }
                    if (StringUtils.isNotEmpty(value) && !value.equals(backupSpaceMain.getExecutionValue())) {
                        backupSpaceMain.setExecutionValue(value);
                        judge = true;
                    }
                    if (judge) {
                        String corn = getCorn(executionCycle, time, value);
                        backupSpaceMain.setCorn(corn);
                    }
                    // task scheduling
                    String jobs = QuartzManager.getTriggerState(backupSpaceMain.getJobId(), spaceId);
                    if (jobs.equals("NONE")) {
                        // Task does not exist and can be added directly
                        QuartzManager.addJob(backupSpaceMain.getJobId(), spaceId, MyJob.class, backupSpaceMain.getCorn());
                    } else if (jobs.equals("NORMAL") && judge) {
                        // Normal Execution - Modify Trigger Time
                        QuartzManager.modifyJobTime(backupSpaceMain.getJobId(), spaceId, backupSpaceMain.getCorn());
                    } else {
                        // Other states stop first - remove before joining
                        // suspend
                        QuartzManager.pauseJob(backupSpaceMain.getJobId(), spaceId);
                        // remove
                        QuartzManager.removeJob(backupSpaceMain.getJobId(), spaceId);
                        QuartzManager.addJob(backupSpaceMain.getJobId(), spaceId, MyJob.class, backupSpaceMain.getCorn());
                    }
                    String endStatus = QuartzManager.getTriggerState(backupSpaceMain.getJobId(), spaceId);
                    backupSpaceMain.setJobStatus(endStatus);
                    backupSpaceMain.setRecentlyTime(executionTime);
                    backupSpaceMain.setNextTime(nextTime);
                    mongoTemplate.save(backupSpaceMain);
                }
            } else {
                // Add Task
                long count = mongoTemplate.count(new Query().addCriteria(Criteria.where("spaceId").is(spaceId)), BackupSpaceMain.class);
                if (count > 0) {
                    return ResultUtil.errorInternational("SPACE_BACK_JOIN");
                }
                String filePath = space.getFilePath();
                BackupSpaceMain backupSpaceMain = new BackupSpaceMain();
                AuthorizationPerson person = new AuthorizationPerson(user);
                backupSpaceMain.setPerson(person);
                backupSpaceMain.setJobId(CommonUtils.generateSnowflake());
                backupSpaceMain.setSpaceId(spaceId);
                backupSpaceMain.setSpaceName(space.getSpaceName());
                backupSpaceMain.setSpacePath(filePath);
                backupSpaceMain.setCreateTime(new Date());
                // strategy
                backupSpaceMain.setStatus(status);
                backupSpaceMain.setStrategy(strategy);
                backupSpaceMain.setExecutionCycle(executionCycle);
                String corn = getCorn(executionCycle, time, value);
                backupSpaceMain.setCorn(corn);
                backupSpaceMain.setExecutionTime(time);
                backupSpaceMain.setExecutionValue(value);
                // Submit to scheduled task
                QuartzManager.addJob(backupSpaceMain.getJobId(), spaceId, MyJob.class, corn);
                String jobs = QuartzManager.getTriggerState(backupSpaceMain.getJobId(), spaceId);
                backupSpaceMain.setJobStatus(jobs);
                backupSpaceMain.setRecentlyTime(executionTime);
                backupSpaceMain.setNextTime(nextTime);
                mongoTemplate.save(backupSpaceMain);
            }
        }
        return ResultUtil.success();
    }

    private String getCorn(String executionCycle, String date, String value) {
        String[] sortTime = date.split(":");
        // second
        String sec = filter(sortTime[2]);
        // minute
        String min = filter(sortTime[1]);
        // hour
        String hour = filter(sortTime[0]);
        // day
        String day = "";
        // month
        String mon = "*";
        // week
        String week = "";
        if (executionCycle.equals(Constants.Backup.DAY)) {
            day = "*";
            mon = "*";
            week = "?";
        } else if (executionCycle.equals(Constants.Backup.WEEK)) {
            day = "?";
            mon = "*";
            week = value;
        } else if (executionCycle.equals(Constants.Backup.MONTH)) {
            if (value.equals("last")) {
                day = "L";
            } else {
                day = filter(value);
            }
            mon = "*";
            week = "?";
        }
        return sec + " " + min + " " + hour + " " + day + " " + mon + " " + week;
    }

    private String filter(String str) {
        if (str.contains("0")) {
            if (str.substring(0, 1).equals("0")) {
                return str.substring(1);
            }
        }
        return str;
    }

    private boolean judgeTime(String time) {
        if (time.length() != 8) {
            return false;
        }
        if (!time.contains(":")) {
            return false;
        }
        if (time.split(":").length != 3) {
            return false;
        }
        return true;
    }

    private boolean judgeValue(String value, String type) {
        if (type.equals(Constants.Backup.MONTH)) {
            if (value.equals("last")) {
                return true;
            }
        }
        int intValue = 0;
        try {
            intValue = Integer.valueOf(value);
        } catch (Exception e) {
            return false;
        }
        if (type.equals(Constants.Backup.WEEK)) {
            if (intValue < 1 || intValue > 7) {
                return false;
            }
        } else {
            if (intValue < 1 || intValue > 33) {
                return false;
            }
        }
        return true;
    }

    private Date getExecutionTime(String time, String cycle) {
        Date date = new Date();
        String dateTimeString = getDateTimeString(date);
        String exTime = dateTimeString.split(" ")[0] + " " + time;
        Date exDate = null;
        try {
            exDate = getStringToDateTime(exTime);
        } catch (ParseException e) {
            e.printStackTrace();
            throw new CommonException(-1, "data to String exception");
        }
        if (date.getTime() < exDate.getTime()) {
            return exDate;
        } else {
            BackupUtils backupUtils = new BackupUtils();
            return backupUtils.getNextDate(exDate, cycle);
        }
    }

    private Date getNextTime(Date executionTime, String cycle) {
        BackupUtils backupUtils = new BackupUtils();
        return backupUtils.getNextDate(executionTime, cycle);
    }

    public ResponseResult<Object> getRecovery(String token, String spaceId) {
        if (StringUtils.isEmpty(spaceId)) {
            return ResultUtil.success(null);
        }
        // Permission verification
        String email = jwtTokenUtils.getEmail(token);
        spaceControlConfig.spatialVerification(spaceId, email, Constants.SpaceRole.LEVEL_OTHER);
        spaceControlConfig.validateSpacePermissions(email, spaceId, SpaceRoleEnum.S_CONF_BAK.getRole());
        // processing logic
        Space space = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(spaceId).and("state").is("1")), Space.class);
        if (null == space) {
            return ResultUtil.errorInternational("RESOURCE_DOES_NOT_EXIST");
        }
        Query query = new Query().addCriteria(Criteria.where("spaceId").is(spaceId));
        BackupSpaceMain backupSpaceMain = mongoTemplate.findOne(query, BackupSpaceMain.class);
        if (null != backupSpaceMain) {
            backupSpaceMain.setSpacePath(null);
        }
        return ResultUtil.success(backupSpaceMain);
    }

    public ResponseResult<Object> backCheck() {
        FtpHost ftpHost = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("invoke").is(true)), FtpHost.class);
        if (null == ftpHost) {
            return ResultUtil.success(false);
        }
        return ResultUtil.success(true);
    }

    /**
     * Add file metadata
     */
    public ResponseResult<Object> addFileData(HttpServletRequest request, String token, FileData fileData) {
        String email = jwtTokenUtils.getEmail(token);
        String hash = fileData.getHash();
        String spaceId = fileData.getSpaceId();
        if (StringUtils.isEmpty(hash) || StringUtils.isEmpty(spaceId)) {
            return ResultUtil.errorInternational("GENERAL_PARAMETER_ERROR");
        }
        String fileAuthorAsHash = fileMappingManage.getFileAuthorAsHash(hash, spaceId);
        String role = fileAuthorAsHash.equals(email) ? SpaceRoleEnum.F_EDIT_AM.getRole() : SpaceRoleEnum.F_EDIT_OT.getRole();
        spaceControlConfig.validateSpacePermissions(email, spaceId, role);
        String id = fileData.getId();
        if (StringUtils.isNotEmpty(id)) {
            // modify
            FileMapping one = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(id)), FileMapping.class, spaceId);
            if (null != one) {
                one.setData(fileData.getData());
                mongoTemplate.save(one, spaceId);
            }
        } else {
            FileMapping fileMapping = fileMappingManage.getFileMappingAsHash(hash, spaceId);
            if (null == fileMapping) {
                try {
                    ElfinderStorage elfinderStorage = elfinderStorageService.getElfinderStorage(request, spaceId);
                    Target target = elfinderStorage.fromHash(hash);
                    String spacePath = target.toString();
                    fileMapping = fileMappingManage.getFileMapping(spacePath, spaceId);
                } catch (Exception e) {
                    e.printStackTrace();
                    return ResultUtil.errorInternational(messageInternational("FILE_SPACE_ERROR"));
                }
            }
            fileMapping.setData(fileData.getData());
            mongoTemplate.save(fileMapping, spaceId);
        }
        return ResultUtil.success();
    }

    /**
     * Obtain file metadata
     */
    public ResponseResult<Object> getFileData(String token, String spaceId, String hash, HttpServletRequest request) {
        if (StringUtils.isEmpty(spaceId) || StringUtils.isEmpty(hash)) {
            return ResultUtil.errorInternational("GENERAL_PARAMETER_ERROR");
        }
        if (!token.equals("public")) {
            String email = jwtTokenUtils.getEmail(token);
            spaceControlConfig.spatialVerification(spaceId, email, Constants.SpaceRole.LEVEL_OTHER);
        }
        FileMapping fileMappingAsHash = fileMappingManage.getFileMappingAsHash(hash, spaceId);
        if (fileMappingAsHash == null) {
            ElfinderStorage elfinderStorage = elfinderStorageService.getElfinderStorage(request, spaceId);
            String path = elfinderStorage.fromHash(hash).toString();
            fileMappingAsHash = fileMappingManage.getFileMapping(path, spaceId);
        }
        List<?> objects = null == fileMappingAsHash ? new ArrayList<>(0) : fileMappingAsHash.getData();
        Map<String, Object> resultMap = new HashMap<>(2);
        resultMap.put("id", null == fileMappingAsHash ? "" : fileMappingAsHash.getId());
        resultMap.put("data", objects);
        return ResultUtil.success(resultMap);
    }

    /**
     * Space Permissions Menu
     */
    public ResponseResult<Object> menuList(String token, String spaceId, String spaceRole) {
        if (StringUtils.isEmpty(spaceId) || StringUtils.isEmpty(spaceId.trim()) || StringUtils.isEmpty(spaceRole) || StringUtils.isEmpty(spaceRole.trim())) {
            return ResultUtil.errorInternational("GENERAL_PARAMETER_ERROR");
        }
        if (!spaceRole.equals(SPACE_SENIOR) && !spaceRole.equals(SPACE_GENERAL)) {
            return ResultUtil.errorInternational("GENERAL_PARAMETER_ERROR");
        }
        // Permission verification
        String email = jwtTokenUtils.getEmail(token);
        spaceControlConfig.spatialVerification(spaceId, email, Constants.SpaceRole.LEVEL_OTHER);
        spaceControlConfig.validateSpacePermissions(email, spaceId, SpaceRoleEnum.S_CONF_PER.getRole());
        List<SpaceMenu> spaceMenuList = mongoTemplate.findAll(SpaceMenu.class);
        Query query = new Query().addCriteria(Criteria.where("spaceId").is(spaceId).and("roleName").is(spaceRole));
        SpaceRole space_role = mongoTemplate.findOne(query, SpaceRole.class);
        if (null == space_role) {
            return ResultUtil.success(spaceMenuList);
        }
        List<String> menus = space_role.getMenus();
        if (null == menus || menus.isEmpty()) {
            return ResultUtil.success(spaceMenuList);
        }
        for (SpaceMenu spaceMenu : spaceMenuList) {
            List<SpaceMenu.Action> actionList = spaceMenu.getActionList();
            for (SpaceMenu.Action action : actionList) {
                for (SpaceMenu.Role role : action.getRoleList()) {
                    if (menus.contains(role.getRoleKey())) {
                        role.setDisable(true);
                    }
                }
            }
        }
        return ResultUtil.success(spaceMenuList);
    }

    /**
     * Configure Space Role
     */
    public ResponseResult<Object> setSpaceRole(String token, List<SpaceRoleRequest> spaceRoleRequests) {
        List<String> validation = validation(spaceRoleRequests);
        if (!validation.isEmpty()) {
            return ResultUtil.error("参数错误: {} " + validation.toString());
        }
        String email = jwtTokenUtils.getEmail(token);
        for (SpaceRoleRequest spaceRoleRequest : spaceRoleRequests) {
            List<String> roles = spaceRoleRequest.getRoles();
            String spaceId = spaceRoleRequest.getSpaceId();
            // Permission verification
            spaceControlConfig.spatialVerification(spaceId, email, Constants.SpaceRole.LEVEL_OTHER);
            spaceControlConfig.validateSpacePermissions(email, spaceId, SpaceRoleEnum.S_CONF_PER.getRole());
            String roleName = spaceRoleRequest.getRoleName();
            if (null == roles) {
                roles = new ArrayList<>(0);
            }
            if (roles.size() > 30) {
                return ResultUtil.errorInternational("GENERAL_PARAMETER_ERROR");
            }
            Query query = new Query().addCriteria(Criteria.where("spaceId").is(spaceId).and("roleName").is(roleName));
            SpaceRole space_role = mongoTemplate.findOne(query, SpaceRole.class);
            if (null == space_role) {
                space_role = new SpaceRole();
                space_role.setSpaceId(spaceId);
                space_role.setRoleName(roleName);
                space_role.setCreateTime(new Date());
                space_role.setUpdateTime(new Date());
            }
            space_role.setMenus(roles);
            mongoTemplate.save(space_role);
            // Clear permissions
            cacheLoading.clearSpaceMenRole(spaceId);
        }
        return ResultUtil.success();
    }

    public ResponseResult<Object> userSpaceRoles(String token, String spaceId) {
        String email = jwtTokenUtils.getEmail(token);
        spaceControlConfig.spatialVerification(spaceId, email, Constants.SpaceRole.LEVEL_OTHER);
        String spaceUserRole = cacheLoading.getSpaceUserRole(spaceId, email);
        if (spaceUserRole.equals(SpaceService.SPACE_OWNER)) {
            List<String> list = new ArrayList<>();
            List<SpaceMenu> spaceMenuList = mongoTemplate.findAll(SpaceMenu.class);
            for (SpaceMenu spaceMenu : spaceMenuList) {
                for (SpaceMenu.Action action : spaceMenu.getActionList()) {
                    for (SpaceMenu.Role role : action.getRoleList()) {
                        list.add(role.getRoleKey());
                    }
                }
            }
            return ResultUtil.success(list);
        } else {
            return ResultUtil.success(cacheLoading.getSpaceMenRole(spaceId, email));
        }
    }

    public ResponseResult<Long> getGb(String token) {
        ApproveSetting approveSetting = approveSettingRepository.findAll().get(0);
        return ResultUtil.success(approveSetting.getGb());
    }

    /**
     * Space file preview component acquisition
     */
    public ResponseResult<Map<String, Object>> getComponent(String token, String spaceId, String hash) {
        if (StringUtils.isEmpty(spaceId) || StringUtils.isEmpty(hash)) {
            return ResultUtil.errorInternational("GENERAL_PARAMETER_ERROR");
        }
        if (!token.equals("public")) {
            String email = jwtTokenUtils.getEmail(token);
            spaceControlConfig.spatialVerification(spaceId, email, Constants.SpaceRole.LEVEL_OTHER);
        }
        Query query = new Query().addCriteria(Criteria.where("hash").is(hash));
        FileMapping one = mongoTemplate.findOne(query, FileMapping.class, spaceId);
        if (null == one) {
            return ResultUtil.errorInternational("file_not_exist");
        }
        if (one.getType() == 1) {
            return ResultUtil.error("请选择文件，文件夹不支持预览");
        }
        Query query1 = new Query().addCriteria(Criteria.where("fileTypes").is(one.getSuffix().toLowerCase()));
        List<ComponentShow> components = mongoTemplate.find(query1, ComponentShow.class, "component");
        Map<String, Object> ret = new HashMap<>();
        ret.put("components", components);
        ret.put("status", 0);
        if (!CollectionUtils.isEmpty(components)) {
            // Components already installed locally
            return ResultUtil.success(ret);
        }
        // Check if there are any components that can be used
        ResponseResult<Object> componentResult = managementService.component(null, 1, 10, 0, null, null);
        List<Map<String, Object>> matchComponents = new ArrayList<>();
        if (componentResult.getCode() == 0) {
            // Successfully obtained component remotely
            Map<String, Object> data = (Map<String, Object>) componentResult.getData();
            Object list = data.get("list");
            if (list != null) {
                List<Map<String, Object>> list2 = (List<Map<String, Object>>) list;
                for (Map<String, Object> comRemote : list2) {
                    if ("front-end".equals(comRemote.get("componentType").toString())) {
                        // Front end components
                        if ((Boolean) comRemote.get("isInstall") == true) {
                            // Already installed
                            continue;
                        }
                        Object parameters = comRemote.get("parameters");
                        if (parameters != null) {
                            boolean isMatch = false;
                            Object suffix = ((Map<String, Object>) parameters).get("suffix");
                            if (suffix != null) {
                                String[] suffixs = suffix.toString().contains(",") ? suffix.toString().split(",") : suffix.toString().split("，");
                                for (String s : suffixs) {
                                    if (one.getSuffix().toLowerCase().equals(s)) {
                                        isMatch = true;
                                        break;
                                    }
                                }
                                if (isMatch) {
                                    matchComponents.add(comRemote);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!CollectionUtils.isEmpty(matchComponents)) {
            // There are corresponding components for locally uninstalled components
            ret.put("status", 1);
            int maxDownloadCount = 0;
            Map<String, Object> retMatchComponent = new HashMap<>();
            // Find the one with the highest number of installations
            for (Map<String, Object> matchComponent : matchComponents) {
                int currentDownloadCount = (Integer) matchComponent.get("downloadCount");
                if (currentDownloadCount >= maxDownloadCount) {
                    maxDownloadCount = currentDownloadCount;
                    retMatchComponent = matchComponent;
                }
            }
            ret.put("matchComponent", retMatchComponent);
        } else {
            // There are no corresponding components for locally uninstalled components
            ret.put("status", 2);
        }
        return ResultUtil.success(ret);
    }

    /**
     * File preview
     */
    public ResponseResult<Object> previewData(String token, String spaceId, String hash, String componentId, HttpServletRequest request) {
        if (StringUtils.isEmpty(componentId) || StringUtils.isEmpty(hash)) {
            return ResultUtil.errorInternational("GENERAL_PARAMETER_ERROR");
        }
        if (!token.equals("public")) {
            String email = jwtTokenUtils.getEmail(token);
            spaceControlConfig.spatialVerification(spaceId, email, Constants.SpaceRole.LEVEL_OTHER);
        }
        Query query = new Query().addCriteria(Criteria.where("hash").is(hash));
        FileMapping one = mongoTemplate.findOne(query, FileMapping.class, spaceId);
        if (null == one) {
            return ResultUtil.errorInternational("file_not_exist");
        }
        if (one.getType() == 1) {
            return ResultUtil.error("请选择文件，文件夹不支持预览");
        }
        File file = new File(one.getPath());
        if (!file.exists()) {
            return ResultUtil.errorInternational("file_not_exist");
        }
        Query query1 = new Query().addCriteria(Criteria.where("_id").is(componentId));
        Component component = mongoTemplate.findOne(query1, Component.class);
        List<String> fileTypes = component.getFileTypes();
        if (!fileTypes.contains(one.getSuffix().toLowerCase()) && !fileTypes.contains(one.getSuffix().toUpperCase())) {
            return ResultUtil.error("该组件不支持预览");
        }
        List<Map<String, Object>> parameters = component.getParameters();
        String type = "";
        String data = "";
        ArrayList<Map<String, Object>> custom = null;
        for (Map<String, Object> parameter : parameters) {
            String key = parameter.get("key").toString();
            if (key.contains("type")) {
                type = parameter.get("value").toString();
            }
            if (key.equals("data")) {
                data = parameter.get("value").toString();
            }
            if (key.equals("custom")) {
                custom = (ArrayList<Map<String, Object>>) parameter.get("value");
            }
        }
        if (StringUtils.isEmpty(type)) {
            return ResultUtil.success();
        }
        Map resultMap = JSONObject.parseObject(data, Map.class);
        if (type.equals("fileUrl")) {
            String url = "";
            String suffix = one.getSuffix().toLowerCase();
            if (suffix.equals("shp")) {
                url = spaceUrl.getCallHost() + "/api/file/shpFileDown?hash=" + hash + "&name=shp.zip" + "&spaceId=" + spaceId;
            } else {
                ResponseResult<Object> responseResult = this.previewUrl(spaceId, null, hash, request);
                if (responseResult.getCode() != 0) {
                    return responseResult;
                }
                url = responseResult.getData().toString();
            }
            List<String> list = new ArrayList<>(1);
            list.add(url);
            resultMap.put("fileUrl", list);
        } else if (type.equals("fileStr")) {
            String content = readJsonFile(file.getPath());
            resultMap.put("content", content);
        } else if ("onlyoffice".equals(type)) {
            Map<String, Object> officeInfo = wopiService.getOfficeInfo(one, spaceId, token);
            String actionUrl = officeInfo.get("actionUrl").toString();
            if (custom != null) {
                for (int i = 0; i < custom.size(); i++) {
                    if ("baseUrl".equals(custom.get(i).get("key"))) {
                        String baseUrl = custom.get(i).get("value").toString();
                        // Server side simple judgment of office document server availability
                        HttpClient httpClient = new HttpClient();
                        try {
                            int respCode = httpClient.doGetRetStatus(baseUrl);
                            if (respCode != 200 && respCode != 403) {
                                return ResultUtil.error(204, "请检查组件配置的文档服务器地址是否可用");
                            }
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                            return ResultUtil.error(204, "请检查组件配置的文档服务器地址是否可用");
                        }
                        actionUrl = baseUrl + actionUrl;
                    }
                }
            }
            resultMap.put("actionUrl", actionUrl);
            resultMap.put("access_token", token);
        } else {
            return ResultUtil.success();
        }
        return ResultUtil.success(resultMap);
    }

    /**
     * Space log download
     */
    public void logDown(String spaceId, HttpServletRequest request, HttpServletResponse response) {
        String token = CommonUtils.getUser(request, Constants.TOKEN);
        if (null == token) {
            throw new CommonException(401, "请登录");
        }
        if (!jwtTokenUtils.validateToken(token)) {
            Cookie cookie = CommonUtils.getCookie(Constants.TOKEN, "", 0, 1);
            Cookie cookie2 = CommonUtils.getCookie(Constants.WAY, "", 0, 1);
            response.addCookie(cookie);
            response.addCookie(cookie2);
            throw new CommonException(401, "请登录");
        } else {
            String ifPresent = tokenCache.getIfPresent(token);
            if (ifPresent == null) {
                throw new CommonException(401, "请登录");
            }
        }
        String email = jwtTokenUtils.getEmail(token);
        // Verify space permissions
        spaceControlConfig.spatialVerification(spaceId, email, Constants.SpaceRole.LEVEL_OTHER);
        SpaceSimple spaceSimple = cacheLoading.getSpaceSimple(spaceId);
        String spaceName = spaceSimple.getSpaceName();
        String logName = spaceName + "_日志.csv";
        String logCsv = spaceId + ".csv";
        // Read JSON file
        String spaceLogPath = spaceUrl.getSpaceLogPath();
        File file = new File(spaceLogPath, spaceId);
        if (!file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            synchronized (this) {
                File logFile = new File(file.getPath(), logCsv);
                if (!logFile.exists()) {
                    File[] files = file.listFiles();
                    List<Object> logList = new ArrayList<>();
                    for (File file1 : files) {
                        String jsonObj = readJsonFile(file1.getPath());
                        logList.addAll(JSONObject.parseObject(jsonObj, List.class));
                    }
                    String hear = "操作人,操作时间,类型,日志描述";
                    List<String> dataList = new ArrayList<>(logList.size());
                    for (Object obj : logList) {
                        SpaceLog spaceSvnLog = JSONObject.parseObject(JSONObject.toJSONString(obj), SpaceLog.class);
                        String personName = null == spaceSvnLog.getOperator().getPersonName() ? "" : spaceSvnLog.getOperator().getPersonName();
                        String createTime = null == spaceSvnLog.getCreateTime() ? "" : CommonUtils.getDateTimeString(spaceSvnLog.getCreateTime());
                        String action = null == spaceSvnLog.getAction() ? "" : spaceSvnLog.getAction();
                        String description = null == spaceSvnLog.getDescription() ? "" : spaceSvnLog.getDescription();
                        dataList.add(personName + "," + createTime + "," + getLogType(action) + "," + description);
                    }
                    logList.clear();
                    CsvUtils.createCSVFile(dataList, hear, file.getPath(), logCsv);
                }
            }
            try {
                CsvUtils.exportFile(response, new File(file.getPath(), logCsv), logName);
                return;
            } catch (IOException e) {
                throw new CommonException(500, "日志下载失败，请重试!");
            }
        }
        throw new CommonException(404, "该空间下无历史日志!");
    }

    private String getLogType(String type) {
        switch(type) {
            case SpaceSvnLog.ACTION_FILE:
                return "文件";
            case SpaceSvnLog.ACTION_TABLE:
                return "表格";
            case SpaceSvnLog.ACTION_PUBLISH:
                return "发布";
            case SpaceSvnLog.ACTION_MEMBER:
                return "成员";
            case SpaceSvnLog.ACTION_VERSION:
                return "版本";
            default:
                return "其他";
        }
    }
}
