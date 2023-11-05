package cn.cnic.dataspace.api.cacheLoading;

import cn.cnic.dataspace.api.model.apply.SpaceApply;
import cn.cnic.dataspace.api.model.center.Account;
import cn.cnic.dataspace.api.model.manage.BasicSetting;
import cn.cnic.dataspace.api.model.manage.ReleaseAccount;
import cn.cnic.dataspace.api.model.manage.SystemConf;
import cn.cnic.dataspace.api.model.network.NetworkConf;
import cn.cnic.dataspace.api.model.space.*;
import cn.cnic.dataspace.api.model.space.child.AuthorizationPerson;
import cn.cnic.dataspace.api.model.space.child.Operator;
import cn.cnic.dataspace.api.model.email.EmailRole;
import cn.cnic.dataspace.api.model.open.AppAuthApi;
import cn.cnic.dataspace.api.model.open.Application;
import cn.cnic.dataspace.api.model.email.SysEmail;
import cn.cnic.dataspace.api.model.login.EscConf;
import cn.cnic.dataspace.api.model.login.UmtConf;
import cn.cnic.dataspace.api.model.login.WechatConf;
import cn.cnic.dataspace.api.model.open.OpenApi;
import cn.cnic.dataspace.api.model.user.ConsumerDO;
import cn.cnic.dataspace.api.model.user.UserEmailRole;
import cn.cnic.dataspace.api.util.CaffeineUtil;
import cn.cnic.dataspace.api.util.Constants;
import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Caching data - technology cloud accounts, etc
 */
@Component
public class CacheLoading {

    @Autowired
    private MongoTemplate mongoTemplate;

    public CacheLoading(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public CacheLoading() {
    }

    private final Cache<String, String> tokenCache = CaffeineUtil.getTokenCache();

    private final Cache<String, List<String>> emailToken = CaffeineUtil.getEmailToken();

    private final Cache<String, Object> config = CaffeineUtil.getConfig();

    private final Cache<String, List<String>> applySpace = CaffeineUtil.getApplySpace();

    private final Cache<String, Application> application = CaffeineUtil.getApplication();

    private final Cache<String, AppAuthApi> authApiCache = CaffeineUtil.getAuthOpenapi();

    private final Cache<String, List<String>> spaceAuth = CaffeineUtil.getSpaceAuth();

    private final Cache<String, List<String>> userEmailRole = CaffeineUtil.getUserEmailRole();

    private final Cache<String, String> spaceShort = CaffeineUtil.getSpaceShort();

    private final Cache<String, Operator> userLog = CaffeineUtil.getUserLog();

    private final Cache<String, SpaceSimple> spaceSimple = CaffeineUtil.getSpaceSimple();

    private final Cache<String, List<String>> spaceMenRole = CaffeineUtil.getSpaceMenRole();

    private final Cache<String, String> spaceUserRole = CaffeineUtil.getSpaceUserRole();

    public static final Cache<String, Date> fileCreateDate = Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).build();

    /**
     * Load technology cloud configuration
     */
    public Object loadingUmt() {
        Object umt = config.getIfPresent(Constants.LoginWay.UMP);
        if (umt == null) {
            // Load Cache
            List<UmtConf> all = mongoTemplate.findAll(UmtConf.class);
            if (all.size() > 0) {
                UmtConf umtConf = all.get(0);
                if (umtConf.getIsOpen()) {
                    config.put(Constants.LoginWay.UMP, umtConf);
                    umt = umtConf;
                }
            }
        }
        return umt;
    }

    // Network disk configuration
    public Object loadingNet() {
        Object net = config.getIfPresent(Constants.LoginWay.NETWORK);
        if (net == null) {
            List<NetworkConf> all = mongoTemplate.findAll(NetworkConf.class);
            if (all.size() > 0) {
                NetworkConf networkConf = all.get(0);
                if (networkConf.getIsOpen()) {
                    config.put(Constants.LoginWay.NETWORK, networkConf);
                    net = networkConf;
                }
            }
        }
        return net;
    }

    // WeChat configuration
    public Object loadingWechat() {
        Object wechat = config.getIfPresent(Constants.LoginWay.WECHAT);
        if (wechat == null) {
            List<WechatConf> all = mongoTemplate.findAll(WechatConf.class);
            if (all.size() > 0) {
                WechatConf wechatConf = all.get(0);
                if (wechatConf.getIsOpen()) {
                    config.put(Constants.LoginWay.WECHAT, wechatConf);
                    wechat = wechatConf;
                }
            }
        }
        return wechat;
    }

