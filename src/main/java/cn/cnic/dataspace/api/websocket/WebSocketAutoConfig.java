package cn.cnic.dataspace.api.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * WebSocketAutoConfig
 */
// @Configuration
// @EnableWebSocketMessageBroker
public class WebSocketAutoConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private WebSocketDecoratorFactory webSocketDecoratorFactory;

    @Autowired
    private PrincipalHandshakeHandler principalHandshakeHandler;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/msg").setAllowedOrigins("*").setHandshakeHandler(principalHandshakeHandler).withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue", "/topic");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.addDecoratorFactory(webSocketDecoratorFactory);
    }
}
