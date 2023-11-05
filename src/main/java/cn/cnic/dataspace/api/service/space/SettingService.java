package cn.cnic.dataspace.api.service.space;

import cn.cnic.dataspace.api.asynchronous.AsyncDeal;
import cn.cnic.dataspace.api.cacheLoading.CacheLoading;
import cn.cnic.dataspace.api.config.CreateMongoIndex;
import cn.cnic.dataspace.api.config.VersionUpdate;
import cn.cnic.dataspace.api.config.space.AsyncMethod;
import cn.cnic.dataspace.api.config.space.MsgUtil;
import cn.cnic.dataspace.api.config.space.SpaceControlConfig;
import cn.cnic.dataspace.api.exception.CommonException;
import cn.cnic.dataspace.api.model.apply.Apply;
import cn.cnic.dataspace.api.model.center.Account;
import cn.cnic.dataspace.api.model.manage.BasicSetting;
import cn.cnic.dataspace.api.model.manage.Identify;
import cn.cnic.dataspace.api.model.manage.ReleaseAccount;
import cn.cnic.dataspace.api.model.network.NetworkConf;
import cn.cnic.dataspace.api.model.space.*;
import cn.cnic.dataspace.api.model.space.child.Operator;
import cn.cnic.dataspace.api.model.space.child.Person;
import cn.cnic.dataspace.api.model.email.EmailRole;
import cn.cnic.dataspace.api.model.login.EscConf;
import cn.cnic.dataspace.api.model.login.UmtConf;
import cn.cnic.dataspace.api.model.login.WechatConf;
import cn.cnic.dataspace.api.model.login.WechatUser;
import cn.cnic.dataspace.api.model.statistics.SpaceDataStatistic;
import cn.cnic.dataspace.api.model.user.*;
import cn.cnic.dataspace.api.repository.*;
import cn.cnic.dataspace.api.service.impl.UserServiceImpl;
import cn.cnic.dataspace.api.util.*;
import cn.cnic.dataspace.api.websocket.SocketManager;
import cn.hutool.extra.cglib.CglibUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import static cn.cnic.dataspace.api.model.apply.Apply.TYPE_SPACE_JOIN;
import static cn.cnic.dataspace.api.model.user.Message.TITLE_PENDING;
import static cn.cnic.dataspace.api.service.space.SpaceService.SIZE_1G;
import static cn.cnic.dataspace.api.util.CommonUtils.*;
import static cn.cnic.dataspace.api.util.Constants.ADMIN;

/**
 * SettingService
 *
 * @author wangCc
 * @date 2021-04-06 15:32
 */
@Slf4j
@Service
@EnableAsync
public class SettingService {

    @Autowired
    private JwtTokenUtils jwtTokenUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplyRepository applyRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private ReleaseAccountRepository releaseAccountRepository;

    @Autowired
    private SpaceUrl spaceUrl;

    @Autowired
    private MsgUtil msgUtil;

    @Autowired
    private SvnSpaceLogRepository svnSpaceLogRepository;

    @Autowired
    private SpaceService spaceService;

    @Lazy
    @Autowired
    private AsyncDeal asyncDeal;

    @Lazy
    @Autowired
    private AsyncMethod asyncMethod;

    @Lazy
    @Autowired
    private MessageService messageService;

    @Lazy
    @Autowired
    private SpaceControlConfig spaceControlConfig;

    @Lazy
    @Autowired
    private VersionUpdate versionUpdate;

    @Lazy
    @Autowired
    private CacheLoading cacheLoading;

    @Lazy
    @Autowired
    private CreateMongoIndex createMongoIndex;

    private final Cache<String, String> tokenCache = CaffeineUtil.getTokenCache();

    private final Cache<String, Object> config = CaffeineUtil.getConfig();

    private final Cache<String, List<String>> emailToken = CaffeineUtil.getEmailToken();

    private final Cache<String, String> publicData = CaffeineUtil.getPublicData();

    /**
     * pwd check as login
     */
    public ResponseResult<Object> checkPassword(String password) {
        if (StringUtils.isEmpty(password)) {
            return ResultUtil.success(messageInternational("WEAK"));
        }
        return ResultUtil.success(CommonUtils.pwdStrength(password));
    }

    /**
     * pwd verification personal setting
     */
    public ResponseResult<Object> verification(String token) {
        return ResultUtil.success(checkPassword(RSAEncrypt.decrypt(userRepository.findById(jwtTokenUtils.getUserIdFromToken(token)).get().getPassword())));
    }

    /**
     * update password
     */
    public ResponseResult<Object> updatePwd(String token, RequestPaw requestPaw) {
        ConsumerDO consumerDO = userRepository.findById(jwtTokenUtils.getUserIdFromToken(token)).get();
        String newPwd = RSAEncrypt.decrypt(requestPaw.getNewPwd());
        String conPwd = RSAEncrypt.decrypt(requestPaw.getConPwd());
        if (null == newPwd || null == conPwd) {
            return ResultUtil.errorInternational("PWD_STRENGTH");
        }
        if (!newPwd.equals(conPwd)) {
            return ResultUtil.errorInternational("PWD_NOT_MATCH");
        }
        if (!STRONG.equals(checkPassword(newPwd))) {
            return ResultUtil.errorInternational("PWD_STRENGTH");
        }
        if (StringUtils.isNotEmpty(requestPaw.getOrigPwd())) {
            String origPwd = RSAEncrypt.decrypt(requestPaw.getOrigPwd());
            if (null == origPwd) {
                return ResultUtil.errorInternational("PWD_STRENGTH");
            }
            String password = consumerDO.getPassword();
            if (StringUtils.isEmpty(password)) {
                return ResultUtil.errorInternational("PWD_ERROR");
            }
            String pwd = RSAEncrypt.decrypt(password);
            if (StringUtils.equals(pwd, newPwd)) {
                return ResultUtil.errorInternational("INCONSISTENT");
            }
            if (!Objects.equals(pwd, origPwd)) {
                return ResultUtil.errorInternational("PWD_ERROR");
            }
        }
        consumerDO.setPassword(requestPaw.getNewPwd());
        userRepository.save(consumerDO);
        return ResultUtil.success();
    }

