package cn.cnic.dataspace.api.service.impl;

import cn.cnic.dataspace.api.asynchronous.AsyncDeal;
import cn.cnic.dataspace.api.cacheLoading.CacheLoading;
import cn.cnic.dataspace.api.config.space.SpaceControlConfig;
import cn.cnic.dataspace.api.exception.CommonException;
import cn.cnic.dataspace.api.model.IpInfo;
import cn.cnic.dataspace.api.model.login.*;
import cn.cnic.dataspace.api.model.manage.Identify;
import cn.cnic.dataspace.api.model.user.*;
import cn.cnic.dataspace.api.service.AuthService;
import cn.cnic.dataspace.api.service.space.SpaceService;
import cn.cnic.dataspace.api.util.*;
import cn.hutool.extra.cglib.CglibUtil;
import com.alibaba.fastjson.JSON;
import com.github.benmanes.caffeine.cache.Cache;
import com.google.code.kaptcha.Producer;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import static cn.cnic.dataspace.api.util.CommonUtils.messageInternational;

@Service
@Slf4j
@EnableAsync
public class AuthServiceImpl implements AuthService {

    private final Cache<String, String> tokenCache = CaffeineUtil.getTokenCache();

    private final Cache<String, List<String>> emailToken = CaffeineUtil.getEmailToken();

    private final Cache<String, String> check = CaffeineUtil.getCHECK();

    private final Cache<String, String> disable = CaffeineUtil.getDisable();

    private final Cache<String, String> network = CaffeineUtil.getThirdParty();

    private final Cache<String, Integer> errorPwd = CaffeineUtil.getErrorPwd();

    private final Cache<String, String> thirdParty = CaffeineUtil.getThirdParty();

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private JwtTokenUtils jwtTokenUtils;

    @Autowired
    private SpaceUrl spaceUrl;

    @Autowired
    private SpaceService spaceService;

    @Autowired
    private CacheLoading cacheLoading;

    @Lazy
    @Autowired
    private AsyncDeal asyncDeal;

    @Autowired
    private SpaceControlConfig spaceControlConfig;

    @Autowired
    private Producer producer;

    @Override
    public ResponseResult<Object> login(String emailAccounts, String password, String unionId, HttpServletResponse response) {
        if (StringUtils.isEmpty(emailAccounts.trim()) || StringUtils.isEmpty(password.trim())) {
            return ResultUtil.errorInternational("AUTH_PWD");
        }
        String email = RSAEncrypt.decrypt(emailAccounts);
        String pwd = RSAEncrypt.decrypt(password);
        if (StringUtils.isEmpty(email) || StringUtils.isEmpty(pwd)) {
            return ResultUtil.errorInternational("AUTH_PWD");
        }
        String emailCheck = disable.getIfPresent(email);
        if (null != emailCheck) {
            return ResultUtil.errorInternational("AUTH_LOCK");
        }
        String emailR = disable.getIfPresent(email + "_login_r");
        if (null != emailR) {
            return ResultUtil.errorInternational("AUTH_DISABLE");
        }
        Query query = new Query();
        query.addCriteria(Criteria.where("emailAccounts").is(email));
        ConsumerDO user = mongoTemplate.findOne(query, ConsumerDO.class, UserServiceImpl.COLLECTION_NAME);
        if (user == null) {
            Integer ifPresent = errorPwd.getIfPresent(email + "_login_r");
            if (ifPresent != null) {
                if (ifPresent >= 5) {
                    disable.put(email + "_login_r", String.valueOf(ifPresent));
                    return ResultUtil.errorInternational("AUTH_DISABLE");
                } else {
                    errorPwd.put(email + "_login_r", ifPresent + 1);
                }
            } else {
                errorPwd.put(email + "_login_r", 1);
            }
            return ResultUtil.errorInternational("AUTH_EMAIL_UNREGISTERED");
        }
        if (user.isDisablePwd()) {
            long time = user.getDisablePwdTime().getTime();
            if (time > CommonUtils.yesterday(24)) {
                return ResultUtil.errorInternational("AUTH_LOCK");
            } else {
                user.setDisablePwd(false);
                user.setDisablePwdTime(null);
                mongoTemplate.save(user);
            }
        }
        if (user.getState() == 0) {
            return ResultUtil.errorInternational("AUTH_EMAIL_UNACTIVATED");
        } else if (user.getDisable() == 1) {
            return ResultUtil.errorInternational("AUTH_USER_DISABLE");
        } else {
            String userPassword = user.getPassword();
            if (!StringUtils.isEmpty(userPassword) && RSAEncrypt.decrypt(userPassword).equals(pwd)) {
                String loginWay = Constants.LoginWay.SYS;
                if (null != unionId) {
                    String wechatId = user.getWechatId();
                    if (StringUtils.isNotEmpty(wechatId)) {
                        return ResultUtil.errorInternational("AU_WECHAT_ACC");
                    } else {
                        user.setWechatId(unionId);
                        mongoTemplate.save(user);
                    }
                    loginWay = Constants.LoginWay.WECHAT;
                }
                loginInfoAddCookie(user, response, loginWay);
                return ResultUtil.success();
            } else {
                Integer ifPresent = errorPwd.getIfPresent(email + "_login");
                int count = 0;
                if (ifPresent != null) {
                    if (ifPresent >= 5) {
                        disable.put(email, String.valueOf(ifPresent));
                        // Clear account online users
                        List<String> tokenList = emailToken.getIfPresent(email);
                        if (null != tokenList && tokenList.size() > 0) {
                            for (String token : tokenList) {
                                tokenCache.invalidate(token);
                            }
                            emailToken.invalidate(email);
                        }
                        user.setDisablePwd(true);
                        user.setDisablePwdTime(new Date());
                        mongoTemplate.save(user);
                        return ResultUtil.errorInternational("AUTH_LOCK");
                    } else {
                        errorPwd.put(email + "_login", ifPresent + 1);
                    }
                    count = ifPresent + 1;
                } else {
                    errorPwd.put(email + "_login", 1);
                    count = 1;
                }
                if (count >= 3) {
                    return ResultUtil.error(CommonUtils.messageInternational("AUTH_PWD_ERROR") + count);
                }
            }
        }
        return ResultUtil.errorInternational("AUTH_ACCOUNT_PWD");
    }

