package cn.cnic.dataspace.api.interceport;

import cn.cnic.dataspace.api.util.*;
import com.alibaba.fastjson.JSON;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interceptor login control
 */
@Slf4j
@Component
public class WebInterceptor extends HandlerInterceptorAdapter {

    private static Integer code = null;

    private static String message = null;

    @Autowired
    private JwtTokenUtils jwtTokenUtils;

    @Autowired
    private SpaceUrl spaceUrl;

    private final Cache<String, String> tokenCache = CaffeineUtil.getTokenCache();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        boolean judge = true;
        String requestURI = request.getRequestURI();
        if (requestURI.contains("doc.html")) {
            // Swagger Path Interception - Production Environment
            if (spaceUrl.getSwaggerEnable()) {
                return judge;
            }
            code = 401;
            message = "请先登录";
            judge = false;
        } else {
            String authToken = jwtTokenUtils.getToken(request);
            if (authToken == null) {
                code = 401;
                message = "请先登录";
                judge = false;
            } else if (!jwtTokenUtils.validateToken(authToken)) {
                log.info("token:  {} 401 身份过期");
                code = 401;
                message = "身份已过期";
                judge = false;
            } else {
                String ifPresent = tokenCache.getIfPresent(authToken);
                if (ifPresent == null) {
                    code = 401;
                    message = "请先登录";
                    judge = false;
                }
                // authentication
                List<String> unAuthPath = jwtTokenUtils.getUnAuthPath(authToken);
                String substring = requestURI.substring(1);
                String uri = "/" + substring.substring(0, substring.indexOf("/") + 1) + "**";
                String uri2 = requestURI.substring(0, requestURI.lastIndexOf("/") + 1) + "**";
                if (!unAuthPath.contains(uri) && !unAuthPath.contains(uri2) && !unAuthPath.contains(requestURI) && !unAuthPath.contains(requestURI + "/**")) {
                    code = 403;
                    message = "权限不足";
                    judge = false;
                }
            }
        }
        if (!judge) {
            if (code == 401) {
                Cookie cookie = CommonUtils.getCookie(Constants.TOKEN, "", 0, 1);
                Cookie way = CommonUtils.getCookie(Constants.WAY, "", 0, 1);
                response.addCookie(cookie);
                response.addCookie(way);
            }
            Map<String, Object> param = new HashMap<>();
            param.put("code", code);
            param.put("message", message);
            errorMsg(response, param);
        }
        return judge;
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
}