    /**
     * get spare email
     */
    public ResponseResult<Object> spareEmail(String token) {
        ConsumerDO consumerDO = userRepository.findById(jwtTokenUtils.getUserIdFromToken(token)).get();
        if (Objects.isNull(consumerDO.getSpareEmail())) {
            return ResultUtil.successInternational("UNBOUND");
        } else {
            return ResultUtil.success(consumerDO.getSpareEmail());
        }
    }

    /**
     * setting spare email
     */
    public ResponseResult<Object> setEmail(String token, String spareEmail) {
        ConsumerDO consumerDO = userRepository.findById(jwtTokenUtils.getUserIdFromToken(token)).get();
        if (mongoTemplate.count(new Query().addCriteria(new Criteria().orOperator(Criteria.where("emailAccounts").is(spareEmail), Criteria.where("spareEmail").is(spareEmail))), ConsumerDO.class) > 0) {
            return ResultUtil.errorInternational("EMAIL_USED");
        }
        consumerDO.setSpareEmail(spareEmail);
        userRepository.save(consumerDO);
        return ResultUtil.success();
    }

    /**
     * my applications
     */
    public ResponseResult<Object> apply(String token, HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>(16);
        final String type = request.getParameter("type");
        final String description = request.getParameter("description");
        Query query = new Query().addCriteria(Criteria.where("applicant.personId").is(jwtTokenUtils.getUserIdFromToken(token)));
        if (StringUtils.isNotBlank(type)) {
            query.addCriteria(Criteria.where("type").is(type));
        }
        if (StringUtils.isNotBlank(description)) {
            query.addCriteria(Criteria.where("description").is(Pattern.compile("^.*" + CommonUtils.escapeExprSpecialWord(description.trim()) + ".*$", Pattern.CASE_INSENSITIVE)));
        }
        query.with(Sort.by("submitDate").descending());
        map.put("count", mongoTemplate.count(query, Apply.class));
        map.put("content", mongoTemplate.find(query.with(PageRequest.of(Integer.parseInt(request.getParameter("pageOffset")), Integer.parseInt(request.getParameter("pageSize")))), Apply.class));
        return ResultUtil.success(map);
    }

    /**
     * extend space size check
     */
    public boolean applyCheck(String token, String spaceId) {
        if (StringUtils.isNotEmpty(spaceId)) {
            final boolean moreThan80 = moreThan80(spaceId);
            log.info("expansion check moreThan80 = " + moreThan80);
            spaceControlConfig.spatialVerification(spaceId, jwtTokenUtils.getEmail(token), Constants.SpaceRole.LEVEL_SENIOR);
            final boolean notPending = !(mongoTemplate.count(new Query().addCriteria(Criteria.where("spaceId").is(spaceId).and("type").is(Apply.TYPE_SPACE_EXPAND).and("approvedStates").is(Apply.APPROVED_NOT)), Apply.class) > 0);
            log.info("expansion check notPending = " + notPending);
            return notPending && moreThan80;
        } else {
            return false;
        }
    }

