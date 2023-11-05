package cn.cnic.dataspace.api.webdav;

import cn.cnic.dataspace.api.config.space.SpaceControlConfig;
import cn.cnic.dataspace.api.model.space.Space;
import cn.cnic.dataspace.api.model.space.child.AuthorizationPerson;
import cn.cnic.dataspace.api.model.user.ConsumerDO;
import cn.cnic.dataspace.api.repository.SpaceRepository;
import cn.cnic.dataspace.api.repository.UserRepository;
import cn.cnic.dataspace.api.util.RSAEncrypt;
import cn.cnic.dataspace.api.util.SpaceRoleEnum;
import io.milton.http.Auth;
import io.milton.http.Request;
import io.milton.http.http11.auth.DigestGenerator;
import io.milton.http.http11.auth.DigestResponse;
import io.milton.resource.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Objects;
import java.util.Set;

/**
 * DataSpaceSecurityManager
 *
 * @author wangCc
 * @date 2021-3-23 20:36:22
 */
@Data
@Slf4j
@Component
public class DataSpaceSecurityManager implements io.milton.http.SecurityManager {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private SpaceControlConfig spaceControlConfig;

    private String realm;

    private DigestGenerator digestGenerator;

    public DataSpaceSecurityManager() {
    }

    @Override
    public Object authenticate(String user, String requestedPassword) {
        // Verify if the user account password is correct
        ConsumerDO userAccounts = userRepository.findByEmailAccounts(user);
        if (Objects.isNull(userAccounts)) {
            return null;
        }
        String realPwd = RSAEncrypt.decrypt(userAccounts.getPassword());
        if (!realPwd.equals(requestedPassword)) {
            // Incorrect account password returns null
            log.warn("that password is incorrect. Try 'password'");
            return null;
        }
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        // Obtain information about HttpServletRequest from RequestAttributes
        HttpServletRequest request = (HttpServletRequest) requestAttributes.resolveReference(RequestAttributes.REFERENCE_REQUEST);
        // webDAV /userId/spaceName/
        String webDavPrefixPath = request.getRequestURI().contains("/api/webDAV/") ? "/api/webDAV" : "/webDAV";
        String uri = null;
        try {
            uri = URLDecoder.decode(request.getRequestURI().replaceAll(webDavPrefixPath, ""), "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        log.debug("----webdav uri----" + uri);
        boolean hasProjectAccess = false;
        // Verify if there is space permission
        String[] split = uri.split("/");
        if (split.length > 1) {
            String code = split[1];
            log.debug("code = " + code);
            String spaceId = spaceControlConfig.getSpaceId(code);
            log.debug("spaceId = " + spaceId);
            try {
                spaceControlConfig.validateSpacePermissions(user, spaceId, SpaceRoleEnum.F_OTHER_WEBDAV.getRole());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return null;
            }
            Space space = spaceRepository.findById(spaceId).get();
            if (StringUtils.equals(space.getUserId(), userAccounts.getId())) {
                // Space Creator
                hasProjectAccess = true;
            } else {
                // Space members
                Set<AuthorizationPerson> authorizationList = space.getAuthorizationList();
                for (AuthorizationPerson authorizationPerson : authorizationList) {
                    if (StringUtils.equals(authorizationPerson.getUserId(), userAccounts.getId())) {
                        hasProjectAccess = true;
                        break;
                    }
                }
            }
            if (hasProjectAccess) {
                // Space permissions available
                return Boolean.TRUE;
            }
        }
        return null;
    }

    @Override
    public Object authenticate(DigestResponse digestRequest) {
        if (digestRequest == null) {
            return null;
        }
        // Verify if the user account password is correct
        ConsumerDO userAccounts = userRepository.findByEmailAccounts(digestRequest.getUser());
        if (Objects.isNull(userAccounts)) {
            return null;
        }
        DigestGenerator gen = new DigestGenerator();
        String actual = gen.generateDigest(digestRequest, RSAEncrypt.decrypt(userAccounts.getPassword()));
        if (!actual.equals(digestRequest.getResponseDigest())) {
            // Incorrect account password returns null
            log.warn("that password is incorrect. Try 'password'");
            return null;
        }
        // webDAV /userId/spaceName/
        String webDavPrefixPath = digestRequest.getUri().contains("/api/webDAV/") ? "/api/webDAV" : "/webDAV";
        String uri = null;
        try {
            uri = URLDecoder.decode(digestRequest.getUri().replaceAll(webDavPrefixPath, ""), "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        log.debug("----webdav uri----" + uri);
        boolean hasProjectAccess = false;
        // Verify if there is space permission
        String[] split = uri.split("/");
        if (split.length > 1) {
            String code = split[1];
            log.debug("code = " + code);
            String spaceId = spaceControlConfig.getSpaceId(code);
            log.debug("spaceId = " + spaceId);
            try {
                spaceControlConfig.validateSpacePermissions(userAccounts.getEmailAccounts(), spaceId, SpaceRoleEnum.F_OTHER_WEBDAV.getRole());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return null;
            }
            Space space = spaceRepository.findById(spaceId).get();
            if (StringUtils.equals(space.getUserId(), userAccounts.getId())) {
                hasProjectAccess = true;
            } else {
                Set<AuthorizationPerson> authorizationList = space.getAuthorizationList();
                for (AuthorizationPerson authorizationPerson : authorizationList) {
                    if (StringUtils.equals(authorizationPerson.getUserId(), userAccounts.getId())) {
                        hasProjectAccess = true;
                        break;
                    }
                }
            }
            if (hasProjectAccess) {
                // Space permissions available
                return Boolean.TRUE;
            }
        }
        return null;
    }

    @Override
    public boolean authorise(Request request, Request.Method method, Auth auth, Resource resource) {
        return auth != null;
    }

    @Override
    public String getRealm(String host) {
        return realm;
    }

    @Override
    public boolean isDigestAllowed() {
        return digestGenerator != null;
    }
}
