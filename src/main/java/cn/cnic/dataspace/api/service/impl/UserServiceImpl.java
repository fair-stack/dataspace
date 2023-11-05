package cn.cnic.dataspace.api.service.impl;

import cn.cnic.dataspace.api.asynchronous.AsyncDeal;
import cn.cnic.dataspace.api.cacheLoading.CacheLoading;
import cn.cnic.dataspace.api.model.manage.Identify;
import cn.cnic.dataspace.api.model.space.Space;
import cn.cnic.dataspace.api.model.space.child.AuthorizationPerson;
import cn.cnic.dataspace.api.model.email.EmailRole;
import cn.cnic.dataspace.api.model.user.*;
import cn.cnic.dataspace.api.service.UserService;
import cn.cnic.dataspace.api.service.space.SpaceService;
import cn.cnic.dataspace.api.util.*;
import cn.hutool.extra.cglib.CglibUtil;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import static cn.cnic.dataspace.api.util.CommonUtils.messageInternational;

/**
 * User service
 */
@Slf4j
@Service
@EnableAsync
public class UserServiceImpl implements UserService {

    public static final String COLLECTION_NAME = "db_user";

    private final Cache<String, String> tokenCache = CaffeineUtil.getTokenCache();

    private final Cache<String, String> spaceInvite = CaffeineUtil.getSpaceInvite();

    private final Cache<String, String> disable = CaffeineUtil.getDisable();

    private final Cache<String, Integer> errorPwd = CaffeineUtil.getErrorPwd();

    private final Cache<String, String> publicData = CaffeineUtil.getPublicData();

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private AsyncDeal asyncDeal;

    @Autowired
    private SpaceUrl spaceUrl;

    @Autowired
    private SpaceService spaceService;

    @Autowired
    private JwtTokenUtils jwtTokenUtils;