    /**
     * space size more than 80
     */
    public boolean moreThan80(String spaceId) {
        SpaceDataStatistic spaceDataStatistic = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("spaceId").is(spaceId)), SpaceDataStatistic.class);
        if (null != spaceDataStatistic) {
            return ((double) spaceDataStatistic.getDataSize() / (double) spaceDataStatistic.getCapacity()) >= 0.8;
        }
        return false;
    }

    /**
     * create an apply
     */
    public ResponseResult<Object> createApply(String token, Apply apply) {
        Token user = jwtTokenUtils.getToken(token);
        ConsumerDO consumerDO = userRepository.findById(user.getUserId()).get();
        // The following is the space expansion, and the application to open the space is in createSpace
        String spaceId = apply.getSpaceId();
        spaceControlConfig.spatialVerification(spaceId, user.getEmailAccounts(), Constants.SpaceRole.LEVEL_SENIOR);
        Space space = spaceRepository.findById(spaceId).get();
        applyCheck(token, spaceId);
        // socket message notify
        Map<String, Object> msgMap = new HashMap<>(16);
        msgMap.put("title", messageInternational("PENDING_APPROVAL_REMINDER"));
        msgMap.put("content", "用户： " + consumerDO.getName() + "(" + consumerDO.getEmailAccounts() + ") 申请扩容空间（" + space.getSpaceName() + "），需要您去审批");
        msgUtil.sendAdminMsg(msgUtil.mapToString(msgMap));
        // log record
        svnSpaceLogRepository.save(SpaceSvnLog.builder().spaceSvnId(generateSnowflake()).spaceId(spaceId).action(SpaceSvnLog.ACTION_OTHER).description(consumerDO.getName() + " 申请扩容 " + apply.getSize() + " GB").operatorId(user.getUserId()).operator(new Operator(consumerDO)).version(-2).createTime(new Date()).build());
        // creat apply
        String content = "用户" + consumerDO.getName() + "（" + consumerDO.getEmailAccounts() + "）申请扩容空间（" + space.getSpaceName() + "）";
        apply.setContent(content);
        apply.setDescription(apply.getDescription());
        apply.setApplyId(generateSnowflake());
        apply.setSubmitDate(getCurrentDateTimeString());
        apply.setApplicant(new Person(consumerDO));
        apply.setApprovedStates(Apply.APPROVED_NOT);
        applyRepository.save(apply);
        // message
        String linkUrl = spaceUrl.getApplyUrl();
        messageService.sendToAdmin(TITLE_PENDING, content, consumerDO, linkUrl);
        // Send emails to all administrators
        sendAdminEmail(apply.getType(), space.getSpaceName(), consumerDO);
        return ResultUtil.success(messageInternational("APPLY_SUCCESSFULLY"));
    }

    /**
     * send all of admin email
     */
    public void sendAdminEmail(String type, String spaceName, ConsumerDO consumerDO) {
        EmailModel emailType = StringUtils.equals(type, Apply.TYPE_SPACE_APPLY) ? EmailModel.EMAIL_APPLY() : EmailModel.EMAIL_CAPACITY();
        String emailTy = StringUtils.equals(type, Apply.TYPE_SPACE_APPLY) ? EmailRole.SPACE_CREATE_APPLY : EmailRole.SPACE_CAPACITY_APPLY;
        emailType.setMessage(emailType.getMessage().replaceAll("spaceName", spaceName).replaceAll("email", consumerDO.getEmailAccounts()));
        List<ConsumerDO> consumerDOS = mongoTemplate.find(new Query().addCriteria(Criteria.where("roles").is(ADMIN)), ConsumerDO.class);
        String[] strArray2 = new String[consumerDOS.size()];
        for (int i = 0; i < consumerDOS.size(); i++) {
            strArray2[i] = consumerDOS.get(i).getEmailAccounts();
        }
        Map<String, Object> attachment = new HashMap<>(16);
        attachment.put("name", consumerDO.getName());
        attachment.put("url", spaceUrl.getAuditUrl());
        attachment.put("email", strArray2);
        asyncDeal.send(attachment, emailType, emailTy);
    }

    /**
     * approve list
     */
    public ResponseResult<Object> approveList(String token, HttpServletRequest request) {
        int pageOffset = 0;
        int pageSize = 0;
        Query query = new Query();
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            if ("pageOffset".equals(entry.getKey()) && (StringUtils.isNotBlank(request.getParameter("pageOffset")))) {
                pageOffset = Integer.parseInt(request.getParameter("pageOffset"));
            } else if ("pageSize".equals(entry.getKey()) && (StringUtils.isNotBlank(request.getParameter("pageSize")))) {
                pageSize = Integer.parseInt(request.getParameter("pageSize"));
            } else if (StringUtils.isNotBlank(request.getParameter(entry.getKey()))) {
                query.addCriteria(Criteria.where(entry.getKey()).regex(Pattern.compile("^.*" + CommonUtils.escapeExprSpecialWord(entry.getValue()[0]) + ".*$", Pattern.CASE_INSENSITIVE)));
            }
        }
        // if ("submitDateStart".equals(entry.getKey()) && (StringUtils.isNotBlank(request.getParameter("submitDateStart")))) {
        // query.addCriteria(Criteria.where("submitDate").gte(request.getParameter("submitDateStart"))
        // .andOperator(Criteria.where("submitDate").lte(request.getParameter("submitDateEnd"))));
        // } else
        query.addCriteria(Criteria.where("type").ne(TYPE_SPACE_JOIN));
        Map<String, Object> content = new HashMap<>(16);
        Map<String, Object> count = new HashMap<>(16);
        count.put("total", mongoTemplate.count(query, Apply.class));
        content.put("content", mongoTemplate.find(query.with(PageRequest.of(pageOffset, pageSize)).with(Sort.by(Sort.Direction.DESC, "submitDate")), Apply.class));
        count.put("pending", mongoTemplate.count(new Query().addCriteria(Criteria.where("approvedStates").is(Apply.APPROVED_NOT).and("type").ne(TYPE_SPACE_JOIN)), Apply.class));
        count.put("pass", mongoTemplate.count(new Query().addCriteria(Criteria.where("approvedStates").is(Apply.APPROVED_PASS).and("type").ne(TYPE_SPACE_JOIN)), Apply.class));
        content.put("count", count);
        return ResultUtil.success(content);
    }

    /**
     * approve apply manage
     */
    public ResponseResult<Object> approve(String token, String applyId, String result, String opinion, String size) {
        Token userToken = jwtTokenUtils.getToken(token);
        Apply apply = applyRepository.findById(applyId).get();
        if (StringUtils.equals(apply.getApprovedStates(), Apply.APPROVED_PASS)) {
            return ResultUtil.error("该申请已经审批过，不支持修改");
        }
        ConsumerDO consumerDO = userRepository.findById(apply.getApplicant().getPersonId()).get();
        Map<String, Object> msgMap = new HashMap<>(16);
        if (!StringUtils.equals(Apply.RESULT_PASS, result)) {
            if (StringUtils.isBlank(opinion)) {
                return ResultUtil.error("驳回意见不能为空");
            }
            apply.setReject(opinion);
            apply.setApprovedResult(Apply.RESULT_NOT_PASS);
            msgMap.put("title", "申请未通过审批");
            String content;
            if (StringUtils.equals(apply.getType(), Apply.TYPE_SPACE_APPLY)) {
                content = "您开通空间的申请未通过，未通过原因：";
                // reject create space and delete it
                spaceService.deleteSpace(token, apply.getSpaceId());
            } else {
                content = "您申请的空间扩容申请未通过，未通过原因：";
            }
            msgMap.put("content", content + opinion + "，如仍有需要请联系管理员");
            msgUtil.sendMsg(apply.getApplicant().getEmail(), msgUtil.mapToString(msgMap));
            sendEmailError(apply.getApplicant().getPersonName(), apply.getApplicant().getEmail(), apply.getSpaceName(), apply.getDescription(), opinion);
        } else {
            final Long defaultSize = spaceService.spaceSizeFromApproveSetting();
            if (StringUtils.equals(apply.getType(), Apply.TYPE_SPACE_EXPAND)) {
                Space space = spaceRepository.findById(apply.getSpaceId()).get();
                space.setSpaceSize(space.getSpaceSize() + SIZE_1G * Integer.parseInt(size));
                spaceRepository.save(space);
                spaceControlConfig.spaceCapacityUpdate(space.getSpaceId(), space.getSpaceSize(), false);
                msgMap.put("title", messageInternational("APPLY_APPROVED"));
                msgMap.put("content", "您的空间（" + apply.getSpaceName() + "）已扩容 " + Integer.parseInt(size) + "GB");
                msgUtil.sendMsg(apply.getApplicant().getEmail(), msgUtil.mapToString(msgMap));
                svnSpaceLogRepository.save(SpaceSvnLog.builder().spaceSvnId(generateSnowflake()).spaceId(apply.getSpaceId()).action(SpaceSvnLog.ACTION_OTHER).description("扩容申请审批通过").operatorId(userToken.getUserId()).operator(new Operator(consumerDO)).version(-2).createTime(new Date()).build());
                SpaceSizeControl.updateCapacity(space.getSpaceId(), space.getSpaceSize());
            } else {
                // create space
                msgMap.put("title", messageInternational("APPLY_APPROVED"));
                msgMap.put("content", "您创建空间（空间名称）的申请已通过管理员审批");
                msgUtil.sendMsg(apply.getApplicant().getEmail(), msgUtil.mapToString(msgMap));
                Space space = spaceRepository.findById(apply.getSpaceId()).get();
                space.setSpaceSize(StringUtils.isNotBlank(size) ? SIZE_1G * Long.parseLong(size) : defaultSize);
                space.setState("1");
                spaceRepository.save(space);
                spaceControlConfig.spaceCapacityUpdate(space.getSpaceId(), space.getSpaceSize(), true);
                versionUpdate.setSpaceType(space);
                SpaceSizeControl.addSpace(space.getSpaceId(), space.getSpaceSize(), 0L);
                // Add spatial database mapping index
                createMongoIndex.createSpaceFileMappingIndex(space.getSpaceId());
                cacheLoading.clearSimple(space.getSpaceId());
            }
            apply.setApprovedResult(Apply.RESULT_PASS);
            sendEmailSuccess(apply.getApplicant().getPersonName(), apply.getApplicant().getEmail(), apply.getSpaceName(), StringUtils.isBlank(size) ? defaultSize : Long.parseLong(size), apply.getType(), apply.getSpaceId());
        }
        final Person approver = new Person(userToken);
        apply.setApprover(approver);
        apply.setCompletedDate(getCurrentDateTimeString());
        apply.setApprovedStates(Apply.APPROVED_PASS);
        applyRepository.save(apply);
        StringBuilder stringBuffer = new StringBuilder();
        if (StringUtils.equals(apply.getType(), Apply.TYPE_SPACE_EXPAND)) {
            stringBuffer.append("空间（" + apply.getSpaceName() + "）的扩容申请");
        } else {
            stringBuffer.append("您创建空间（" + apply.getSpaceName() + "）的申请");
        }
        if (!StringUtils.equals(Apply.RESULT_PASS, result)) {
            stringBuffer.append("未通过，未通过原因：" + opinion + " 如仍有需要，请联系管理员");
            messageService.sendToApplicant(Message.TITLE_NOT_PASSED, stringBuffer.toString(), approver, apply.getApplicant(), 0);
        } else {
            if (StringUtils.equals(apply.getType(), Apply.TYPE_SPACE_EXPAND)) {
                stringBuffer.append("已通过，扩容" + apply.getSize() + " GB");
            } else {
                stringBuffer.append(messageInternational("APPROVED"));
            }
            messageService.sendToApplicant(Message.TITLE_PASSED, stringBuffer.toString(), approver, apply.getApplicant(), 1);
        }
        return ResultUtil.successMsg(messageInternational("APPROVAL_COMPLETED"));
    }

    /**
     * get basic setting
     */
    public ResponseResult<Object> basic() {
        BasicSetting setting = mongoTemplate.findOne(new Query(), BasicSetting.class);
        if (Objects.isNull(setting)) {
            setting = new BasicSetting();
            setting.setTopic("黑色");
            setting.setDataSpaceName("数据协同管理工具");
        }
        return ResultUtil.success(setting);
    }

    /**
     * basic setting set
     */
    public ResponseResult<Object> setBasic(String token, BasicSetting basicSetting) {
        String dataSpaceName = basicSetting.getDataSpaceName();
        if (StringUtils.isNotEmpty(dataSpaceName)) {
            if (isSpecialChar(dataSpaceName)) {
                return ResultUtil.error("DataSpace名称含有特殊字符: " + takeOutChar(dataSpaceName));
            }
        } else {
            return ResultUtil.error("请填写DataSpace名称!");
        }
        String indexTitle = basicSetting.getIndexTitle();
        if (StringUtils.isEmpty(indexTitle)) {
            return ResultUtil.error("请填写首页标题!");
        }
        String indexDescription = basicSetting.getIndexDescription();
        if (StringUtils.isEmpty(indexDescription)) {
            return ResultUtil.error("请填写首页描述!");
        }
        String topic = basicSetting.getTopic();
        if (StringUtils.isEmpty(topic)) {
            return ResultUtil.error("请设置空间主题!");
        }
        String logo = basicSetting.getLogo();
        if (StringUtils.isNotEmpty(logo)) {
            if (!isPicBase(logo)) {
                return ResultUtil.error("系统logo 请上传图片格式");
            }
        } else {
            return ResultUtil.error("请配置系统logo!");
        }
        List<String> banners = basicSetting.getBanners();
        if (null != banners && !banners.isEmpty()) {
            for (String banner : banners) {
                if (!isPicBase(banner)) {
                    return ResultUtil.error("首页Banner 请上传图片格式");
                }
            }
        } else {
            return ResultUtil.error("请配置首页Banner!");
        }
        BeanUtils.copyProperties(basicSetting, Objects.requireNonNull(mongoTemplate.findOne(new Query(), BasicSetting.class)));
        mongoTemplate.save(basicSetting, "setting");
        config.invalidate(Constants.CaffeType.BASIS);
        return ResultUtil.success();
    }

    /**
     * User Insertion
     */
    public ResponseResult<Object> userList(int pageOffset, int pageSize, String name, String email, String org, String role) {
        Query query = new Query();
        if (StringUtils.isNotEmpty(name)) {
            Pattern pattern = Pattern.compile("^.*" + CommonUtils.escapeExprSpecialWord(name.trim()) + ".*$", Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where("name").in(pattern));
        }
        if (StringUtils.isNotEmpty(email)) {
            Pattern pattern = Pattern.compile("^.*" + CommonUtils.escapeExprSpecialWord(email.trim()) + ".*$", Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where("emailAccounts").in(pattern));
        }
        if (StringUtils.isNotEmpty(org)) {
            Pattern pattern = Pattern.compile("^.*" + CommonUtils.escapeExprSpecialWord(org.trim()) + ".*$", Pattern.CASE_INSENSITIVE);
            query.addCriteria(Criteria.where("orgChineseName").in(pattern));
        }
        if (StringUtils.isNotEmpty(role)) {
            query.addCriteria(Criteria.where("roles").in(role.trim()));
        }
        long count = mongoTemplate.count(query, UserServiceImpl.COLLECTION_NAME);
        List<UserShow> consumerDOS = null;
        if (count > 0) {
            query.with(PageRequest.of(pageOffset - 1, pageSize));
            query.with(Sort.by(Sort.Direction.DESC, "createTime"));
            consumerDOS = mongoTemplate.find(query, UserShow.class, UserServiceImpl.COLLECTION_NAME);
            for (UserShow consumerDO : consumerDOS) {
                if (null != consumerDO.getDisablePwd() && consumerDO.getDisablePwd() && consumerDO.getDisable() == 0) {
                    consumerDO.setDisable(2);
                }
            }
        }
        Map<String, Object> resultMap = new HashMap<>(16);
        resultMap.put("total", count);
        resultMap.put("data", consumerDOS);
        return ResultUtil.success(resultMap);
    }

    /**
     * Disable Enable Users
     */
    public ResponseResult<Object> disable(String userId, int disable) {
        Query query = new Query().addCriteria(Criteria.where("_id").is(userId));
        ConsumerDO one = mongoTemplate.findOne(query, ConsumerDO.class);
        if (one == null) {
            return ResultUtil.error("用户不存在!");
        }
        if (disable != 0 && disable != 1) {
            return ResultUtil.errorInternational("GENERAL_PARAMETER_ERROR");
        }
        Update update = new Update();
        // Disabled
        update.set("disable", disable);
        mongoTemplate.upsert(query, update, UserServiceImpl.COLLECTION_NAME);
        if (disable == 1) {
            // Clean cache
            cacheLoading.clearUserCaffeine(one.getId(), one.getEmailAccounts());
        }
        return ResultUtil.success();
    }

    /**
     * Add users to the page
     */
    public ResponseResult<Object> adminUserAdd(String token, ManualAdd manualAdd) {
        Token user = jwtTokenUtils.getToken(token);
        List<String> validation = validation(manualAdd);
        if (validation.size() > 0) {
            return ResultUtil.error(validation.toString());
        }
        String email = RSAEncrypt.decrypt(manualAdd.getEmailAccounts());
        // Email verification
        if (!isEmail(email)) {
            return ResultUtil.error("请输入正确的邮箱地址!");
        }
        String name = RSAEncrypt.decrypt(manualAdd.getName());
        if (isSpecialChar(name)) {
            return ResultUtil.error("姓名不能带有特殊字符: " + takeOutChar(name != null ? name : ""));
        }
        if (isSpecialChar(manualAdd.getOrg())) {
            return ResultUtil.error("单位不能带有特殊字符: " + takeOutChar(manualAdd.getOrg()));
        }
        String res = publicData.getIfPresent(email + "_management_register");
        if (StringUtils.isNotEmpty(res)) {
            return ResultUtil.success();
        } else {
            publicData.put(email + "_management_register", CommonUtils.getDateTimeString(new Date()));
        }
        ConsumerDO consumerDO = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("emailAccounts").is(email)), ConsumerDO.class);
        if (consumerDO != null && consumerDO.getState() != 2) {
            return ResultUtil.error("该邮箱已注册");
        } else if (consumerDO == null) {
            consumerDO = new ConsumerDO();
        }
        CglibUtil.copy(manualAdd, consumerDO);
        consumerDO.setName(name);
        consumerDO.setEmailAccounts(email.trim());
        consumerDO.setState(1);
        consumerDO.setOrgChineseName(manualAdd.getOrg());
        consumerDO.setAddWay("页面添加");
        consumerDO.setCreateTime(LocalDateTime.now());
        mongoTemplate.save(consumerDO, UserServiceImpl.COLLECTION_NAME);
        // Send email
        EmailModel emailInvite = EmailModel.EMAIL_INVITE();
        long stringTime = new Date().getTime();
        String code = SMS4.Encryption(emailInvite.getType() + "&" + email + "&" + stringTime);
        Map<String, Object> param = new HashMap<>(16);
        param.put("name", user.getName());
        String call = emailInvite.getCall().replace("email", user.getEmailAccounts());
        emailInvite.setCall(call);
        param.put("email", email);
        param.put("url", spaceUrl.getCallHost() + "/api/ps.av?code=" + code);
        asyncDeal.send(param, emailInvite, emailInvite.getType());
        return ResultUtil.success();
    }

    /**
     * Role List
     */
    public ResponseResult<Object> roleList() {
        List<RoleDO> all = mongoTemplate.findAll(RoleDO.class);
        return ResultUtil.success(all);
    }

    public ResponseResult<Object> adminUserUpdate(String token, ManualAdd manualAdd) {
        if (manualAdd == null || StringUtils.isEmpty(manualAdd.getUserId())) {
            return ResultUtil.error("参数错误");
        }
        String name = RSAEncrypt.decrypt(manualAdd.getName());
        if (isSpecialChar(name)) {
            return ResultUtil.error("姓名不能带有特殊字符: " + takeOutChar(name));
        }
        if (isSpecialChar(manualAdd.getOrg())) {
            return ResultUtil.error("单位不能带有特殊字符: " + takeOutChar(manualAdd.getOrg()));
        }
        Query query = new Query().addCriteria(Criteria.where("_id").is(manualAdd.getUserId()));
        ConsumerDO one = mongoTemplate.findOne(query, ConsumerDO.class);
        if (one == null) {
            return ResultUtil.error("用户不存在!");
        }
        Update update = new Update();
        update.set("name", name);
        if (manualAdd.getRoles() != null && manualAdd.getRoles().size() > 0) {
            update.set("roles", manualAdd.getRoles());
            // Clear account online users
            List<String> tokenList = emailToken.getIfPresent(one.getEmailAccounts());
            if (null != tokenList && tokenList.size() > 0) {
                for (String t : tokenList) {
                    tokenCache.invalidate(t);
                }
                emailToken.invalidate(one.getEmailAccounts());
            }
        }
        update.set("orgChineseName", manualAdd.getOrg());
        mongoTemplate.upsert(query, update, UserServiceImpl.COLLECTION_NAME);
        String updateName = null;
        if (!one.getName().equals(name)) {
            updateName = name;
        }
        if (null != updateName) {
            asyncDeal.userinfoUpdate(one.getId(), null, updateName);
        }
        return ResultUtil.success();
    }

    public ResponseResult<Object> identify(Identify identify) {
        if (identify == null || StringUtils.isEmpty(identify.getSuffix())) {
            return ResultUtil.error("参数错误");
        }
        Query query = new Query().addCriteria(Criteria.where("suffix").is(identify.getSuffix()));
        Identify one = mongoTemplate.findOne(query, Identify.class);
        if (one != null) {
            identify.setId(one.getId());
            identify.setCreateTime(one.getCreateTime());
        } else {
            identify.setCreateTime(new Date());
        }
        mongoTemplate.save(identify);
        return ResultUtil.success();
    }

    public ResponseResult<Object> identifyDelete(String id) {
        Query query = new Query().addCriteria(Criteria.where("_id").is(id));
        mongoTemplate.remove(query, Identify.class);
        return ResultUtil.success();
    }

    public ResponseResult<Object> identifyQuery(int pageOffset, int pageSize) {
        Query query = new Query();
        long count = mongoTemplate.count(query, Identify.class);
        List<Identify> identifies = null;
        if (count > 0) {
            query.with(PageRequest.of(pageOffset - 1, pageSize));
            query.with(Sort.by(Sort.Direction.DESC, "createTime"));
            identifies = mongoTemplate.find(query, Identify.class);
        }
        Map<String, Object> resultMap = new HashMap<>(16);
        resultMap.put("total", count);
        resultMap.put("data", identifies);
        return ResultUtil.success(resultMap);
    }

    public ResponseResult<Object> releaseAccountQuery(int pageOffset, int pageSize) {
        Query query = new Query();
        long count = mongoTemplate.count(query, ReleaseAccount.class);
        List<ReleaseAccount> identifies = null;
        if (count > 0) {
            query.with(PageRequest.of(pageOffset - 1, pageSize));
            query.with(Sort.by(Sort.Direction.DESC, "createTime"));
            identifies = mongoTemplate.find(query, ReleaseAccount.class);
        }
        Map<String, Object> resultMap = new HashMap<>(16);
        resultMap.put("total", count);
        resultMap.put("data", identifies);
        return ResultUtil.success(resultMap);
    }

    public ResponseResult<Object> releaseAccountAddOrUpdate(ReleaseAccount releaseAccount) {
        List<String> validation = validation(releaseAccount);
        if (validation.size() > 0) {
            return ResultUtil.error(validation.toString());
        }
        if (StringUtils.isEmpty(releaseAccount.getId())) {
            Query query = new Query().addCriteria(Criteria.where("orgId").is(releaseAccount.getOrgId()));
            query.with(Sort.by(Sort.Direction.DESC, "createTime"));
            ReleaseAccount one = mongoTemplate.findOne(query, ReleaseAccount.class);
            if (one != null) {
                return ResultUtil.error("该发布机构已存在发布账号");
            }
            releaseAccount.setCreateTime(new Date());
        } else {
            releaseAccount.setUpdateTime(new Date());
        }
        releaseAccountRepository.save(releaseAccount);
        config.invalidate(Constants.CaffeType.PUBLIC_ORG);
        return ResultUtil.success();
    }

    public ResponseResult<Object> releaseAccountDelete(String id) {
        releaseAccountRepository.deleteById(id);
        config.invalidate(Constants.CaffeType.PUBLIC_ORG);
        return ResultUtil.success();
    }

    public ResponseResult<Object> umtCheck(String token) {
        String userId = jwtTokenUtils.getUserIdFromToken(token);
        Query query = new Query().addCriteria(Criteria.where("_id").is(userId));
        ConsumerDO one = mongoTemplate.findOne(query, ConsumerDO.class);
        if (one != null) {
            String password = one.getPassword();
            return ResultUtil.success(StringUtils.isNotEmpty(password));
        }
        return ResultUtil.success(false);
    }

    public ResponseResult<Object> setAcc(String acc, String pwd, boolean isOpen) {
        String decrypt = RSAEncrypt.decrypt(pwd);
        if (StringUtils.isEmpty(decrypt)) {
            return ResultUtil.error("密码格式错误!");
        }
        // Call the main center
        Map<String, Object> paramMap = new HashMap<>(16);
        paramMap.put("account", acc);
        paramMap.put("password", decrypt);
        ResponseResult<Object> responseResult = publicQuery(paramMap);
        if (responseResult.getCode() != 0) {
            return responseResult;
        }
        Account account = new Account();
        account.setAccount(acc);
        account.setPassword(pwd);
        account.setIsOpen(isOpen);
        Map data = (Map) responseResult.getData();
        if (!data.containsKey("zh_Name")) {
            return ResultUtil.error("未找到相关机构!");
        }
        account.setOrgName(data.get("zh_Name").toString());
        mongoTemplate.remove(new Query(), Account.class);
        mongoTemplate.save(account);
        config.put(Constants.CaffeType.ACC, account);
        config.put(Constants.CaffeType.ACC_OPEN, account);
        return ResultUtil.success(account.getOrgName());
    }

    public ResponseResult<Object> setAccType(String id, boolean isOpen) {
        Query query = new Query().addCriteria(Criteria.where("_id").is(id));
        Account one = mongoTemplate.findOne(query, Account.class);
        if (one != null) {
            one.setIsOpen(isOpen);
            mongoTemplate.save(one);
            config.invalidate(Constants.CaffeType.ACC);
            config.invalidate(Constants.CaffeType.ACC_OPEN);
        }
        return ResultUtil.success();
    }

    public ResponseResult<Object> setAccQuery() {
        Object object = cacheLoading.loadingCenter();
        if (object != null) {
            Map<String, Object> map = new HashMap<>(16);
            Account account = (Account) object;
            map.put("id", account.getId());
            map.put("org", account.getOrgName());
            map.put("isOpen", account.getIsOpen());
            return ResultUtil.success(map);
        } else {
            return ResultUtil.success(null);
        }
    }

    public ResponseResult<Object> umtGet() {
        List<UmtConf> all = mongoTemplate.findAll(UmtConf.class);
        if (all.size() > 0) {
            return ResultUtil.success(all.get(0));
        } else {
            return ResultUtil.success(null);
        }
    }

    public ResponseResult<Object> umtUpdate(String id, String appKey, String appSecret, String page, boolean isOpen) {
        UmtConf umtConf = null;
        if (StringUtils.isNotEmpty(id)) {
            UmtConf umt = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(id)), UmtConf.class);
            if (umt == null) {
                return ResultUtil.error("修改的信息未找到");
            }
            umtConf = umt;
            umtConf.setLastUpdateTime(new Date());
        } else {
            umtConf = new UmtConf();
            umtConf.setCreateTime(new Date());
        }
        umtConf.setAppKey(appKey);
        umtConf.setAppSecret(appSecret);
        umtConf.setHongPage(page);
        umtConf.setIsOpen(isOpen);
        mongoTemplate.save(umtConf);
        config.invalidate(Constants.LoginWay.UMP);
        return ResultUtil.success();
    }

    private ResponseResult<Object> publicQuery(Map<String, Object> paramMap) {
        HttpClient httpClient = new HttpClient();
        String result = "";
        try {
            result = httpClient.doPostJsonWayTwo(JSON.toJSONString(paramMap), spaceUrl.getCenterHost() + "/api/v1/findOrgByAccount");
        } catch (CommonException exception) {
            return ResultUtil.error("调用总中心接口失败: " + exception.getMsg());
        } catch (Exception e) {
            return ResultUtil.error("调用总中心接口失败!");
        }
        if (StringUtils.isEmpty(result)) {
            return ResultUtil.error("解析失败");
        }
        Map resultMap = JSONObject.parseObject(result, Map.class);
        if (resultMap == null) {
            return ResultUtil.error("解析失败");
        }
        int code = (int) resultMap.get("code");
        boolean success = (boolean) resultMap.get("success");
        if (code != 200 || !success) {
            return ResultUtil.error((String) resultMap.get("message"));
        }
        return ResultUtil.success(resultMap.get("result"));
    }

    public ResponseResult<Object> umtCk() {
        Object umtObject = cacheLoading.loadingUmt();
        Object wechatObject = cacheLoading.loadingWechat();
        Object escObject = cacheLoading.loadingEsc();
        Map<String, Object> resultMap = new HashMap<>();
        if (umtObject != null) {
            UmtConf umtConf = (UmtConf) umtObject;
            resultMap.put("umt", umtConf.getIsOpen());
        } else {
            resultMap.put("umt", false);
        }
        if (wechatObject != null) {
            WechatConf wechatConf = (WechatConf) wechatObject;
            resultMap.put("wechat", wechatConf.getIsOpen());
        } else {
            resultMap.put("wechat", false);
        }
        if (escObject != null) {
            EscConf escConf = (EscConf) escObject;
            resultMap.put("esc", escConf.getIsOpen());
        } else {
            resultMap.put("esc", false);
        }
        return ResultUtil.success(resultMap);
    }

    public ResponseResult<Object> netCheck() {
        Object obj = cacheLoading.loadingNet();
        if (obj != null) {
            NetworkConf umtConf = (NetworkConf) obj;
            return ResultUtil.success(umtConf.getIsOpen());
        } else {
            return ResultUtil.success(false);
        }
    }

    public ResponseResult<Object> netGet() {
        List<NetworkConf> all = mongoTemplate.findAll(NetworkConf.class);
        if (all.size() > 0) {
            return ResultUtil.success(all.get(0));
        } else {
            return ResultUtil.success(null);
        }
    }

    public ResponseResult<Object> netUpdate(String id, String appKey, String appSecret, String page, boolean isOpen) {
        NetworkConf networkConf = null;
        if (StringUtils.isNotEmpty(id)) {
            NetworkConf net = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(id)), NetworkConf.class);
            if (net == null) {
                return ResultUtil.error("修改的信息未找到");
            }
            networkConf = net;
            networkConf.setLastUpdateTime(new Date());
        } else {
            networkConf = new NetworkConf();
            networkConf.setCreateTime(new Date());
        }
        networkConf.setAppKey(appKey);
        networkConf.setSecretKey(appSecret);
        networkConf.setHongPage(page);
        networkConf.setIsOpen(isOpen);
        mongoTemplate.save(networkConf);
        config.invalidate(Constants.LoginWay.NETWORK);
        return ResultUtil.success();
    }

    public ResponseResult<Object> picture(MultipartFile blobAvatar) throws IOException {
        String fileName = blobAvatar.getOriginalFilename();
        String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);
        if (!isPic(suffix)) {
            return ResultUtil.error("请上传图片格式");
        }
        File dir = new File("/data/spaceLogo");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String uuid = generateUUID();
        File filePath = new File(dir, uuid + "." + suffix);
        blobAvatar.transferTo(filePath);
        String logoPath = filePath.getPath().replaceAll("/data/spaceLogo", "");
        // Storage
        BasicSetting one = mongoTemplate.findOne(new Query(), BasicSetting.class);
        one.setBanner(logoPath);
        mongoTemplate.save(one);
        return ResultUtil.success(logoPath);
    }

    public ResponseResult<Object> usGet(String userId) {
        Query query = new Query().addCriteria(Criteria.where("_id").is(userId));
        UserShow one = mongoTemplate.findOne(query, UserShow.class, UserServiceImpl.COLLECTION_NAME);
        return ResultUtil.success(one);
    }

    private void sendEmailSuccess(String name, String email, String spaceName, long size, String type, String spaceId) {
        EmailModel emailSuccess = EmailModel.EMAIL_SUCCESS();
        Map<String, Object> param = new HashMap<>(16);
        String spaceDeal = spaceUrl.getSpaceDetailUrl().replaceAll("spaceId", spaceId);
        if (StringUtils.equals(type, Apply.TYPE_SPACE_EXPAND)) {
            emailSuccess.setMessage("您的空间（" + spaceName + "）已扩容" + size + "GB");
            emailSuccess.setAlert("请点击下方按钮进入空间");
            emailSuccess.setButton("进入空间");
        } else {
            emailSuccess.setMessage(emailSuccess.getMessage().replaceAll("spaceName", spaceName));
        }
        param.put("name", name);
        param.put("url", spaceDeal);
        param.put("email", email);
        String emailTy = StringUtils.equals(type, Apply.TYPE_SPACE_APPLY) ? EmailRole.SPACE_CREATE_APPLY : EmailRole.SPACE_CAPACITY_APPLY;
        asyncDeal.send(param, emailSuccess, emailTy);
    }

    private void sendEmailError(String name, String email, String spaceName, String type, String opinion) {
        EmailModel emailSuccess = EmailModel.EMAIL_ERROR();
        Map<String, Object> param = new HashMap<>(16);
        if (StringUtils.equals(type, Apply.TYPE_SPACE_EXPAND)) {
            emailSuccess.setMessage("很遗憾您的空间（" + spaceName + "）扩容申请未通过，未通过原因：");
            emailSuccess.setAlert(opinion);
        } else {
            emailSuccess.setAlert(opinion);
            emailSuccess.setMessage(emailSuccess.getMessage().replaceAll("spaceName", spaceName));
        }
        param.put("name", name);
        param.put("email", email);
        String emailTy = StringUtils.equals(type, Apply.TYPE_SPACE_APPLY) ? EmailRole.SPACE_CREATE_APPLY : EmailRole.SPACE_CAPACITY_APPLY;
        asyncDeal.send(param, emailSuccess, emailTy);
    }

    public ResponseResult<Object> userDelete(String token, String userId) {
        if (StringUtils.isEmpty(userId.trim())) {
            return ResultUtil.error("参数错误!");
        }
        Query query = new Query().addCriteria(Criteria.where("_id").is(userId));
        ConsumerDO user = mongoTemplate.findOne(query, ConsumerDO.class);
        if (user != null) {
            String email = user.getEmailAccounts();
            // Physical Operations
            asyncMethod.deleteUser(user, spaceUrl);
            // Clean cache
            cacheLoading.clearUserCaffeine(user.getId(), user.getEmailAccounts());
            // Clear socket
            SocketManager.removeByEmail(email);
        }
        return ResultUtil.success();
    }

    public ResponseResult<Object> wechatGet() {
        List<WechatConf> all = mongoTemplate.findAll(WechatConf.class);
        if (all.size() > 0) {
            return ResultUtil.success(all.get(0));
        } else {
            return ResultUtil.success(null);
        }
    }

    public ResponseResult<Object> wechatUpdate(String id, String appId, String secretKey, String page, boolean isOpen) {
        WechatConf wechatConf = null;
        if (StringUtils.isNotEmpty(id)) {
            WechatConf weConf = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(id)), WechatConf.class);
            if (weConf == null) {
                return ResultUtil.error("修改的信息未找到");
            }
            wechatConf = weConf;
            wechatConf.setLastUpdateTime(new Date());
        } else {
            wechatConf = new WechatConf();
            wechatConf.setCreateTime(new Date());
        }
        wechatConf.setAppId(appId);
        wechatConf.setSecretKey(secretKey);
        wechatConf.setHongPage(page);
        wechatConf.setIsOpen(isOpen);
        mongoTemplate.save(wechatConf);
        config.invalidate(Constants.LoginWay.WECHAT);
        return ResultUtil.success();
    }

    public ResponseResult<Object> escGet() {
        List<EscConf> all = mongoTemplate.findAll(EscConf.class);
        if (all.size() > 0) {
            return ResultUtil.success(all.get(0));
        } else {
            return ResultUtil.success(null);
        }
    }

    public ResponseResult<Object> escUpdate(String id, String clientId, String clientSecret, String page, boolean isOpen) {
        EscConf escConf = null;
        if (StringUtils.isNotEmpty(id)) {
            EscConf esConf = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(id)), EscConf.class);
            if (esConf == null) {
                return ResultUtil.error("修改的信息未找到");
            }
            escConf = esConf;
            escConf.setLastUpdateTime(new Date());
        } else {
            escConf = new EscConf();
            escConf.setCreateTime(new Date());
        }
        escConf.setClientId(clientId);
        escConf.setClientSecret(clientSecret);
        escConf.setHongPage(page);
        escConf.setIsOpen(isOpen);
        mongoTemplate.save(escConf);
        config.invalidate(Constants.LoginWay.ESCIENCE);
        return ResultUtil.success();
    }

    /**
     * Personal WeChat binding information
     */
    public ResponseResult<Object> weConf(String token) {
        String userIdFromToken = jwtTokenUtils.getUserIdFromToken(token);
        Query query = new Query().addCriteria(Criteria.where("_id").is(userIdFromToken));
        ConsumerDO consumerDO = mongoTemplate.findOne(query, ConsumerDO.class);
        if (null == consumerDO) {
            return ResultUtil.success(new HashMap<>(0));
        }
        if (StringUtils.isEmpty(consumerDO.getWechatId())) {
            return ResultUtil.success(new HashMap<>(0));
        } else {
            WechatUser onionId = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("unionId").is(consumerDO.getWechatId())), WechatUser.class);
            return ResultUtil.success(onionId);
        }
    }

    /**
     * Unbind WeChat
     */
    public ResponseResult<Object> weRelieve(String token) {
        String userIdFromToken = jwtTokenUtils.getUserIdFromToken(token);
        Query query = new Query().addCriteria(Criteria.where("_id").is(userIdFromToken));
        ConsumerDO consumerDO = mongoTemplate.findOne(query, ConsumerDO.class);
        if (null == consumerDO) {
            return ResultUtil.errorInternational("AUTH_USER_NOT_FOUND");
        }
        if (consumerDO.getState() != 1) {
            return ResultUtil.errorInternational("USER_UNACTIVATED");
        }
        if (StringUtils.isNotEmpty(consumerDO.getWechatId())) {
            consumerDO.setWechatId(null);
            mongoTemplate.save(consumerDO);
            return ResultUtil.success();
        }
        return ResultUtil.errorInternational("SS_WECHAT_RE");
    }
}
