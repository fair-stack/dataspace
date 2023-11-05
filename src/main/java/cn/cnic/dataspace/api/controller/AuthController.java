package cn.cnic.dataspace.api.controller;

import cn.cnic.dataspace.api.exception.ExceptionType;
import cn.cnic.dataspace.api.service.AuthService;
import cn.cnic.dataspace.api.util.CommonUtils;
import cn.cnic.dataspace.api.util.Constants;
import cn.cnic.dataspace.api.util.ResponseResult;
import cn.cnic.dataspace.api.util.ResultUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Login, logout, authentication
 */
@RestController
@Api(tags = "登录认证")
public class AuthController {

    private AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @ApiOperation("系统登录")
    @GetMapping("/login")
    public ResponseResult<Object> login(@RequestParam(name = "emailAccounts") String emailAccounts, @RequestParam(name = "password") String password, HttpServletResponse response) {
        return authService.login(emailAccounts, password, null, response);
    }

    @ApiOperation("登出")
    @GetMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        authService.logout(request, response);
        return;
    }

    /**
     * Technology Cloud Login
     */
    @ApiOperation("科技云登录")
    @GetMapping("/umt.log")
    public void umtLogin(HttpServletResponse response) {
        authService.umtLogin(response);
        return;
    }

    /**
     * WeChat login
     */
    @ApiOperation("微信登录")
    @GetMapping("/wechat.login")
    public void wechatLogin(@RequestParam(name = "type", required = false) String type, HttpServletResponse response) {
        authService.wechatLogin(type, response);
        return;
    }

    /**
     * WeChat login
     */
    @ApiOperation("共享网登录")
    @GetMapping("/esc.login")
    public void escLogin(HttpServletResponse response) {
        authService.escLogin(response);
        return;
    }

    /**
     * Obtain user information based on login token
     */
    @GetMapping("/get.u")
    public ResponseResult<Object> getUserInfo(HttpServletRequest request, HttpServletResponse response) {
        String token = CommonUtils.getUser(request, Constants.TOKEN);
        if (token == null) {
            return ResultUtil.error(ExceptionType.LOGIN_EXCEPRION);
        }
        return authService.getUserInfo(token, response);
    }

    @ApiOperation(value = "邮箱未收到，再次发送邮箱（register/changePwd）")
    @GetMapping("/send")
    public ResponseResult<Object> emailSend(@RequestParam(name = "email") String email, @RequestParam(name = "type") String type) {
        return authService.emailSend(email, type);
    }

    @ApiOperation(value = "用户完善信息-工作单位")
    @GetMapping("/ump.work")
    public ResponseResult<Object> umpWork(@RequestParam(name = "code") String code, @RequestParam(name = "work") String work, HttpServletResponse response) {
        return authService.umpWork(code, work, response);
    }

    @ApiOperation(value = "获取微信用户的个人信息")
    @GetMapping("/wechat.info")
    public ResponseResult<Object> wechatUserinfo(HttpServletRequest request) {
        return authService.wechatUserinfo(request);
    }

    @ApiOperation(value = "微信用户绑定账号并登录")
    @GetMapping("/wechat.acc")
    public ResponseResult<Object> wechatAcc(@RequestParam(name = "emailAccounts") String emailAccounts, @RequestParam(name = "password") String password, HttpServletRequest request, HttpServletResponse response) {
        return authService.wechatAcc(emailAccounts, password, request, response);
    }

    @ApiOperation(value = "微信用户注册绑定用户")
    @GetMapping("/wechat.register")
    public ResponseResult<Object> wechatRegister(@RequestParam(name = "emailAccounts") String emailAccounts, @RequestParam(name = "name") String name, @RequestParam(name = "org") String org, HttpServletRequest request, HttpServletResponse response) {
        return authService.wechatRegister(emailAccounts, name, org, request, response);
    }

    /**
     * Obtain verification code
     */
    @GetMapping("/getCode")
    public ResponseResult<Object> getCode(HttpServletRequest request, HttpServletResponse response) {
        return authService.getCode(request, response);
    }
}
