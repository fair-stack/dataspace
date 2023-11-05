package cn.cnic.dataspace.api.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * WebSocketInterceptor
 *
 * @author wangCc
 * @date 2021-10-30 19:43
 */
@Slf4j
@Component
public class WebSocketInterceptor extends HandlerInterceptorAdapter {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        log.info("you have entered in websocket interceptor where you can do sth you wanna do ....");
        return true;
    }
}
