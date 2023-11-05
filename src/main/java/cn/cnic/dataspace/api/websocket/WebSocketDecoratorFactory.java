package cn.cnic.dataspace.api.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;
import java.security.Principal;

/**
 * The server and client will be executed when shaking hands and waving hands
 */
@Slf4j
public class // @Component
WebSocketDecoratorFactory implements WebSocketHandlerDecoratorFactory {

    @Override
    public WebSocketHandler decorate(WebSocketHandler handler) {
        return new WebSocketHandlerDecorator(handler) {

            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                Principal principal = session.getPrincipal();
                if (principal != null) {
                    log.info("user:{} 已连接", principal.getName());
                    // Identity verification successful, cache socket connection
                    // SocketManager.add(principal.getName(), session);
                }
                super.afterConnectionEstablished(session);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                Principal principal = session.getPrincipal();
                if (principal != null) {
                    log.info("user:{} 断开连接", principal.getName());
                    // Identity verification successful, remove socket connection
                    SocketManager.removeByEmail(principal.getName());
                }
                super.afterConnectionClosed(session, closeStatus);
            }
        };
    }
}