    @Override
    public ResponseResult<Object> add(ConsumerDTO consumerDTO, HttpServletRequest request) {
        if (consumerDTO == null) {
            return ResultUtil.errorInternational("USER_PARAMETER");
        }
        // Parameter verification
        List<String> validation = CommonUtils.validation(consumerDTO);
        if (validation.size() > 0) {
            return ResultUtil.error(validation.toString());
        }
        // Verification code verification
        String verificationCode = consumerDTO.getVerificationCode();
        HttpSession session = request.getSession();
        Object attribute = session.getAttribute(Constants.EmailSendType.EMAIL_CODE);
        if (attribute == null) {
            return ResultUtil.errorInternational("VERIFICATION_CODE");
        }
        if (!attribute.toString().equals(verificationCode.toLowerCase())) {
            return ResultUtil.errorInternational("VERIFICATION_CODE_ERROR");
        }
        // Decryption
        String emailDe = RSAEncrypt.decrypt(consumerDTO.getEmailAccounts());
        if (StringUtils.isNotEmpty(emailDe) && StringUtils.isNotEmpty(emailDe.trim())) {
            if (!CommonUtils.isEmail(emailDe.trim())) {
                return ResultUtil.errorInternational("USER_EMAIL");
            }
        } else {
            return ResultUtil.errorInternational("USER_EMAIL_ERROR");
        }
        String emailAccounts = emailDe.trim();
        String emailR = disable.getIfPresent(emailAccounts + "_login_r");
        if (null != emailR) {
            return ResultUtil.errorInternational("USER_DISABLE");
        }
        // Email verification
        String name = RSAEncrypt.decrypt(consumerDTO.getName());
        if (StringUtils.isNotEmpty(name)) {
            if (CommonUtils.isSpecialChar(name)) {
                return ResultUtil.error(messageInternational("USER_REAL_NAME") + CommonUtils.takeOutChar(name));
            }
        } else {
            return ResultUtil.errorInternational("USER_NAME_ERROR");
        }
        if (CommonUtils.isSpecialChar(consumerDTO.getOrg())) {
            return ResultUtil.error(messageInternational("USER_ORG_ERROR") + CommonUtils.takeOutChar(consumerDTO.getOrg()));
        }
        // password verifiers
        String password = RSAEncrypt.decrypt(consumerDTO.getPassword());
        String confirmPassword = RSAEncrypt.decrypt(consumerDTO.getConfirmPassword());
        if (StringUtils.isEmpty(password) || StringUtils.isEmpty(confirmPassword)) {
            return ResultUtil.errorInternational("PWD_ERROR");
        }
        if (!password.equals(confirmPassword)) {
            return ResultUtil.errorInternational("PWD_NOT_MATCH");
        }
        if (!CommonUtils.passVerify(password)) {
            return ResultUtil.errorInternational("PWD_STRENGTH");
        }
        String res = publicData.getIfPresent(emailAccounts + "_register");
        if (StringUtils.isNotEmpty(res)) {
            return ResultUtil.success();
        } else {
            publicData.put(emailAccounts + "_register", CommonUtils.getDateTimeString(new Date()));
        }
        ConsumerDO user1 = getUserInfoByName(emailAccounts);
        if (user1 != null && user1.getState() == 2 && StringUtils.isNotEmpty(consumerDTO.getCode())) {
            String code = consumerDTO.getCode();
            // Register through invitation code
            if (StringUtils.isEmpty(code)) {
                return ResultUtil.errorInternational("USER_CODE_EMPTY");
            }
            String decrypt = SMS4.Decrypt(code);
            if (decrypt == null) {
                return ResultUtil.errorInternational("USER_CODE_ERROR");
            }
            if (!decrypt.contains("&")) {
                return ResultUtil.errorInternational("USER_CODE_WRONG");
            }
            String[] split = decrypt.split("&");
            String type = split[0];
            String email = split[1];
            if (!user1.getEmailAccounts().equals(email)) {
                return ResultUtil.errorInternational("USER_EMAIL_WRONG");
            }
            String time = split[2];
            if (time.trim().equals("")) {
                return ResultUtil.errorInternational("USER_TIME_FORMAT");
            }
            Long activationTime = Long.valueOf(time);
            if (activationTime.longValue() < CommonUtils.yesterday(72)) {
                return ResultUtil.errorInternational("USER_CODE_DISABLE");
            }
            // Modify user status
            if (type.equals("spaceInvite")) {
                String spaceId = split[3];
                String sourUserId = split[4];
                String spaceRole = null;
                if (split.length > 5) {
                    spaceRole = split[5];
                }
                Query query = new Query();
                query.addCriteria(Criteria.where("emailAccounts").is(email));
                Update update = new Update();
                update.set("name", name);
                update.set("password", consumerDTO.getPassword());
                update.set("state", 1);
                update.set("orgChineseName", consumerDTO.getOrg());
                update.set("addWay", "空间邀请");
                // Determine email suffix
                List<Identify> all = mongoTemplate.findAll(Identify.class);
                if (all.size() > 0) {
                    boolean judge = true;
                    for (Identify identify : all) {
                        if (email.contains(identify.getSuffix())) {
                            update.set("roles", new ArrayList<String>() {

                                {
                                    add(Constants.SENIOR);
                                }
                            });
                            judge = false;
                            break;
                        }
                    }
                    if (judge) {
                        update.set("roles", new ArrayList<String>() {

                            {
                                add(Constants.GENERAL);
                            }
                        });
                    }
                } else {
                    update.set("roles", new ArrayList<String>() {

                        {
                            add(Constants.GENERAL);
                        }
                    });
                }
                mongoTemplate.upsert(query, update, COLLECTION_NAME);
                // Join Space
                spaceService.spaceInviteByEmail(spaceId, user1.getId(), sourUserId, spaceRole);
                return ResultUtil.success();
            }
        } else if (user1 != null && user1.getState() == 0) {
            sendEmail(name, emailAccounts, spaceUrl.getEmailActivation(), EmailModel.EMAIL_REGISTER());
            user1.setCreateTime(LocalDateTime.now());
            mongoTemplate.save(user1, COLLECTION_NAME);
            return ResultUtil.success();
        } else if (user1 != null && user1.getState() == 2) {
            // To be registered
            log.info("空间邀请的用户 页面注册!");
        } else if (user1 == null) {
            user1 = new ConsumerDO();
        } else {
            return ResultUtil.errorInternational("USER_EXIST");
        }
        Integer check = errorPwd.getIfPresent(emailAccounts + "_register");
        if (check != null) {
            if (check >= 5) {
                disable.put(emailAccounts + "_login_r", String.valueOf(check));
                return ResultUtil.errorInternational("USER_DISABLE");
            } else {
                errorPwd.put(emailAccounts + "_register", check + 1);
            }
        } else {
            errorPwd.put(check + "_login_r", 1);
        }
        consumerDTO.setName(name);
        consumerDTO.setEmailAccounts(emailAccounts);
        CglibUtil.copy(consumerDTO, user1);
        user1.setCreateTime(LocalDateTime.now());
        user1.setOrgChineseName(consumerDTO.getOrg());
        user1.setState(0);
        user1.setAddWay("注册");
        // Determine email suffix
        addRole(user1);
        sendEmail(name, emailAccounts, spaceUrl.getEmailActivation(), EmailModel.EMAIL_REGISTER());
        mongoTemplate.save(user1, COLLECTION_NAME);
        return ResultUtil.success();
    }