    @Override
    public void emailActivation(String code, HttpServletResponse response) throws IOException {
        if (StringUtils.isEmpty(code)) {
            returnRes(response, "code errorInternational!");
            return;
        }
        String decrypt = SMS4.Decrypt(code);
        if (decrypt == null) {
            returnRes(response, "code errorInternational!");
            return;
        }
        if (!decrypt.contains("&")) {
            returnRes(response, "code errorInternational!");
            return;
        }
        String[] split = decrypt.split("&");
        String type = split[0];
        String email = split[1];
        String time = split[2];
        if (time.trim().equals("")) {
            returnRes(response, "code errorInternational!");
            return;
        }
        Long activationTime = Long.valueOf(time);
        if (activationTime.longValue() < CommonUtils.yesterday(72)) {
            returnRes(response, "Link broken!");
            return;
        }
        Query query = new Query();
        query.addCriteria(Criteria.where("emailAccounts").is(email));
        ConsumerDO user = mongoTemplate.findOne(query, ConsumerDO.class, UserServiceImpl.COLLECTION_NAME);
        if (user.getState() == 1) {
            String html = "<script type='text/javascript'>location.href='" + spaceUrl.getSuccessUrl() + "';</script>";
            response.getWriter().print(html);
            return;
        }
        if (user == null) {
            returnRes(response, "Email is not registered, please register first!");
            return;
        }
        Update update = new Update();
        if (!StringUtils.isEmpty(user.getName())) {
            update.set("state", 1);
        }
        mongoTemplate.upsert(query, update, UserServiceImpl.COLLECTION_NAME);
        // Space invitation activation
        if (type.equals("SpaceInvite")) {
            String spaceId = split[3];
            if (spaceId == null || spaceId.equals("")) {
                returnRes(response, "The space invitation code is incorrect!");
                return;
            }
            String sourUserId = split[4];
            String spaceRole = null;
            if (split.length > 5) {
                spaceRole = split[5];
            }
            spaceService.spaceInviteByEmail(spaceId, user.getId(), sourUserId, spaceRole);
        }
        String html = "<script type='text/javascript'>location.href='" + spaceUrl.getSuccessUrl() + "';</script>";
        response.getWriter().print(html);
        return;
    }