    // Shared Network
    public Object loadingEsc() {
        Object esc = config.getIfPresent(Constants.LoginWay.ESCIENCE);
        if (esc == null) {
            List<EscConf> all = mongoTemplate.findAll(EscConf.class);
            if (all.size() > 0) {
                EscConf escConf = all.get(0);
                if (escConf.getIsOpen()) {
                    config.put(Constants.LoginWay.ESCIENCE, escConf);
                    esc = escConf;
                }
            }
        }
        return esc;
    }

    // Central account
    public Object loadingCenter() {
        Object acc = config.getIfPresent(Constants.CaffeType.ACC_OPEN);
        if (acc == null) {
            List<Account> all = mongoTemplate.findAll(Account.class);
            if (all.size() > 0) {
                Account account = all.get(0);
                if (account.getIsOpen()) {
                    config.put(Constants.CaffeType.ACC_OPEN, account);
                }
                acc = account;
            }
        }
        return acc;
    }

    // Central account
    public Object loadingCenterOpen() {
        Object acc = config.getIfPresent(Constants.CaffeType.ACC);
        if (acc == null) {
            List<Account> all = mongoTemplate.findAll(Account.class);
            if (all.size() > 0) {
                Account account = all.get(0);
                if (account.getIsOpen()) {
                    config.put(Constants.CaffeType.ACC, account);
                    acc = account;
                }
            }
        }
        return acc;
    }

    public Object loadingOrg() {
        Object publicOrg = config.getIfPresent(Constants.CaffeType.PUBLIC_ORG);
        // Obtaining Authorization Authority
        if (publicOrg == null) {
            List<ReleaseAccount> all = mongoTemplate.findAll(ReleaseAccount.class);
            if (all.size() > 0) {
                List<String> orgList = new ArrayList<>();
                for (ReleaseAccount releaseAccount : all) {
                    orgList.add(releaseAccount.getOrgId());
                }
                publicOrg = orgList;
                config.put(Constants.CaffeType.PUBLIC_ORG, orgList);
            }
        }
        return publicOrg;
    }

    // Loading System Configuration
    public Object loadingConfig() {
        Object basis = config.getIfPresent(Constants.CaffeType.BASIS);
        if (basis == null) {
            List<BasicSetting> all = mongoTemplate.findAll(BasicSetting.class);
            if (all.size() > 0) {
                BasicSetting basicSetting = all.get(0);
                Map<String, Object> map = new HashMap<>();
                map.put("dataSpaceName", basicSetting.getDataSpaceName());
                map.put("logo", basicSetting.getLogo());
                map.put("copyright", basicSetting.getCopyright());
                config.put(Constants.CaffeType.BASIS, map);
                basis = map;
            }
        }
        return basis;
    }

    /**
     * Space deletion - offline
     */
    public void clearSpaceCaffeine(String spaceId, String code) {
        // Space user permissions
        spaceAuth.invalidate(spaceId);
        // Clear Space Short Chains
        removeSpaceId(code);
        // Easy to clear space
        clearSimple(spaceId);
        // Clear Space Detailed User Role
        clearSpaceMenRole(spaceId);
        // Clear space user permission names
        clearSpaceRole(spaceId);
        // Delete FTP user cache
        CaffeineUtil.clearFtpUserId();
        CaffeineUtil.clearFtpShor();
    }

    /**
     * User Disabled - Delete
     */
    public void clearUserCaffeine(String userId, String email) {
        // Clear account online users
        List<String> tokenList = emailToken.getIfPresent(email);
        if (null != tokenList && tokenList.size() > 0) {
            for (String t : tokenList) {
                tokenCache.invalidate(t);
            }
            emailToken.invalidate(email);
        }
        // Delete FTP user cache
        CaffeineUtil.clearFtpUserId();
        CaffeineUtil.clearFtpShor();
        // Space application
        applySpace.invalidate(userId);
        // Space member roles
        clearSpaceRole(email);
        // Refine roles
        clearSpaceMenRole(email);
        removeUserEmailRole(email);
        clearOperator(email);
    }

    // Obtain records of personal application to join the space
    public List<String> getUserApplySpaces(String userId) {
        List<String> spaceIds = applySpace.getIfPresent(userId);
        if (null == spaceIds) {
            spaceIds = new ArrayList<>();
            List<SpaceApply> spaceApplies = mongoTemplate.find(new Query().addCriteria(Criteria.where("userId").is(userId).and("state").is(0)), SpaceApply.class);
            for (SpaceApply spaceApply : spaceApplies) {
                spaceIds.add(spaceApply.getSpaceId());
            }
            applySpace.put(userId, spaceIds);
        }
        return spaceIds;
    }