    private void sendEmail(String name, String emailAccounts, String url, EmailModel emailType) {
        // Verify email sending interval time
        String key = Constants.EmailSendType.REGISTER + emailAccounts;
        if (CaffeineUtil.checkEmailAging(key)) {
            return;
        }
        long stringTime = new Date().getTime();
        String code = SMS4.Encryption(emailType.getType() + "&" + emailAccounts + "&" + stringTime);
        Map<String, Object> param = new HashMap<>();
        param.put("name", name);
        param.put("email", emailAccounts);
        param.put("url", url + code);
        asyncDeal.send(param, emailType, emailType.getType());
        return;
    }

    @Override
    public ResponseResult<Object> update(String token, ConsumerInfoDTO user) {
        String email = tokenCache.getIfPresent(token);
        if (StringUtils.isEmpty(email)) {
            return ResultUtil.errorInternational("AUTH_TOKEN");
        }
        List<String> roles = jwtTokenUtils.getRoles(token);
        if (!roles.contains(Constants.ADMIN)) {
            String emailAccounts = user.getEmailAccounts();
            if (!email.equals(emailAccounts)) {
                return ResultUtil.errorInternational("USER_OTHER");
            }
        }
        Query query = new Query();
        query.addCriteria(Criteria.where("emailAccounts").is(email));
        ConsumerDO one = mongoTemplate.findOne(query, ConsumerDO.class);
        if (one == null) {
            return ResultUtil.errorInternational("USER_UNREGISTERED");
        }
        Update update = new Update();
        if (!StringUtils.isEmpty(user.getName())) {
            if (CommonUtils.isSpecialChar(user.getName())) {
                return ResultUtil.error(messageInternational("USER_NAME_CHAR") + CommonUtils.takeOutChar(user.getName()));
            }
            update.set("name", user.getName().trim());
        } else {
            return ResultUtil.errorInternational("USER_NAME");
        }
        if (!StringUtils.isEmpty(user.getEnglishName())) {
            if (CommonUtils.isSpecialChar(user.getEnglishName())) {
                return ResultUtil.error(messageInternational("USER_ENG_NAME_CHAR") + CommonUtils.takeOutChar(user.getEnglishName()));
            }
            update.set("englishName", user.getEnglishName().trim());
        }
        if (!StringUtils.isEmpty(user.getOrgChineseName())) {
            if (CommonUtils.isSpecialChar(user.getOrgChineseName())) {
                return ResultUtil.error(messageInternational("USER_ORG_CHAR") + CommonUtils.takeOutChar(user.getOrgChineseName()));
            }
            update.set("orgChineseName", user.getOrgChineseName().trim());
        } else {
            return ResultUtil.errorInternational("USER_ORG");
        }
        if (!StringUtils.isEmpty(user.getOrgEnglishName())) {
            if (CommonUtils.isSpecialChar(user.getOrgEnglishName())) {
                return ResultUtil.error(messageInternational("USER_ENG_ORG_CHAR") + CommonUtils.takeOutChar(user.getOrgEnglishName()));
            }
            update.set("orgEnglishName", user.getOrgEnglishName().trim());
        }
        if (!StringUtils.isEmpty(user.getTelephone())) {
            update.set("telephone", user.getTelephone().trim());
        }
        if (!StringUtils.isEmpty(user.getOrcId())) {
            update.set("orcId", user.getOrcId());
        }
        if (!StringUtils.isEmpty(user.getAvatar())) {
            String avatar = user.getAvatar();
            if (!CommonUtils.isPicBase(avatar)) {
                return ResultUtil.errorInternational("USER_AVATAR");
            }
            String head = String.valueOf(new Date().getTime());
            String imagePath = "/" + Constants.Image.image + "/" + Constants.Image.USER + "/" + one.getId() + "_" + head + ".jpg";
            try {
                CommonUtils.generateImage(avatar, spaceUrl.getRootDir() + imagePath);
                update.set("avatar", imagePath);
                if (!StringUtils.isEmpty(user.getAvatar())) {
                    Files.delete(new File(spaceUrl.getRootDir(), user.getAvatar()).toPath());
                }
            } catch (Exception e) {
                update.set("avatar", user.getAvatar());
            }
        }
        String introduction = user.getIntroduction();
        if (!StringUtils.isEmpty(introduction)) {
            update.set("introduction", introduction);
        } else {
            update.set("introduction", "");
        }
        mongoTemplate.upsert(query, update, COLLECTION_NAME);
        String updateName = null;
        String updateAvatar = null;
        if (!one.getName().equals(user.getName())) {
            updateName = user.getName();
        }
        if (StringUtils.isNotEmpty(user.getAvatar())) {
            if (one.getAvatar() == null || !one.getAvatar().equals(user.getAvatar())) {
                updateAvatar = user.getAvatar();
            }
        }
        if (null != updateName || null != updateAvatar) {
            asyncDeal.userinfoUpdate(one.getId(), updateAvatar, updateName);
        }
        return ResultUtil.success(true);
    }

