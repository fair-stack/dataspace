package cn.cnic.dataspace.api.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Map;

/**
 * We can determine whether users can connect by requesting information, such as tokens or sessions, in order to prevent illegal users
 */
// @Component
public class PrincipalHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        /**Here, you can determine how to obtain unique values, such as Unicode, according to your needs*/
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletServerHttpRequest = (ServletServerHttpRequest) request;
            HttpServletRequest httpRequest = servletServerHttpRequest.getServletRequest();
            /**This is where you can obtain your most familiar stranger and carry parameters. You can use cookies, request headers, or URLs to carry them, while I use URLs to carry them*/
            final String name = httpRequest.getParameter("name");
            if (StringUtils.isEmpty(name)) {
                return null;
            }
            return () -> name;
        }
        return null;
    }
}
