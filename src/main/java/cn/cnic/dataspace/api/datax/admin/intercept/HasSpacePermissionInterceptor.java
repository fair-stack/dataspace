package cn.cnic.dataspace.api.datax.admin.intercept;

import cn.cnic.dataspace.api.config.space.MsgUtil;
import cn.cnic.dataspace.api.config.space.SpaceControlConfig;
import cn.cnic.dataspace.api.datax.admin.aop.HasSpacePermission;
import cn.cnic.dataspace.api.datax.admin.aop.SpacePermission;
import cn.cnic.dataspace.api.model.space.Space;
import cn.cnic.dataspace.api.model.space.child.AuthorizationPerson;
import cn.cnic.dataspace.api.model.user.ConsumerDO;
import cn.cnic.dataspace.api.repository.UserRepository;
import cn.cnic.dataspace.api.util.CommonUtils;
import cn.cnic.dataspace.api.util.JwtTokenUtils;
import cn.cnic.dataspace.api.util.Token;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class HasSpacePermissionInterceptor extends HandlerInterceptorAdapter {

    @Resource
    private MsgUtil msgUtil;

    @Resource
    private JwtTokenUtils jwtTokenUtils;

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private UserRepository userRepository;

    @Resource
    private SpaceControlConfig spaceControlConfig;

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        HandlerMethod method = (HandlerMethod) handler;
        HasSpacePermission classAnnotation = method.getMethod().getDeclaringClass().getAnnotation(HasSpacePermission.class);
        HasSpacePermission methodAnnotation = method.getMethodAnnotation(HasSpacePermission.class);
        // Method priority ratio class
        HasSpacePermission useAnnotation = methodAnnotation == null ? classAnnotation : methodAnnotation;
        if (useAnnotation != null) {
            SpacePermission permission = useAnnotation.value();
            boolean isAccess = spaceAuth(request, response, permission);
            return isAccess;
        } else {
            // No annotation, no verification permission required
            return true;
        }
    }

    private boolean spaceAuth(HttpServletRequest request, HttpServletResponse response, SpacePermission spacePermission) {
        if (spacePermission == SpacePermission.NO_VALID) {
            return true;
        }
        Token token = getToken(request);
        String spaceId = getSpaceId(request);
        return spaceAuth(spaceId, token, response, spacePermission);
    }

    public Token getToken(HttpServletRequest request) {
        String token = jwtTokenUtils.getToken(request);
        return jwtTokenUtils.getToken(token);
    }

    public String getSpaceId(HttpServletRequest request) {
        String spaceId = request.getHeader("spaceId");
        return spaceId;
    }

    private boolean spaceAuth(String spaceId, Token token, HttpServletResponse response, SpacePermission spacePermission) {
        Query query = new Query().addCriteria(Criteria.where("_id").is(spaceId));
        Space space = mongoTemplate.findOne(query, Space.class);
        if (null == space || !space.getState().equals("1")) {
            result(response, 500, "SPACE_REVIEW");
            return false;
        }
        // Verify the user's status
        ConsumerDO user = userRepository.findById(token.getUserId()).orElse(null);
        if (user == null) {
            result(response, 500, "AUTH_USER_NOT_FOUND");
            return false;
        } else if (user.getState() == 0) {
            result(response, 500, "USER_UNACTIVATED");
            return false;
        } else if (user.getState() == 3) {
            result(response, 500, "SAFE_DISABLE");
            return false;
        } else if (user.getState() == 2) {
            result(response, 500, "USER_UNREGISTERED");
            return false;
        }
        // Verify user access to space
        if (!token.getUserId().equals(space.getUserId())) {
            Set<AuthorizationPerson> authorizationList = space.getAuthorizationList();
            boolean judge = false;
            for (AuthorizationPerson authorizationPerson : authorizationList) {
                if (authorizationPerson.getUserId().equals(token.getUserId())) {
                    judge = true;
                }
            }
            if (!judge) {
                result(response, 403, "ACCESS_FORBIDDEN");
                return false;
            }
        }
        if (spacePermission == SpacePermission.T_READ) {
            return true;
        }
        // check role
        try {
            spaceControlConfig.validateSpacePermissions(token.getEmailAccounts(), spaceId, spacePermission.gettPermission());
        } catch (RuntimeException e) {
            result(response, -1, "PERMISSION_FORBIDDEN");
            return false;
        }
        return true;
    }

    /**
     * Error return
     */
    private void result(HttpServletResponse response, int code, String message) {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        Map<String, Object> result = new HashMap<>(2);
        result.put("code", code);
        result.put("msg", CommonUtils.messageInternational(message));
        try {
            response.getWriter().write(msgUtil.mapToString(result));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