    @Override
    public ResponseResult<Object> query(String token) {
        String email = tokenCache.getIfPresent(token);
        if (email == null || email.equals("")) {
            return ResultUtil.errorInternational("USER_NOT_LOG");
        }
        Query query = new Query();
        query.addCriteria(Criteria.where("emailAccounts").is(email));
        UserShow one = mongoTemplate.findOne(query, UserShow.class, COLLECTION_NAME);
        return ResultUtil.success(one);
    }

    @Override
    public ResponseResult<Object> find(String token, String email, String spaceId) {
        Query spaceQuery = new Query().addCriteria(Criteria.where("_id").is(spaceId));
        Space space = mongoTemplate.findOne(spaceQuery, Space.class);
        Query query = new Query();
        if (StringUtils.isNotEmpty(email) && StringUtils.isNotEmpty(email.trim())) {
            Criteria criteria = new Criteria();
            Pattern pattern = Pattern.compile("^.*" + CommonUtils.escapeExprSpecialWord(email.trim()) + ".*$", Pattern.CASE_INSENSITIVE);
            criteria.orOperator(Criteria.where("emailAccounts").is(pattern), Criteria.where("name").is(pattern));
            // query.addCriteria(Criteria.where("emailAccounts").is(pattern));
            query.addCriteria(criteria);
        }
        List<UserSpaceShow> userSpaceShows = mongoTemplate.find(query, UserSpaceShow.class, COLLECTION_NAME);
        if (userSpaceShows.size() == 0 && StringUtils.isNotEmpty(email)) {
            // Join Search User
            // Email verification
            if (CommonUtils.isEmail(email)) {
                String ifPresent = spaceInvite.getIfPresent(email);
                if (ifPresent == null) {
                    ifPresent = CommonUtils.generateUUID();
                    spaceInvite.put(ifPresent, email);
                    spaceInvite.put(email, ifPresent);
                }
                UserSpaceShow userSpaceShow = new UserSpaceShow();
                userSpaceShow.setEmailAccounts(email);
                userSpaceShow.setState(2);
                userSpaceShow.setUserId(ifPresent);
                userSpaceShows.add(userSpaceShow);
            }
        } else {
            Set<AuthorizationPerson> authorizationList = space.getAuthorizationList();
            Set<String> userIdList = new HashSet<>();
            authorizationList.stream().forEachOrdered(author -> {
                userIdList.add(author.getUserId());
            });
            userSpaceShows.stream().forEachOrdered(user -> {
                if (userIdList.contains(user.getUserId())) {
                    user.setJudge(true);
                }
            });
        }
        return ResultUtil.success(userSpaceShows);
    }

