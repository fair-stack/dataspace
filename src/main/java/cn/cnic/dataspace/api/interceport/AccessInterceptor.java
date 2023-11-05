package cn.cnic.dataspace.api.interceport;

import cn.cnic.dataspace.api.asynchronous.AsyncDeal;
import cn.cnic.dataspace.api.util.CommonUtils;
import cn.cnic.dataspace.api.util.JwtTokenUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Slf4j
@EnableAsync
@Component
public class AccessInterceptor extends HandlerInterceptorAdapter {

    @Autowired
    private JwtTokenUtils jwtTokenUtils;

    @Autowired
    private AsyncDeal asyncDeal;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession();
        String token = jwtTokenUtils.getToken(request);
        if (null == token) {
            token = "";
        }
        Object user = null;
        synchronized (this) {
            user = session.getAttribute("access" + token);
            if (user == null) {
                session.setAttribute("access" + token, "hello");
                session.setMaxInactiveInterval(20 * 60);
            }
        }
        if (null != user) {
            return true;
        }
        String ipAddr = CommonUtils.getIpAddr(request);
        Update update = new Update();
        String email = "无";
        if (StringUtils.isNotEmpty(token) && jwtTokenUtils.validateToken(token)) {
            email = jwtTokenUtils.getEmail(token);
        }
        update.setOnInsert("email", email);
        asyncDeal.accessStatistics(ipAddr, update, email);
        return true;
    }
}
