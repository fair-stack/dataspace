package cn.cnic.dataspace.api.service;

import cn.cnic.dataspace.api.model.user.Channel;
import cn.cnic.dataspace.api.util.ResponseResult;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface AuthService {

    /**
     * Login authentication
     */
    ResponseResult<Object> login(String emailAccounts, String password, String openId, HttpServletResponse response);

    void emailActivation(String code, HttpServletResponse response) throws IOException;

    void logout(HttpServletRequest request, HttpServletResponse response) throws IOException;

    ResponseResult<Object> getUserInfo(String token, HttpServletResponse response);

    void umtCallback(String code, HttpServletResponse response) throws IOException;

    void umtLogin(HttpServletResponse response);

    void passActivation(String code, HttpServletRequest request, HttpServletResponse response) throws IOException;

    ResponseResult<Object> emailSend(String email, String type);

    ResponseResult<Object> umpWork(String code, String work, HttpServletResponse response);

    ResponseResult<Object> channel(Channel channel);

    void channelLogin(String id, HttpServletResponse response) throws ServletException, IOException;

    void wechatLogin(String type, HttpServletResponse response);

    void wechatCallback(String code, String state, HttpServletRequest request, HttpServletResponse response) throws IOException;

    ResponseResult<Object> wechatAcc(String emailAccounts, String password, HttpServletRequest request, HttpServletResponse response);

    ResponseResult<Object> wechatRegister(String emailAccounts, String name, String org, HttpServletRequest request, HttpServletResponse response);

    void escLogin(HttpServletResponse response);

    ResponseResult<Object> wechatUserinfo(HttpServletRequest request);

    void escCallback(String code, HttpServletResponse response) throws IOException;

    void spaceDetails(String key, String user, String spaceId, HttpServletResponse response) throws IOException;

    ResponseResult<Object> getCode(HttpServletRequest request, HttpServletResponse response);
}