    @Override
    public ResponseResult<Object> importUser(String token, ManualAddList manualAddList) {
        List<String> validation = CommonUtils.validation(manualAddList);
        if (validation.size() > 0) {
            return ResultUtil.error(validation.toString());
        }
        Token user = jwtTokenUtils.getToken(token);
        if (CommonUtils.isSpecialChar(manualAddList.getOrg())) {
            return ResultUtil.errorInternational(messageInternational("USER_ORG_CHAR") + CommonUtils.takeOutChar(manualAddList.getOrg()));
        }
        List<ManualAddList.Person> personList = manualAddList.getPerson();
        for (ManualAddList.Person person : personList) {
            // Verify if the email exists
            String email = RSAEncrypt.decrypt(person.getEmail());
            String name = RSAEncrypt.decrypt(person.getName());
            if (StringUtils.isEmpty(email) || StringUtils.isEmpty(name)) {
                return ResultUtil.errorInternational("USER_EMAIL_NOT_COMPLETE");
            }
            if (!CommonUtils.isEmail(email)) {
                return ResultUtil.error(messageInternational("USER_EMAIL_FORMAT") + email);
            }
            if (CommonUtils.isSpecialChar(name)) {
                return ResultUtil.error(messageInternational("USER_NAME_CHAR") + CommonUtils.takeOutChar(name));
            }
            ConsumerDO consumerDO = getUserInfoByName(email.trim());
            if (consumerDO != null && consumerDO.getState() != 2) {
                return ResultUtil.error(messageInternational("USER_EMAIL_EXIST") + email.trim());
            } else if (consumerDO == null) {
                consumerDO = new ConsumerDO();
            }
            consumerDO.setName(name);
            consumerDO.setEmailAccounts(email.trim());
            consumerDO.setState(1);
            consumerDO.setOrgChineseName(manualAddList.getOrg());
            consumerDO.setCreateTime(LocalDateTime.now());
            consumerDO.setRoles(new ArrayList<String>() {

                {
                    add(manualAddList.getRole());
                }
            });
            consumerDO.setAddWay("批量添加");
            mongoTemplate.save(consumerDO, COLLECTION_NAME);
            // Send email
            log.info(" 发送邮箱: " + consumerDO.getEmailAccounts());
            EmailModel emailInvite = EmailModel.EMAIL_INVITE();
            long stringTime = new Date().getTime();
            String code = SMS4.Encryption(emailInvite.getType() + "&" + email.trim() + "&" + stringTime);
            Map<String, Object> param = new HashMap<>(16);
            param.put("name", user.getName());
            String call = emailInvite.getCall().replace("email", user.getEmailAccounts());
            emailInvite.setCall(call);
            param.put("email", email.trim());
            param.put("url", spaceUrl.getCallHost() + "/api/ps.av?code=" + code);
            asyncDeal.send(param, emailInvite, emailInvite.getType());
        }
        return ResultUtil.success();
    }