    private void returnRes(HttpServletResponse response, String message) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("code", 500);
        paramMap.put("message", message);
        errorMsg(response, paramMap);
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String token = CommonUtils.getUser(request, Constants.TOKEN);
        String way = CommonUtils.getUser(request, Constants.WAY);
        String loginUrl = spaceUrl.getWebUrl();
        ;
        Cookie cookie = CommonUtils.getCookie(Constants.TOKEN, "", -1, 1);
        Cookie cookieTo = CommonUtils.getCookie(Constants.WAY, "", -1, 1);
        response.addCookie(cookie);
        response.addCookie(cookieTo);
        request.getSession().invalidate();
        if (token != null && null != tokenCache && null != tokenCache.getIfPresent(token)) {
            String userId = jwtTokenUtils.getUserIdFromToken(token);
            String username = tokenCache.getIfPresent(token);
            if (emailToken != null) {
                emailToken.invalidate(username);
            }
            network.invalidate(Constants.LoginWay.NETWORK + username);
            network.invalidate(Constants.LoginWay.NETWORK_USER + username);
            tokenCache.invalidate(token);
            // Record login logs, modify user login status
            loginLogRecording(userId, username, false, way);
            if (way.equals(Constants.LoginWay.UMP)) {
                loginUrl = spaceUrl.getCasLogoutUrl();
                Object umt = judgeUmt(response);
                if (null == umt) {
                    return;
                }
                UmtConf umtConf = (UmtConf) umt;
                loginUrl = loginUrl + umtConf.getHongPage();
            } else if (way.equals(Constants.LoginWay.ESCIENCE)) {
                Object umt = judgeEsc(response);
                if (null == umt) {
                    return;
                }
                if (tokenCache != null) {
                    HttpClient httpClient = new HttpClient();
                    Map<String, String> hearMap = new HashMap<>();
                    String escToken = tokenCache.getIfPresent(Constants.LoginWay.ESCIENCE + token);
                    hearMap.put("Authorization", escToken);
                    httpClient.doGet(EscUrl.logoutUrl, hearMap);
                    tokenCache.invalidate(Constants.LoginWay.ESCIENCE + token);
                }
            }
        }
        response.sendRedirect(loginUrl);
        return;
    }

    private ConsumerDO umpUserDispose(String username) {
        Query query = new Query();
        query.addCriteria(Criteria.where("emailAccounts").is(username));
        ConsumerDO user = mongoTemplate.findOne(query, ConsumerDO.class, UserServiceImpl.COLLECTION_NAME);
        return user;
    }

    @Override
    public ResponseResult<Object> getUserInfo(String token, HttpServletResponse response) {
        if (!jwtTokenUtils.validateToken(token)) {
            Cookie cookie = CommonUtils.getCookie(Constants.TOKEN, "", 0, 1);
            Cookie cookie2 = CommonUtils.getCookie(Constants.WAY, "", 0, 1);
            response.addCookie(cookie);
            response.addCookie(cookie2);
            return ResultUtil.errorInternational("AUTH_TOKEN");
        } else {
            String ifPresent = tokenCache.getIfPresent(token);
            if (ifPresent == null) {
                return ResultUtil.errorInternational("LOGIN_EXCEPTION");
            }
        }
        Token user = jwtTokenUtils.getToken(token);
        if (user == null) {
            return ResultUtil.errorInternational("AUTH_TOKEN");
        }
        Query query = new Query();
        query.addCriteria(Criteria.where("emailAccounts").is(user.getEmailAccounts()));
        ConsumerDO consumerDO = mongoTemplate.findOne(query, ConsumerDO.class, UserServiceImpl.COLLECTION_NAME);
        user.setAccessToken(token);
        user.setAvatar(consumerDO.getAvatar());
        return ResultUtil.success(user);
    }

    private ConsumerDO umpAdd(String realName, String email, String way, String work) {
        Query query = new Query();
        query.addCriteria(Criteria.where("emailAccounts").is(email));
        ConsumerDO judge = mongoTemplate.findOne(query, ConsumerDO.class, UserServiceImpl.COLLECTION_NAME);
        if (judge != null) {
            throw new CommonException(CommonUtils.messageInternational("SYSTEM_ERROR"));
        }
        ConsumerDO user = new ConsumerDO();
        user.setEmailAccounts(email);
        user.setCreateTime(LocalDateTime.now());
        user.setState(1);
        user.setAddWay(way);
        user.setName(realName);
        if (null != work) {
            user.setOrgChineseName(work);
        }
        // Determine email suffix
        addRole(user);
        return mongoTemplate.save(user, UserServiceImpl.COLLECTION_NAME);
    }

    @Override
    public void umtCallback(String code, HttpServletResponse response) throws IOException {
        // Technology Cloud Certification
        HttpClient httpClient = new HttpClient();
        Object umt = judgeUmt(response);
        if (null == umt) {
            return;
        }
        UmtConf umtConf = (UmtConf) umt;
        String param = "redirect_uri=" + umtConf.getHongPage() + spaceUrl.getUmtCallbackUrl() + "&client_id=" + umtConf.getAppKey() + "&client_secret=" + umtConf.getAppSecret() + "&" + spaceUrl.getAuthParam() + code;
        String reData = httpClient.sendPost(spaceUrl.getAuthUrl(), param);
        if (StringUtils.isEmpty(reData)) {
            returnRes(response, "param code errorInternational");
            return;
        }
        log.info("科技云 回调数据:" + reData);
        Gson gson = new Gson();
        Map map = gson.fromJson(reData, Map.class);
        String userInfo = (String) map.get("userInfo");
        Map userMap = gson.fromJson(userInfo, Map.class);
        String username = (String) userMap.get("cstnetId");
        String realName = (String) userMap.get("truename");
        ConsumerDO user = umpUserDispose(username);
        String url;
        if (user == null || StringUtils.isEmpty(user.getOrgChineseName())) {
            if (null != user && (user.getState() == 2 || user.getState() == 0)) {
                mongoTemplate.remove(user);
                user = null;
            }
            if (user == null) {
                umpAdd(realName, username, "科技云", null);
            }
            // Generate code and send it to the front-end page
            long stringTime = new Date().getTime();
            String to = SMS4.Encryption(Constants.LoginWay.UMP + "&" + username + "&" + stringTime);
            url = spaceUrl.getUmpWork() + username + "&way=" + Constants.LoginWay.UMP + "&code=" + to;
        } else {
            url = spaceUrl.getSpaceUrl();
            loginInfoAddCookie(user, response, Constants.LoginWay.UMP);
        }
        String html = "<script type='text/javascript'>location.href='" + url + "';</script>";
        response.getWriter().print(html);
        return;
    }

    @Override
    public void umtLogin(HttpServletResponse response) {
        String loginUrl = spaceUrl.getCasLoginUrl();
        Object umt = judgeUmt(response);
        if (null == umt) {
            return;
        }
        UmtConf umtConf = (UmtConf) umt;
        try {
            response.sendRedirect(loginUrl + "redirect_uri=" + umtConf.getHongPage() + spaceUrl.getUmtCallbackUrl() + "&client_id=" + umtConf.getAppKey());
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return;
    }

    @Override
    public void passActivation(String code, HttpServletRequest request, HttpServletResponse response) throws IOException {
        checkCode(code, response);
        String decrypt = SMS4.Decrypt(code);
        String[] split = decrypt.split("&");
        String email = split[1];
        String time = split[2];
        String chick = check.getIfPresent(email + time);
        if (chick != null) {
            returnRes(response, "This link has been used, please try again!");
            return;
        }
        Query query = new Query();
        query.addCriteria(Criteria.where("emailAccounts").is(email));
        ConsumerDO user = mongoTemplate.findOne(query, ConsumerDO.class, UserServiceImpl.COLLECTION_NAME);
        if (user == null || user.getState() == 0) {
            returnRes(response, "Mailbox not registered or activated!");
            return;
        }
        String id = request.getSession().getId();
        String ifPresent = check.getIfPresent(id);
        if (ifPresent == null) {
            check.put(id, email + "&" + time);
        }
        // Frontend password modification page
        String html = "<script type='text/javascript'>location.href='" + spaceUrl.getUpPwdUrl() + "';</script>";
        response.getWriter().print(html);
        return;
    }

    @Override
    public ResponseResult<Object> emailSend(String email, String type) {
        String key = Constants.EmailSendType.REGISTER_TO + email;
        if (CaffeineUtil.checkEmailAging(key)) {
            return ResultUtil.errorInternational("SEND_EMAIL_TIME");
        }
        String url = "";
        long stringTime = new Date().getTime();
        String code = SMS4.Encryption(type + "&" + email + "&" + stringTime);
        Query query = new Query().addCriteria(Criteria.where("emailAccounts").is(email));
        ConsumerDO one = mongoTemplate.findOne(query, ConsumerDO.class);
        if (null == one) {
            return ResultUtil.errorInternational("AUTH_EMAIL_ERROR");
        }
        EmailModel emailType;
        if (type.equals("register")) {
            url = spaceUrl.getEmailActivation() + code;
            emailType = EmailModel.EMAIL_REGISTER();
        } else if (type.equals("changePwd")) {
            url = spaceUrl.getCallHost() + "/api/ps.av?code=" + code;
            emailType = EmailModel.EMAIL_PASS();
        } else {
            return ResultUtil.success();
        }
        Map<String, Object> param = new HashMap<>();
        param.put("name", one.getName());
        param.put("email", email);
        param.put("url", url);
        asyncDeal.send(param, emailType, emailType.getType());
        return ResultUtil.success();
    }

    @Override
    public ResponseResult<Object> umpWork(String code, String work, HttpServletResponse response) {
        checkCode(code);
        String decrypt = SMS4.Decrypt(code);
        String[] split = decrypt.split("&");
        String email = split[1];
        String type = split[0];
        Query query = new Query();
        query.addCriteria(Criteria.where("emailAccounts").is(email));
        ConsumerDO user = mongoTemplate.findOne(query, ConsumerDO.class);
        if (user == null) {
            return ResultUtil.errorInternational("AUTH_USER_NOT_FOUND");
        }
        user.setOrgChineseName(work);
        mongoTemplate.save(user);
        loginInfoAddCookie(user, response, type);
        return ResultUtil.success();
    }

    /**
     * User synchronization channel
     */
    @Override
    public ResponseResult<Object> channel(Channel consumerDTO) {
        if (consumerDTO == null) {
            return ResultUtil.errorInternational("USER_PARAMETER");
        }
        List<String> validation = CommonUtils.validation(consumerDTO);
        if (validation.size() > 0) {
            return ResultUtil.error(-1, validation.toString());
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
        if (StringUtils.isNotEmpty(consumerDTO.getPassword()) && StringUtils.isNotEmpty(consumerDTO.getConfirmPassword())) {
            String password = RSAEncrypt.decrypt(consumerDTO.getPassword());
            String confirmPassword = RSAEncrypt.decrypt(consumerDTO.getConfirmPassword());
            if (StringUtils.isNotEmpty(password) && StringUtils.isNotEmpty(confirmPassword)) {
                if (StringUtils.isEmpty(password) || StringUtils.isEmpty(confirmPassword)) {
                    return ResultUtil.errorInternational("PWD_ERROR");
                }
                if (!password.equals(confirmPassword)) {
                    return ResultUtil.errorInternational("PWD_NOT_MATCH");
                }
                if (!CommonUtils.passVerify(password)) {
                    return ResultUtil.errorInternational("PWD_STRENGTH");
                }
            }
        }
        String emailAccounts = emailDe.trim();
        Query query = new Query();
        query.addCriteria(Criteria.where("emailAccounts").is(emailAccounts));
        ConsumerDO user = mongoTemplate.findOne(query, ConsumerDO.class, UserServiceImpl.COLLECTION_NAME);
        String userId;
        if (user == null) {
            user = new ConsumerDO();
            consumerDTO.setPassword(consumerDTO.getPassword());
            consumerDTO.setName(name);
            consumerDTO.setEmailAccounts(emailAccounts);
            CglibUtil.copy(consumerDTO, user);
            user.setCreateTime(LocalDateTime.now());
            user.setOrgChineseName(consumerDTO.getOrg());
            user.setState(1);
            user.setAddWay("干细胞用户同步");
            // Determine email suffix
            addRole(user);
            ConsumerDO insert = mongoTemplate.insert(user);
            userId = insert.getId();
        } else {
            userId = user.getId();
        }
        return ResultUtil.success(userId);
    }

    @Override
    public void channelLogin(String id, HttpServletResponse response) throws IOException {
        if (StringUtils.isEmpty(id.trim())) {
            throw new CommonException("param is null!");
        }
        String decrypt = SMS4.Decrypt(id);
        if (StringUtils.isEmpty(decrypt)) {
            throw new CommonException("无法识别!");
        }
        Query query = new Query().addCriteria(Criteria.where("_id").is(decrypt));
        ConsumerDO one = mongoTemplate.findOne(query, ConsumerDO.class);
        if (null == one) {
            throw new CommonException("用户不存在!");
        }
        if (one.getState() != 1) {
            throw new CommonException("用户已被禁用!");
        }
        loginInfoAddCookie(one, response, Constants.LoginWay.CHANNEL);
        String html = "<script type='text/javascript'>location.href='" + spaceUrl.getSpaceUrl() + "';</script>";
        response.getWriter().print(html);
        return;
    }

    /**
     * WeChat login
     */
    @Override
    public void wechatLogin(String type, HttpServletResponse response) {
        Object wechat = judgeWechat(response);
        if (null == wechat) {
            return;
        }
        WechatConf wechatConf = (WechatConf) wechat;
        String state = "login";
        try {
            if (StringUtils.isNotEmpty(type) && type.equals("1")) {
                state = "binding";
            }
            String authUrl = WechatUrl.getAuthUrl(wechatConf.getAppId(), wechatConf.getHongPage() + spaceUrl.getWechatCallbackUrl(), state);
            response.sendRedirect(authUrl);
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return;
    }

    /**
     * WeChat login callback
     */
    @Override
    public void wechatCallback(String code, String state, HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpClient httpClient = new HttpClient();
        Object wechat = judgeWechat(response);
        if (null == wechat) {
            return;
        }
        WechatConf wechatConf = (WechatConf) wechat;
        String tokenParam = WechatUrl.getTokenUrl(wechatConf.getAppId(), code, wechatConf.getSecretKey());
        String reData = httpClient.sendPost(WechatUrl.tokenUrl, tokenParam);
        if (StringUtils.isEmpty(reData)) {
            returnRes(response, "param code errorInternational");
            return;
        }
        log.info("wechat {} 获取token数据:" + reData);
        Gson gson = new Gson();
        Map dataMap = gson.fromJson(reData, Map.class);
        if (null == dataMap || !dataMap.containsKey("access_token")) {
            log.info("wechat-callback-getToken: error {} " + reData);
            returnRes(response, "Login error!");
            return;
        }
        String access_token = (String) dataMap.get("access_token");
        String openid = (String) dataMap.get("openid");
        // Obtain WeChat user information
        String result = null;
        try {
            result = httpClient.doGetWayTwo(WechatUrl.getUserInfoUrl(openid, access_token));
        } catch (URISyntaxException e) {
            e.printStackTrace();
            log.info("wechat-callback-getToken: error {} " + reData);
            returnRes(response, "Login error!");
            return;
        }
        log.info("wechat {} 获取用户信息数据:" + reData);
        String info = new String(result.getBytes("iso-8859-1"), "utf-8");
        Map userInfo = gson.fromJson(info, Map.class);
        if (null == userInfo || !userInfo.containsKey("unionid")) {
            log.info("wechat-callback-getUserinfo: error {} " + result);
            returnRes(response, "Login error!");
            return;
        }
        String unionId = (String) userInfo.get("unionid");
        String nickname = (String) userInfo.get("nickname");
        String headImgUrl = (String) userInfo.get("headimgurl");
        ConsumerDO wechatId = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("wechatId").is(unionId)), ConsumerDO.class);
        String webUrl;
        if (state.equals("binding")) {
            // User binding
            if (null != wechatId) {
                returnRes(response, "The wechat account has been bound to another system account!");
                return;
            }
            String token = CommonUtils.getUser(request, Constants.TOKEN);
            if (StringUtils.isEmpty(token)) {
                returnRes(response, "Please log in!");
                return;
            }
            String userIdFromToken = jwtTokenUtils.getUserIdFromToken(token);
            ConsumerDO consumerDO = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("_id").is(userIdFromToken)), ConsumerDO.class);
            if (null == consumerDO) {
                returnRes(response, "User does not exist!");
                return;
            }
            if (StringUtils.isNotEmpty(consumerDO.getWechatId())) {
                returnRes(response, "This system account has been bound to other wechat accounts!");
                return;
            }
            consumerDO.setWechatId(unionId);
            mongoTemplate.save(consumerDO);
            WechatUser wechatUser = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("unionId").is(unionId)), WechatUser.class);
            wechatUpdate(userInfo, wechatUser);
            webUrl = spaceUrl.getWechatConfUrl();
        } else {
            if (null == wechatId) {
                // New User - Jump to Bind User Page
                WechatUser wechatUser = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("unionId").is(unionId)), WechatUser.class);
                wechatUpdate(userInfo, wechatUser);
                String sessionId = request.getSession().getId();
                thirdParty.put(Constants.LoginWay.WECHAT + sessionId, unionId);
                thirdParty.put(Constants.LoginWay.WECHAT + unionId, headImgUrl + "~" + nickname);
                webUrl = spaceUrl.getWechatBindingUrl();
            } else {
                // Found bound user login
                webUrl = spaceUrl.getSpaceUrl();
                loginInfoAddCookie(wechatId, response, Constants.LoginWay.WECHAT);
            }
        }
        String html = "<script type='text/javascript'>location.href='" + webUrl + "';</script>";
        response.getWriter().print(html);
        return;
    }

    @Override
    public ResponseResult<Object> wechatUserinfo(HttpServletRequest request) {
        String sessionId = request.getSession().getId();
        String unionId = thirdParty.getIfPresent(Constants.LoginWay.WECHAT + sessionId);
        if (null == unionId) {
            return ResultUtil.success();
        }
        String ifPresent = thirdParty.getIfPresent(Constants.LoginWay.WECHAT + unionId);
        if (ifPresent == null) {
            return ResultUtil.errorInternational("AU_WECHAT_LOGIN");
        }
        String[] split = ifPresent.split("~");
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("name", split[1]);
        resultMap.put("image", split[0]);
        return ResultUtil.success(resultMap);
    }

    @Override
    public ResponseResult<Object> wechatAcc(String emailAccounts, String password, HttpServletRequest request, HttpServletResponse response) {
        String sessionId = request.getSession().getId();
        String unionId = thirdParty.getIfPresent(Constants.LoginWay.WECHAT + sessionId);
        if (null == unionId) {
            return ResultUtil.errorInternational("AU_WECHAT_LOGIN");
        }
        ResponseResult<Object> login = login(emailAccounts, password, unionId, response);
        if (login.getCode() == 0) {
            thirdParty.invalidate(Constants.LoginWay.WECHAT + sessionId);
            thirdParty.invalidate(Constants.LoginWay.WECHAT + unionId);
        }
        return login;
    }

    @Override
    public ResponseResult<Object> wechatRegister(String emailAccounts, String name, String org, HttpServletRequest request, HttpServletResponse response) {
        String sessionId = request.getSession().getId();
        String unionId = thirdParty.getIfPresent(Constants.LoginWay.WECHAT + sessionId);
        if (null == unionId) {
            return ResultUtil.errorInternational("AU_WECHAT_LOGIN");
        }
        // Decryption
        String username = RSAEncrypt.decrypt(emailAccounts);
        String realName = RSAEncrypt.decrypt(name);
        String work = RSAEncrypt.decrypt(org);
        if (StringUtils.isEmpty(username.trim()) || StringUtils.isEmpty(realName.trim()) || StringUtils.isEmpty(work.trim())) {
            return ResultUtil.errorInternational("PARAMETER_ERROR");
        }
        if (!CommonUtils.isEmail(username.trim())) {
            return ResultUtil.errorInternational("USER_EMAIL");
        }
        if (CommonUtils.isSpecialChar(realName)) {
            return ResultUtil.error(messageInternational("USER_REAL_NAME") + CommonUtils.takeOutChar(realName));
        }
        if (CommonUtils.isSpecialChar(work)) {
            return ResultUtil.error(messageInternational("USER_ORG_ERROR") + CommonUtils.takeOutChar(work));
        }
        Query query = new Query().addCriteria(Criteria.where("emailAccounts").is(username.trim()));
        ConsumerDO consumerDO = mongoTemplate.findOne(query, ConsumerDO.class);
        if (null != consumerDO) {
            return ResultUtil.warning("USER_EXIST");
        }
        consumerDO = new ConsumerDO();
        consumerDO.setWechatId(unionId);
        consumerDO.setEmailAccounts(username);
        consumerDO.setState(1);
        consumerDO.setOrgChineseName(work);
        consumerDO.setAddWay("微信");
        consumerDO.setCreateTime(LocalDateTime.now());
        consumerDO.setName(realName);
        addRole(consumerDO);
        ConsumerDO insert = mongoTemplate.insert(consumerDO);
        loginInfoAddCookie(insert, response, Constants.LoginWay.WECHAT);
        thirdParty.invalidate(Constants.LoginWay.WECHAT + sessionId);
        thirdParty.invalidate(Constants.LoginWay.WECHAT + unionId);
        return ResultUtil.success();
    }

    @Override
    public void escLogin(HttpServletResponse response) {
        Object esc = judgeEsc(response);
        if (null == esc) {
            return;
        }
        EscConf escConf = (EscConf) esc;
        try {
            String authUrl = EscUrl.getAuthUrl(escConf.getClientId(), escConf.getHongPage() + spaceUrl.getEscienceCallbackUrl());
            response.sendRedirect(authUrl);
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return;
    }

    @Override
    public void escCallback(String code, HttpServletResponse response) throws IOException {
        HttpClient httpClient = new HttpClient();
        Object escObj = judgeEsc(response);
        if (null == escObj) {
            return;
        }
        EscConf escConf = (EscConf) escObj;
        String tokenParam = EscUrl.getTokenUrl(escConf.getClientId(), code, escConf.getClientSecret(), escConf.getHongPage() + spaceUrl.getEscienceCallbackUrl());
        String reData = httpClient.sendPost(EscUrl.tokenUrl, tokenParam);
        if (StringUtils.isEmpty(reData)) {
            returnRes(response, "param code errorInternational");
            return;
        }
        log.info("esc(共享网) {} 获取token数据:" + reData);
        Gson gson = new Gson();
        Map dataMap = gson.fromJson(reData, Map.class);
        if (null == dataMap || !dataMap.containsKey("access_token")) {
            log.info("esc-callback-getToken: error {} " + reData);
            returnRes(response, "Login error!");
            return;
        }
        String access_token = (String) dataMap.get("access_token");
        String token_type = (String) dataMap.get("token_type");
        // Obtain shared network user information
        Map<String, String> hearMap = new HashMap<>();
        hearMap.put("Authorization", token_type + " " + access_token);
        String result = httpClient.doGet(EscUrl.userUrl, hearMap);
        log.info("esc(共享网) {} 获取用户数据:" + result);
        Map userInfo = gson.fromJson(result, Map.class);
        if (null == userInfo || !userInfo.containsKey("code") || (int) (double) userInfo.get("code") != 200) {
            log.info("esc-callback-getUserinfo: error {} " + result);
            returnRes(response, "Login error!");
            return;
        }
        Map userMap = (Map) ((Map) userInfo.get("data")).get("user");
        if (null == userMap) {
            log.info("esc-callback-getUserinfo: error {} " + result);
            returnRes(response, "Login error!");
            return;
        }
        String email = (String) userMap.get("email");
        String username = (String) userMap.get("username");
        String orgName = (String) userMap.get("orgName");
        ConsumerDO consumerDO = umpUserDispose(email);
        String webUrl;
        if (consumerDO == null || StringUtils.isEmpty(consumerDO.getOrgChineseName())) {
            if (consumerDO.getState() == 2 || consumerDO.getState() == 0) {
                mongoTemplate.remove(consumerDO);
                consumerDO = null;
            }
            if (consumerDO == null) {
                consumerDO = umpAdd(username, email, "共享网", orgName);
            }
            // Generate code and send it to the front-end page
            if (StringUtils.isEmpty(consumerDO.getOrgChineseName()) && StringUtils.isNotEmpty(orgName)) {
                // Automatic completion mechanism
                consumerDO.setOrgChineseName(orgName);
                mongoTemplate.save(consumerDO);
                Token token = saveUserInfo(consumerDO);
                tokenCache.put(Constants.LoginWay.ESCIENCE + token.getAccessToken(), token_type + " " + access_token);
                webUrl = spaceUrl.getSpaceUrl();
                loginInfoAddCookie(token, response, Constants.LoginWay.ESCIENCE);
            } else {
                long stringTime = new Date().getTime();
                String to = SMS4.Encryption(Constants.LoginWay.ESCIENCE + "&" + email + "&" + stringTime);
                webUrl = spaceUrl.getUmpWork() + email + "&way=" + Constants.LoginWay.ESCIENCE + "&code=" + to;
            }
        } else {
            Token token = saveUserInfo(consumerDO);
            tokenCache.put(Constants.LoginWay.ESCIENCE + token.getAccessToken(), token_type + " " + access_token);
            webUrl = spaceUrl.getSpaceUrl();
            loginInfoAddCookie(token, response, Constants.LoginWay.ESCIENCE);
        }
        String html = "<script type='text/javascript'>location.href='" + webUrl + "';</script>";
        response.getWriter().print(html);
        return;
    }

    @Override
    public void spaceDetails(String key, String user, String spaceId, HttpServletResponse response) throws IOException {
        if (StringUtils.isEmpty(user) || StringUtils.isEmpty(user.trim())) {
            throw new CommonException("param is null!");
        }
        if (StringUtils.isEmpty(spaceId) || StringUtils.isEmpty(spaceId.trim())) {
            throw new CommonException("param is null!");
        }
        Query query = new Query().addCriteria(Criteria.where("_id").is(user));
        ConsumerDO one = mongoTemplate.findOne(query, ConsumerDO.class);
        if (null == one) {
            throw new CommonException("用户不存在!");
        }
        if (one.getState() != 1) {
            throw new CommonException("用户未激活!");
        }
        if (one.getDisable() == 1) {
            throw new CommonException("用户已被禁用!");
        }
        if (null == one.getAppKey() || !one.getAppKey().equals(key)) {
            throw new CommonException("无权操作此用户!");
        }
        spaceControlConfig.spatialVerification(spaceId, one.getEmailAccounts(), Constants.SpaceRole.LEVEL_OTHER);
        loginInfoAddCookie(one, response, Constants.LoginWay.OPEN);
        String url = spaceUrl.getSpaceDetailUrl().replaceAll("spaceId", spaceId);
        String html = "<script type='text/javascript'>location.href='" + url + "';</script>";
        response.getWriter().print(html);
        return;
    }

    @Override
    public ResponseResult<Object> getCode(HttpServletRequest request, HttpServletResponse response) {
        // Generate verification code
        String capText = producer.createText();
        BufferedImage image = producer.createImage(capText);
        // Convert stream information writing
        FastByteArrayOutputStream os = new FastByteArrayOutputStream();
        try {
            ImageIO.write(image, "jpg", os);
        } catch (IOException e) {
            log.error("ImageIO write err", e);
        }
        HttpSession session = request.getSession();
        // In seconds
        session.setMaxInactiveInterval(60);
        session.setAttribute(Constants.EmailSendType.EMAIL_CODE, capText.toLowerCase());
        return ResultUtil.success(cn.hutool.core.codec.Base64.encode(os.toByteArray()));
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

    private Token saveUserInfo(ConsumerDO user) {
        Token token = generateToken(user);
        if (null == token) {
            throw new CommonException(500, CommonUtils.messageInternational("SYSTEM_ERROR"));
        }
        String username = user.getEmailAccounts();
        tokenCache.put(token.getAccessToken(), username);
        List<String> ifPresent = emailToken.getIfPresent(username);
        if (null == ifPresent || ifPresent.size() == 0) {
            emailToken.put(username, new ArrayList<String>() {

                {
                    add(token.getAccessToken());
                }
            });
        } else {
            ifPresent.add(token.getAccessToken());
            emailToken.put(username, ifPresent);
        }
        return token;
    }

    private Token generateToken(ConsumerDO user) {
        // Obtain unauthorized paths
        List<String> roles = user.getRoles();
        if (roles == null) {
            return null;
        }
        String role = "";
        if (roles.contains(Constants.ADMIN)) {
            role = Constants.ADMIN;
        } else if (roles.contains(Constants.SENIOR)) {
            role = Constants.SENIOR;
        } else if (roles.contains(Constants.GENERAL)) {
            role = Constants.GENERAL;
        }
        Query query = new Query().addCriteria(Criteria.where("logo").is(role));
        RoleDO roleDO = mongoTemplate.findOne(query, RoleDO.class);
        String accessToken = jwtTokenUtils.generateToken(user, "accessToken", new HashSet<>(roleDO.getPathList()));
        String refreshToken = jwtTokenUtils.generateRefreshToken(user, "refreshToken", new HashSet<>(roleDO.getPathList()));
        Token token = new Token();
        token.setUserId(user.getId());
        token.setName(user.getName());
        token.setEmailAccounts(user.getEmailAccounts());
        token.setRoles(user.getRoles());
        token.setAccessToken(accessToken);
        token.setRefreshToken(refreshToken);
        return token;
    }

    private Object judgeUmt(HttpServletResponse response) {
        Object umt = cacheLoading.loadingUmt();
        if (umt == null) {
            // Not added to technology cloud configuration
            returnRes(response, CommonUtils.messageInternational("AUTH_CLOUD"));
        }
        return umt;
    }

    private Object judgeWechat(HttpServletResponse response) {
        Object wechat = cacheLoading.loadingWechat();
        if (wechat == null) {
            // Not added to WeChat configuration
            returnRes(response, CommonUtils.messageInternational("AUTH_CLOUD"));
        }
        return wechat;
    }

    private Object judgeEsc(HttpServletResponse response) {
        Object esc = cacheLoading.loadingEsc();
        if (esc == null) {
            // Not joined the shared network configuration
            returnRes(response, CommonUtils.messageInternational("AUTH_CLOUD"));
        }
        return esc;
    }

    private void checkCode(String code, HttpServletResponse response) throws IOException {
        if (StringUtils.isEmpty(code)) {
            returnRes(response, "code errorInternational!");
            return;
        }
        String decrypt = SMS4.Decrypt(code);
        if (decrypt == null) {
            returnRes(response, "code errorInternational!");
            return;
        }
        if (!decrypt.contains("&")) {
            returnRes(response, "code errorInternational!");
            return;
        }
        String[] split = decrypt.split("&");
        String email = split[1];
        if (StringUtils.isEmpty(email)) {
            returnRes(response, "errorInternational");
            return;
        }
        String time = split[2];
        if (time.trim().equals("")) {
            returnRes(response, "code errorInternational!");
            return;
        }
        Long activationTime = Long.valueOf(time);
        if (activationTime.longValue() < CommonUtils.yesterday(24)) {
            returnRes(response, "Link broken!");
            return;
        }
    }

    private void checkCode(String code) {
        if (StringUtils.isEmpty(code)) {
            throw new CommonException(-1, "code errorInternational!");
        }
        String decrypt = SMS4.Decrypt(code);
        if (decrypt == null) {
            throw new CommonException(-1, "code errorInternational!");
        }
        if (!decrypt.contains("&")) {
            throw new CommonException(-1, "code errorInternational!");
        }
        String[] split = decrypt.split("&");
        String email = split[1];
        if (StringUtils.isEmpty(email)) {
            throw new CommonException(-1, "code errorInternational!");
        }
        String time = split[2];
        if (time.trim().equals("")) {
            throw new CommonException(-1, "code errorInternational!");
        }
        Long activationTime = Long.valueOf(time);
        if (activationTime.longValue() < CommonUtils.yesterday(24)) {
            throw new CommonException(-1, "Link broken!");
        }
    }

    /**
     * WeChat user synchronization+modification
     */
    private void wechatUpdate(Map<String, Object> wechatMap, WechatUser wechatUser) {
        String unionId = (String) wechatMap.get("unionid");
        String nickname = (String) wechatMap.get("nickname");
        String headImgUrl = (String) wechatMap.get("headimgurl");
        String openid = (String) wechatMap.get("openid");
        int sex = (int) (double) wechatMap.get("sex");
        String province = (String) wechatMap.get("province");
        String city = (String) wechatMap.get("city");
        String country = (String) wechatMap.get("country");
        // 1 is male and 2 is female
        String gen = sex == 1 ? "男" : "女";
        if (null == wechatUser) {
            WechatUser chatUser = new WechatUser();
            chatUser.setUnionId(unionId);
            chatUser.setRealName(nickname);
            chatUser.setHeadImgUrl(headImgUrl);
            chatUser.setOpenId(openid);
            chatUser.setSex(gen);
            chatUser.setProvince(province);
            chatUser.setCity(city);
            chatUser.setCountry(country);
            chatUser.setCreateTime(new Date());
            mongoTemplate.insert(chatUser);
        } else {
            // update
            boolean judge = false;
            if (StringUtils.isEmpty(wechatUser.getRealName()) || !wechatUser.getRealName().equals(nickname)) {
                wechatUser.setRealName(nickname);
                judge = true;
            }
            if (StringUtils.isEmpty(wechatUser.getHeadImgUrl()) || !wechatUser.getHeadImgUrl().equals(headImgUrl)) {
                wechatUser.setHeadImgUrl(headImgUrl);
                judge = true;
            }
            if (StringUtils.isEmpty(wechatUser.getSex()) || !wechatUser.getSex().equals(gen)) {
                wechatUser.setSex(gen);
                judge = true;
            }
            if (StringUtils.isEmpty(wechatUser.getProvince()) || !wechatUser.getProvince().equals(province)) {
                wechatUser.setProvince(province);
                judge = true;
            }
            if (StringUtils.isEmpty(wechatUser.getCity()) || !wechatUser.getCity().equals(city)) {
                wechatUser.setCity(city);
                judge = true;
            }
            if (StringUtils.isEmpty(wechatUser.getCountry()) || !wechatUser.getCountry().equals(country)) {
                wechatUser.setCountry(country);
                judge = true;
            }
            if (judge) {
                wechatUser.setLastUpdateTime(new Date());
                mongoTemplate.save(wechatUser);
            }
        }
        return;
    }

    /**
     * User login information saving cookie
     */
    private void loginInfoAddCookie(ConsumerDO user, HttpServletResponse response, String loginWay) {
        Token token = saveUserInfo(user);
        Cookie cookie = CommonUtils.getCookie(Constants.TOKEN, token.getAccessToken(), -1, 1);
        Cookie cookieTo = CommonUtils.getCookie(Constants.WAY, loginWay, -1, 1);
        response.addCookie(cookie);
        response.addCookie(cookieTo);
        // Record login logs, modify user login status
        loginLogRecording(token.getUserId(), token.getEmailAccounts(), true, loginWay);
    }

    private void loginInfoAddCookie(Token token, HttpServletResponse response, String loginWay) {
        Cookie cookie = CommonUtils.getCookie(Constants.TOKEN, token.getAccessToken(), -1, 1);
        Cookie cookieTo = CommonUtils.getCookie(Constants.WAY, loginWay, -1, 1);
        response.addCookie(cookie);
        response.addCookie(cookieTo);
        // Record login logs, modify user login status
        loginLogRecording(token.getUserId(), token.getEmailAccounts(), true, loginWay);
    }

    /**
     * User login logging
     */
    private void loginLogRecording(String userId, String email, boolean isLogin, String loginWay) {
        Query query = new Query().addCriteria(Criteria.where("_id").is(userId));
        Update update = new Update();
        update.set("online", isLogin);
        if (isLogin) {
            update.set("lastLoginTime", new Date());
        } else {
            update.set("lastLogoutTime", new Date());
        }
        // Modify online status
        mongoTemplate.upsert(query, update, ConsumerDO.class);
        // Record login logs
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String ipAddr = CommonUtils.getIpAddr(request);
        IpInfo ipInfoBean = Ip2regionAnalysis.getInstance().getIpInfoBean(ipAddr);
        UserLoginLog userLoginLog = new UserLoginLog(userId, email, ipAddr, ipInfoBean, loginWay, isLogin ? "login" : "logout");
        mongoTemplate.insert(userLoginLog);
    }
}