    // Add a record of personal application to join the space
    public void upUserApplySpaces(String userId, String spaceId) {
        List<String> spaceIds = applySpace.getIfPresent(userId);
        if (null == spaceIds) {
            spaceIds = new ArrayList<>();
            List<SpaceApply> spaceApplies = mongoTemplate.find(new Query().addCriteria(Criteria.where("userId").is(userId).and("state").is(0)), SpaceApply.class);
            for (SpaceApply spaceApply : spaceApplies) {
                spaceIds.add(spaceApply.getSpaceId());
            }
        }
        spaceIds.add(spaceId);
        applySpace.put(userId, spaceIds);
    }

    // Remove records of individual applications to join the space
    public void deleteUserApply(String userId, String spaceId) {
        List<String> spaceIds = applySpace.getIfPresent(userId);
        if (null != spaceIds) {
            spaceIds.remove(spaceId);
            applySpace.put(userId, spaceIds);
        }
    }

    // Load system mailbox
    public SysEmail getSysEmail() {
        Object sysEmailCaf = config.getIfPresent(Constants.CaffeType.SYS_EMAIL);
        if (null == sysEmailCaf) {
            SystemConf systemConf = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("type").is(Constants.CaffeType.SYS_EMAIL)), SystemConf.class);
            if (null != systemConf) {
                Object conf = systemConf.getConf();
                SysEmail sysEmail = JSONObject.parseObject(JSONObject.toJSONString(conf), SysEmail.class);
                config.put(Constants.CaffeType.SYS_EMAIL, sysEmail);
                return sysEmail;
            }
        }
        return (SysEmail) sysEmailCaf;
    }

    // Authorization Configuration
    public Application getApplication(String appKey) {
        Application app = application.getIfPresent(appKey);
        if (null == app) {
            Application appOne = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("appKey").is(Long.valueOf(appKey))), Application.class);
            if (null != appOne && appOne.getState() == 0) {
                application.put(appKey, appOne);
                return appOne;
            }
        }
        return app;
    }

    public void upDateApplication(Application app) {
        application.put(app.getAppKey().toString(), app);
    }

    public void delApplication(String appKey) {
        application.invalidate(appKey);
    }

    // Obtain application permissions
    public AppAuthApi getAppAuthPath(String appKey) {
        AppAuthApi ifPresent = authApiCache.getIfPresent(appKey);
        if (null == ifPresent) {
            Query query = new Query();
            query.addCriteria(Criteria.where("authApp.appKey").is(appKey).and("authApp.expire").is(false).and("state").is(Constants.OpenApiState.online));
            List<OpenApi> openApis = mongoTemplate.find(query, OpenApi.class);
            List<String> pathList = new ArrayList<>(openApis.size());
            for (OpenApi openApi : openApis) {
                pathList.add(openApi.getPath());
            }
            AppAuthApi appAuthApi = new AppAuthApi(appKey, pathList);
            authApiCache.put(appKey, appAuthApi);
            return appAuthApi;
        }
        return ifPresent;
    }

    public void clearAppAuthPath(String appKey) {
        authApiCache.invalidate(appKey);
    }

    // Space User Members - ()
    public List<String> getSpaceAuth(String spaceId) {
        List<String> ifPresent = spaceAuth.getIfPresent(spaceId);
        if (null == ifPresent) {
            Space space = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(spaceId)), Space.class);
            if (null != space && space.getState().equals("1")) {
                Set<AuthorizationPerson> authorizationList = space.getAuthorizationList();
                List<String> list = new ArrayList<>(authorizationList.size());
                for (AuthorizationPerson person : authorizationList) {
                    list.add(person.getUserId());
                }
                ifPresent = list;
                spaceAuth.put(spaceId, list);
            } else {
                ifPresent = new ArrayList<>(0);
            }
        }
        return ifPresent;
    }

    public void deleteSpaceAuth(String spaceId, String userId) {
        List<String> ifPresent = spaceAuth.getIfPresent(spaceId);
        if (null != ifPresent) {
            ifPresent.remove(userId);
            spaceAuth.put(spaceId, ifPresent);
        }
    }

    public void updateSpaceAuth(String spaceId, String userId) {
        List<String> ifPresent = spaceAuth.getIfPresent(spaceId);
        if (null != ifPresent) {
            if (!ifPresent.contains(userId)) {
                ifPresent.add(userId);
                spaceAuth.put(spaceId, ifPresent);
            }
        }
    }

    public List<String> getUserEmailRole(String email) {
        List<String> list = userEmailRole.getIfPresent(email);
        if (null == list) {
            ConsumerDO emailAccounts = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("emailAccounts").is(email)), ConsumerDO.class);
            if (null == emailAccounts) {
                list = Arrays.asList(EmailRole.list);
            } else {
                String id = emailAccounts.getId();
                UserEmailRole userEmail = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("userId").is(id)), UserEmailRole.class);
                if (null == userEmail) {
                    list = Arrays.asList(EmailRole.list);
                } else {
                    list = new ArrayList<>(5);
                    Map<String, Boolean> emailRole = userEmail.getEmailRole();
                    for (String key : emailRole.keySet()) {
                        Boolean aBoolean = emailRole.get(key);
                        if (aBoolean) {
                            list.add(key);
                        }
                    }
                }
            }
            userEmailRole.put(email, list);
        }
        return list;
    }

    public void removeUserEmailRole(String email) {
        userEmailRole.invalidate(email);
    }

    /**
     * Mapping of spatial storage identifiers and spatial IDs
     */
    public String getSpaceId(String code) {
        String spaceId = spaceShort.getIfPresent(code);
        if (null == spaceId) {
            Space space = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("spaceShort").is(code)), Space.class);
            if (null == space) {
                return null;
            }
            spaceShort.put(code, space.getSpaceId());
            spaceId = space.getSpaceId();
        }
        return spaceId;
    }

    public void removeSpaceId(String code) {
        spaceShort.invalidate(code);
    }

    /**
     * Obtain log user information
     */
    public Operator getOperator(String userEmail) {
        Operator operator = userLog.getIfPresent(userEmail);
        if (null == operator) {
            ConsumerDO emailAccounts = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("emailAccounts").is(userEmail)), ConsumerDO.class);
            operator = new Operator(emailAccounts);
            userLog.put(userEmail, operator);
        }
        return operator;
    }

    public void clearOperator(String userEmail) {
        userLog.invalidate(userEmail);
    }

    /**
     * Get simple space information
     */
    public SpaceSimple getSpaceSimple(String spaceId) {
        SpaceSimple simple = spaceSimple.getIfPresent(spaceId);
        if (null == simple) {
            Space space = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(spaceId)), Space.class);
            if (null != space) {
                simple = new SpaceSimple(space.getSpaceId(), space.getSpaceName(), space.getFilePath(), space.getState());
                spaceSimple.put(spaceId, simple);
            }
        }
        return simple;
    }

    public void clearSimple(String spaceId) {
        spaceSimple.invalidate(spaceId);
    }

    /**
     * Space member roles
     */
    public String getSpaceUserRole(String spaceId, String email) {
        String userRole = spaceUserRole.getIfPresent(spaceId + email);
        if (null == userRole) {
            Space space = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(spaceId)), Space.class);
            String role = "";
            Set<AuthorizationPerson> authorizationList = space.getAuthorizationList();
            for (AuthorizationPerson person : authorizationList) {
                if (person.getEmail().equals(email)) {
                    role = person.getRole();
                }
            }
            userRole = role;
            spaceUserRole.put(spaceId + email, role);
        }
        return userRole;
    }

    public void clearSpaceUserRole(String spaceId, String email) {
        spaceUserRole.invalidate(spaceId + email);
    }

    public void clearSpaceRole(String spaceId) {
        ConcurrentMap<@NonNull String, @NonNull String> stringListConcurrentMap = spaceUserRole.asMap();
        for (String key : stringListConcurrentMap.keySet()) {
            if (key.contains(spaceId)) {
                spaceUserRole.invalidate(key);
            }
        }
    }

    /**
     * Space member refinement permissions
     */
    public List<String> getSpaceMenRole(String spaceId, String email) {
        List<String> spaceRoleList = spaceMenRole.getIfPresent(spaceId + email);
        if (null == spaceRoleList) {
            String spaceUserRole = this.getSpaceUserRole(spaceId, email);
            if (StringUtils.isEmpty(spaceUserRole)) {
                return new ArrayList<>(0);
            }
            Query query = new Query().addCriteria(Criteria.where("spaceId").is(spaceId).and("roleName").is(spaceUserRole));
            SpaceRole one = mongoTemplate.findOne(query, SpaceRole.class);
            if (null == one) {
                spaceRoleList = new ArrayList<>(0);
            } else {
                spaceRoleList = one.getMenus();
            }
            spaceMenRole.put(spaceId + email, spaceRoleList);
        }
        return spaceRoleList;
    }

    public void clearSpaceMenRole(String spaceId) {
        ConcurrentMap<@NonNull String, @NonNull List<String>> stringListConcurrentMap = spaceMenRole.asMap();
        for (String key : stringListConcurrentMap.keySet()) {
            if (key.contains(spaceId)) {
                spaceMenRole.invalidate(key);
            }
        }
    }
}
