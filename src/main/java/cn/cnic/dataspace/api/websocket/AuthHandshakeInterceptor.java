package cn.cnic.dataspace.api.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import java.util.Map;

/**
 * websocket
 *
 * @Author jmal
 * @Date 2020-07-01 14:12
 */
// @Component
public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse, WebSocketHandler webSocketHandler, Map<String, Object> map) throws Exception {
        ServletServerHttpRequest request = (ServletServerHttpRequest) serverHttpRequest;
        System.out.println("request.getServletRequest().getRequestURI() = " + request.getServletRequest().getRequestURI());
        System.out.println("request.getServletRequest().getContextPath() = " + request.getServletRequest().getContextPath());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse, WebSocketHandler webSocketHandler, Exception e) {
    }
}