    /**
     * Email
     */
    @Override
    public ResponseResult<Object> setEmailList(String token) {
        String userId = jwtTokenUtils.getUserIdFromToken(token);
        List<String> emailList = Arrays.asList(EmailRole.listTo);
        List<Map<String, Object>> resultList = new ArrayList<>(emailList.size());
        UserEmailRole userEmailRole = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("userId").is(userId)), UserEmailRole.class);
        List<String> judgeList = new ArrayList<>(emailList.size());
        if (null == userEmailRole) {
            for (String key : emailList) {
                String[] split = key.split(":");
                judgeList.add(split[0]);
            }
        } else {
            Map<String, Boolean> emailRole = userEmailRole.getEmailRole();
            for (String key : emailRole.keySet()) {
                Boolean aBoolean = emailRole.get(key);
                if (aBoolean) {
                    judgeList.add(key);
                }
            }
        }
        for (String key : emailList) {
            Map<String, Object> resultMap = new HashMap<>(3);
            String[] split = key.split(":");
            resultMap.put("name", split[1]);
            resultMap.put("value", split[0]);
            resultMap.put("state", judgeList.contains(split[0]));
            resultList.add(resultMap);
        }
        judgeList.clear();
        return ResultUtil.success(resultList);
    }

    @Override
    public ResponseResult<Object> setEmail(String token, String type, Boolean value) {
        if (StringUtils.isEmpty(type) || StringUtils.isEmpty(type.trim())) {
            return ResultUtil.errorInternational("GENERAL_PARAMETER_ERROR");
        }
        List<String> strings = Arrays.asList(EmailRole.list);
        boolean judge = false;
        for (String string : strings) {
            if (string.contains(type)) {
                judge = true;
                break;
            }
        }
        if (!judge) {
            return ResultUtil.errorInternational("GENERAL_PARAMETER_ERROR");
        }
        Token userToken = jwtTokenUtils.getToken(token);
        UserEmailRole userEmailRole = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("userId").is(userToken.getUserId())), UserEmailRole.class);
        if (null == userEmailRole) {
            userEmailRole = new UserEmailRole();
            userEmailRole.setUserId(userToken.getUserId());
            Map<String, Boolean> addMap = new HashMap<>(1);
            addMap.put(type, value);
            userEmailRole.setEmailRole(addMap);
            mongoTemplate.insert(userEmailRole);
        } else {
            Map<String, Boolean> emailRole = userEmailRole.getEmailRole();
            emailRole.put(type, value);
            userEmailRole.setEmailRole(emailRole);
            mongoTemplate.save(userEmailRole);
        }
        CacheLoading cacheLoading = new CacheLoading();
        cacheLoading.removeUserEmailRole(userToken.getEmailAccounts());
        return ResultUtil.success();
    }

    private void addRole(ConsumerDO user) {
        String emailAccounts = user.getEmailAccounts();
        List<Identify> all = mongoTemplate.findAll(Identify.class);
        if (all.size() > 0) {
            for (Identify identify : all) {
                if (emailAccounts.contains(identify.getSuffix())) {
                    user.setRoles(new ArrayList<String>() {

                        {
                            add(Constants.SENIOR);
                        }
                    });
                    break;
                }
            }
            if (user.getRoles() == null || user.getRoles().size() == 0) {
                user.setRoles(new ArrayList<String>() {

                    {
                        add(Constants.GENERAL);
                    }
                });
            }
        } else {
            user.setRoles(new ArrayList<String>() {

                {
                    add(Constants.GENERAL);
                }
            });
        }
    }

    private ConsumerDO getUserInfoByName(String email) {
        Query query = new Query();
        query.addCriteria(Criteria.where("emailAccounts").is(email));
        return mongoTemplate.findOne(query, ConsumerDO.class, COLLECTION_NAME);
    }

    private Map<String, Object> setMessage(String message) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", 500);
        map.put("message", message);
        return map;
    }
}
