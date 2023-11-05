package cn.cnic.dataspace.api.util;

import cn.cnic.dataspace.api.model.space.SpaceSimple;
import cn.cnic.dataspace.api.model.space.child.Operator;
import cn.cnic.dataspace.api.model.open.AppAuthApi;
import cn.cnic.dataspace.api.model.open.Application;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * CaffeineUtil
 *
 * @author jmal
 */
@Component
public class CaffeineUtil {

    /**
     * FTP cache
     */
    private final static Cache<String, String> FTP_USERID = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.DAYS).build();

    private final static Cache<String, Map<String, String>> short_chain = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.DAYS).build();

    // User token
    private static Cache<String, String> tokenCache;

    // User+token
    private static Cache<String, List<String>> emailToken;

    // Preview Address Cache
    private static Cache<String, String> privateLink;

    // Publishing Institution Path
    private static Cache<String, Map<String, String>> publicOrgUrl;

    // Publish Template
    private static Cache<String, String> publicModel;

    // SCIDB token cache
    private static Cache<String, String> SCIENCE_DATA;

    // Space invitation
    private static Cache<String, String> SPACE_INVITE;

    // Space application record
    private static Cache<String, List<String>> APPLY_SPACE;

    // Overall space downloads, visits, etc
    private static Cache<String, Object> SPACE_STATISTIC;

    // Space user cache
    private static Cache<String, List<String>> SPACE_AUTH;

    // Space member roles
    private static Cache<String, String> SPACE_USER_ROLE;

    // Space member refinement permissions
    private static Cache<String, List<String>> SPACE_MEN_ROLE;

    // Mapping of Spatial Storage Code and Spatial IP
    private static Cache<String, String> SPACE_SHORT;

    // Third party information caching
    private static Cache<String, String> THIRD_PARTY;

    // Short time operation verification
    private static Cache<String, String> CHECK;

    // Password error count verification
    private static Cache<String, Integer> ERROR_PWD;

    private static Cache<String, String> disable;

    // Email time judgment
    private static Cache<String, String> EMAIL_SEND;

    // Other system configurations
    private static Cache<String, Object> config;

    // Authorization application information cache
    private static Cache<String, Application> application;

    // Application API authorization information cache
    private static Cache<String, AppAuthApi> AUTH_OPENAPI;

    // User email notification data
    private static Cache<String, List<String>> USER_EMAIL_ROLE;

    // Used for user revocation and thread interruption during publishing
    private static Cache<String, Boolean> PUBLIC_FILE_STOP;

    // User log information
    private static Cache<String, Operator> USER_LOG;

    // Simple spatial information caching
    public static Cache<String, SpaceSimple> SPACE_SIMPLE;

    // Data submission and publication duplicate verification
    public static Cache<String, String> PUBLIC_DATA;

    public static void setShortChain(String username, Map<String, String> authorities) {
        short_chain.put(username, authorities);
    }

    public static Map<String, String> getShortChain(String username) {
        return short_chain.getIfPresent(username);
    }

    @PostConstruct
    public void initCache() {
        initMyCache();
    }

    public static void initMyCache() {
        if (tokenCache == null) {
            tokenCache = Caffeine.newBuilder().expireAfterAccess(3, TimeUnit.HOURS).build();
        }
        if (emailToken == null) {
            emailToken = Caffeine.newBuilder().expireAfterAccess(3, TimeUnit.HOURS).build();
        }
        if (privateLink == null) {
            privateLink = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.DAYS).build();
        }
        if (publicOrgUrl == null) {
            publicOrgUrl = Caffeine.newBuilder().build();
        }
        if (publicModel == null) {
            publicModel = Caffeine.newBuilder().build();
        }
        if (SCIENCE_DATA == null) {
            SCIENCE_DATA = Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).build();
        }
        if (THIRD_PARTY == null) {
            THIRD_PARTY = Caffeine.newBuilder().expireAfterAccess(3, TimeUnit.HOURS).build();
        }
        if (SPACE_INVITE == null) {
            SPACE_INVITE = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.DAYS).build();
        }
        if (config == null) {
            config = Caffeine.newBuilder().expireAfterWrite(7, TimeUnit.DAYS).build();
        }
        if (CHECK == null) {
            CHECK = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.DAYS).build();
        }
        if (EMAIL_SEND == null) {
            EMAIL_SEND = Caffeine.newBuilder().expireAfterWrite(3, TimeUnit.MINUTES).build();
        }
    }

    public static Cache<String, String> getTokenCache() {
        if (tokenCache == null) {
            initMyCache();
        }
        return tokenCache;
    }

    public static Cache<String, List<String>> getEmailToken() {
        if (emailToken == null) {
            initMyCache();
        }
        return emailToken;
    }

    public static Cache<String, String> getPrivateLink() {
        if (privateLink == null) {
            initMyCache();
        }
        return privateLink;
    }

    public static Cache<String, Map<String, String>> getPublicOrgUrl() {
        if (publicOrgUrl == null) {
            initMyCache();
        }
        return publicOrgUrl;
    }

    public static Cache<String, String> getPublicModel() {
        if (publicModel == null) {
            initMyCache();
        }
        return publicModel;
    }

    public static Cache<String, String> getScienceData() {
        if (SCIENCE_DATA == null) {
            initMyCache();
        }
        return SCIENCE_DATA;
    }

    public static Cache<String, String> getThirdParty() {
        if (THIRD_PARTY == null) {
            initMyCache();
        }
        return THIRD_PARTY;
    }

    public static Cache<String, String> getSpaceInvite() {
        if (SPACE_INVITE == null) {
            initMyCache();
        }
        return SPACE_INVITE;
    }

    public static Cache<String, Object> getConfig() {
        if (config == null) {
            initMyCache();
        }
        return config;
    }

    public static Cache<String, String> getCHECK() {
        if (CHECK == null) {
            initMyCache();
        }
        return CHECK;
    }

    public static Cache<String, String> getEmailSend() {
        if (EMAIL_SEND == null) {
            initMyCache();
        }
        return EMAIL_SEND;
    }

    /**
     * Frequent email sending with a delay of 3 minutes
     */
    public static boolean checkEmailAging(String key) {
        Cache<String, String> emailSend = getEmailSend();
        String result = emailSend.getIfPresent(key);
        if (null == result) {
            emailSend.put(key, CommonUtils.getDateTimeString(new Date()));
            return false;
        }
        return true;
    }

    public static Cache<String, Integer> getErrorPwd() {
        if (ERROR_PWD == null) {
            ERROR_PWD = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build();
        }
        return ERROR_PWD;
    }

    public static Cache<String, String> getDisable() {
        if (disable == null) {
            disable = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.DAYS).build();
        }
        return disable;
    }

    public static Cache<String, List<String>> getApplySpace() {
        if (APPLY_SPACE == null) {
            APPLY_SPACE = Caffeine.newBuilder().expireAfterWrite(4, TimeUnit.HOURS).build();
        }
        return APPLY_SPACE;
    }

    public static Cache<String, Object> getSpaceStatistic() {
        if (SPACE_STATISTIC == null) {
            SPACE_STATISTIC = Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.DAYS).build();
        }
        return SPACE_STATISTIC;
    }

    public static Cache<String, Application> getApplication() {
        if (application == null) {
            application = Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.DAYS).build();
        }
        return application;
    }

    public static Cache<String, AppAuthApi> getAuthOpenapi() {
        if (AUTH_OPENAPI == null) {
            AUTH_OPENAPI = Caffeine.newBuilder().expireAfterWrite(12, TimeUnit.HOURS).build();
        }
        return AUTH_OPENAPI;
    }

    public static Cache<String, List<String>> getSpaceAuth() {
        if (SPACE_AUTH == null) {
            SPACE_AUTH = Caffeine.newBuilder().expireAfterWrite(12, TimeUnit.HOURS).build();
        }
        return SPACE_AUTH;
    }

    public static Cache<String, String> getSpaceUserRole() {
        if (SPACE_USER_ROLE == null) {
            SPACE_USER_ROLE = Caffeine.newBuilder().expireAfterWrite(12, TimeUnit.HOURS).build();
        }
        return SPACE_USER_ROLE;
    }

    public static Cache<String, List<String>> getSpaceMenRole() {
        if (SPACE_MEN_ROLE == null) {
            SPACE_MEN_ROLE = Caffeine.newBuilder().expireAfterWrite(12, TimeUnit.HOURS).build();
        }
        return SPACE_MEN_ROLE;
    }

    public static Cache<String, List<String>> getUserEmailRole() {
        if (USER_EMAIL_ROLE == null) {
            USER_EMAIL_ROLE = Caffeine.newBuilder().expireAfterWrite(12, TimeUnit.HOURS).build();
        }
        return USER_EMAIL_ROLE;
    }

    public static Cache<String, Boolean> getPublicFileStop() {
        if (PUBLIC_FILE_STOP == null) {
            PUBLIC_FILE_STOP = Caffeine.newBuilder().expireAfterWrite(12, TimeUnit.HOURS).build();
        }
        return PUBLIC_FILE_STOP;
    }

    public static Cache<String, String> getSpaceShort() {
        if (SPACE_SHORT == null) {
            SPACE_SHORT = Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.DAYS).build();
        }
        return SPACE_SHORT;
    }

    public static Cache<String, String> getPublicData() {
        if (PUBLIC_DATA == null) {
            PUBLIC_DATA = Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MILLISECONDS).build();
        }
        return PUBLIC_DATA;
    }

    public static void setSpaceFull(String username, String userId) {
        FTP_USERID.put(username, userId);
    }

    public static String getSpaceFull(String username) {
        return FTP_USERID.getIfPresent(username);
    }

    public static boolean spaceFull(String userId) {
        return FTP_USERID.getIfPresent(userId) != null;
    }

    public static void clearFtpUserId() {
        FTP_USERID.cleanUp();
    }

    public static void clearFtpShor() {
        short_chain.cleanUp();
    }

    public static Cache<String, Operator> getUserLog() {
        if (USER_LOG == null) {
            USER_LOG = Caffeine.newBuilder().expireAfterWrite(12, TimeUnit.HOURS).build();
        }
        return USER_LOG;
    }

    public static Cache<String, SpaceSimple> getSpaceSimple() {
        if (SPACE_SIMPLE == null) {
            SPACE_SIMPLE = Caffeine.newBuilder().expireAfterWrite(12, TimeUnit.HOURS).build();
        }
        return SPACE_SIMPLE;
    }
}
