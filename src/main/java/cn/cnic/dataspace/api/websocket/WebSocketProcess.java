package cn.cnic.dataspace.api.websocket;

import cn.cnic.dataspace.api.model.space.Space;
import cn.cnic.dataspace.api.model.space.child.AuthorizationPerson;
import cn.cnic.dataspace.api.repository.SpaceRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import javax.annotation.Resource;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * WebSocketProcess
 *
 * @author wangCc
 * @date 2021-10-30 19:13:46
 */
@Slf4j
@Component
@ServerEndpoint(value = "/msg/{email}")
public class WebSocketProcess {

    // private Session session;
    @OnOpen
    public void onOpen(Session session, @PathParam("email") String email) {
        // this.session = session;
        String key = SocketManager.generateKey(email, session.getId());
        if (SocketManager.contains(key)) {
            log.warn("客户端程序{}已有连接,无需建立连接", email);
            return;
        }
        if ((!StringUtils.equals("undefined", email)) && StringUtils.isNotBlank(email)) {
            SocketManager.add(key, session);
            log.info("socket open: {} ", key);
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("email") String email) {
        // this.session = session;
        String key = SocketManager.generateKey(email, session.getId());
        if (!SocketManager.contains(key)) {
            log.warn("客户端程序{}没有连接,无需断开连接", key);
            return;
        }
        SocketManager.remove(key);
        log.info("socket close: {} ", key);
    }

    @OnMessage
    public void onMessage(String message, @PathParam("email") String email) {
        log.info("receive a message from client whose email={},msg={}", email, message);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        try {
            log.error("Error while websocket. " + error.getMessage());
        } catch (Throwable throwable) {
            log.info(error.getMessage());
        }
    }

    /**
     * send Message to specific client
     */
    public void sendMessage(String email, String message) throws Exception {
        List<Session> sessions = SocketManager.getByEmail(email);
        if (CollectionUtils.isEmpty(sessions)) {
            return;
        }
        for (Session session : sessions) {
            if (!ObjectUtils.isEmpty(session)) {
                if (session.isOpen()) {
                    session.getBasicRemote().sendText(message);
                } else {
                    String cacheKey = SocketManager.generateKey(email, session.getId());
                    log.error("websocket session={} is closed ", cacheKey);
                }
            } else {
                log.error("websocket session={} is null");
            }
        }
    }

    @Resource
    private SpaceRepository spaceRepository;

    /**
     * send Message to all client
     */
    public void sendMessage2AllSpaceClient(String spaceId, String message) throws Exception {
        Optional<Space> byId = spaceRepository.findById(spaceId);
        if (byId.isPresent()) {
            Space space = byId.get();
            Set<AuthorizationPerson> authorizationList = space.getAuthorizationList();
            if (authorizationList != null) {
                Set<String> spaceEmails = authorizationList.stream().map(var -> var.getEmail()).collect(Collectors.toSet());
                for (String spaceEmail : spaceEmails) {
                    List<Session> sessions = SocketManager.getByEmail(spaceEmail);
                    if (!CollectionUtils.isEmpty(sessions)) {
                        for (Session session : sessions) {
                            if (session != null) {
                                if (session.isOpen()) {
                                    session.getAsyncRemote().sendText(message);
                                } else {
                                    String cacheKey = SocketManager.generateKey(spaceEmail, session.getId());
                                    log.error("websocket session={} is closed ", cacheKey);
                                }
                            } else {
                                log.error("websocket session is null");
                            }
                        }
                    }
                }
            } else {
                log.error("--- sendMessage2AllSpaceClient failed, authorizationList empty ---");
            }
        } else {
            log.error("--- sendMessage2AllSpaceClient failed, space not found ---");
        }
    }
}
